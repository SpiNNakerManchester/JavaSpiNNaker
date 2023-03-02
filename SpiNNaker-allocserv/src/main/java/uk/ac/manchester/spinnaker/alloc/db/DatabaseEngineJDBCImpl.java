/*
 * Copyright (c) 2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.db;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.sql.Statement.RETURN_GENERATED_KEYS;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.jdbc.core.namedparam.NamedParameterUtils.buildSqlParameterList;
import static org.springframework.jdbc.core.namedparam.NamedParameterUtils.parseSqlStatement;
import static org.springframework.jdbc.core.namedparam.NamedParameterUtils.parseSqlStatementIntoString;
import static uk.ac.manchester.spinnaker.alloc.IOUtils.serialize;

import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.datasource.init.UncategorizedScriptException;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Implementation of the {@link DatabaseAPI} that uses JDBC to talk to MySQL.
 */
@Service
@Primary
public class DatabaseEngineJDBCImpl implements DatabaseAPI {
	private static final Logger log = getLogger(DatabaseEngineJDBCImpl.class);

	@Value("classpath:/spalloc-mysql.sql")
	private Resource sqlDDLFile;

	@Value("classpath:/spalloc-tombstone-mysql.sql")
	private Resource tombstoneDDLFile;

	private final JdbcTemplate jdbcTemplate;

	private final JdbcTemplate tombstoneJdbcTemplate;

	private final TransactionTemplate transactionTemplate;

	/**
	 * Create a new JDBC Database API.
	 *
	 * @param jdbcTemplate The connection to the main database.
	 * @param tombstoneJdbcTemplate The connection to the historical database.
	 * @param transactionManager The transaction manager.
	 */
	@Autowired
	public DatabaseEngineJDBCImpl(
			@Qualifier("mainDatabase") JdbcTemplate jdbcTemplate,
			@Qualifier("historicalDatabase") JdbcTemplate tombstoneJdbcTemplate,
			@Qualifier("mainTransactionManager")
				PlatformTransactionManager transactionManager) {
		this.jdbcTemplate = jdbcTemplate;
		this.tombstoneJdbcTemplate = tombstoneJdbcTemplate;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@PostConstruct
	private void setup() {
		log.info("Running Database setup...");
		runSQLFile(sqlDDLFile, jdbcTemplate);
		if (tombstoneJdbcTemplate != null) {
			log.info("Running tombstone setup");
			runSQLFile(tombstoneDDLFile, tombstoneJdbcTemplate);
		}
	}

	private void runSQLFile(Resource sqlFile, JdbcTemplate template) {
		var sql = readSQL(sqlFile);
		var lines = sql.split("\n");
		int i = 0;
		while (i < lines.length) {
			// Find the next statement start
			while (i < lines.length && !lines[i].startsWith("-- STMT")) {
				log.trace("DDL non-statement line {}", lines[i]);
				i++;
			}
			// Skip the STMT line
			i++;

			// Build a statement until it ends
			var stmt = new StringBuilder();
			while (i < lines.length && !lines[i].startsWith("-- STMT")
					&& !lines[i].startsWith("-- IGNORE")) {
				var line = lines[i].trim();
				if (!line.startsWith("--") && !line.isEmpty()) {
					log.trace("DDL statement line {}", line);
					stmt.append(line);
					stmt.append('\n');
				}
				i++;
			}
			if (stmt.length() != 0) {
				var statement = stmt.toString();
				log.debug("Executing DDL Statement: {}", statement);
				template.execute(statement);
			}
		}
	}

	private final class ConnectionImpl implements Connection {
		/** Whether a rollback has been requested on a transaction. */
		private boolean doRollback = false;

		/** The JdbcTemplate to use. */
		private final JdbcTemplate connectionJdbcTemplate;

		ConnectionImpl(JdbcTemplate connectionJdbcTemplate) {
			this.connectionJdbcTemplate = connectionJdbcTemplate;
		}

		@Override
		public void close() {
			// Does nothing
		}

		@Override
		public Query query(String sql) {
			return new QueryImpl(sql, connectionJdbcTemplate);
		}

		@Override
		public Query query(Resource sqlResource) {
			return new QueryImpl(readSQL(sqlResource), connectionJdbcTemplate);
		}

		@Override
		public Update update(String sql) {
			return new UpdateImpl(sql, connectionJdbcTemplate);
		}

		@Override
		public Update update(Resource sqlResource) {
			return new UpdateImpl(readSQL(sqlResource), connectionJdbcTemplate);
		}

		@Override
		public void transaction(boolean lockForWriting, Transacted operation) {
			// Use the other method, ignoring the result value
			transaction(lockForWriting, () -> {
				operation.act();
				return this;
			});
		}

		@Override
		public void transaction(Transacted operation) {
			transaction(true, operation);
		}

		@Override
		public <T> T transaction(TransactedWithResult<T> operation) {
			return transaction(true, operation);
		}

		@Override
		public synchronized <T> T transaction(boolean lockForWriting,
				TransactedWithResult<T> operation) {
			return transactionTemplate.execute(status -> {
				try {
					return operation.act();
				} finally {
					if (doRollback) {
						status.setRollbackOnly();
						doRollback = false;
					}
				}
			});
		}

		@Override
		public boolean isReadOnly() {
			return false;
		}

		@Override
		public void rollback() {
			doRollback = true;
		}

		@Override
		public Query query(String sql, boolean lockType) {
			return query(sql);
		}

		@Override
		public Query query(Resource sqlResource, boolean lockType) {
			return query(sqlResource);
		}
	}

	private abstract class StatementImpl implements StatementCommon {
		private final String originalSql;

		final String sql;

		final JdbcTemplate jdbcTemplate;

		StatementImpl(String sql, JdbcTemplate jdbcTemplate) {
			this.originalSql = sql;
			this.sql = parseSqlStatementIntoString(sql);
			this.jdbcTemplate = jdbcTemplate;
		}

		@Override
		public void close() {
			// Does Nothing
		}

		@Override
		public final List<String> getParameters() {
			return buildSqlParameterList(parseSqlStatement(originalSql),
					new EmptySqlParameterSource()).stream()
					.map(SqlParameter::getName).collect(toUnmodifiableList());
		}

		final Object[] resolveArguments(Object[] arguments) {
			var resolved = new Object[arguments.length];
			for (int i = 0; i < arguments.length; i++) {
				var arg = arguments[i];
				// The classes we augment the DB driver with
				if (arg instanceof Optional) {
					// Unpack one layer of Optional only; absent = NULL
					arg = ((Optional<?>) arg).orElse(null);
				}
				if (arg instanceof Instant) {
					arg = ((Instant) arg).getEpochSecond();
				} else if (arg instanceof Duration) {
					arg = ((Duration) arg).getSeconds();
				} else if (arg instanceof Enum) {
					arg = ((Enum<?>) arg).ordinal();
				} else if (arg != null && arg instanceof Serializable
						&& !(arg instanceof String || arg instanceof Number
								|| arg instanceof Boolean
								|| arg instanceof byte[])) {
					try {
						arg = serialize(arg);
					} catch (IOException e) {
						arg = null;
					}
				}
				resolved[i] = arg;
			}
			return resolved;
		}
	}

	private final class QueryImpl extends StatementImpl implements Query {
		QueryImpl(String sql, JdbcTemplate queryJdbcTemplate) {
			super(sql, queryJdbcTemplate);
		}

		@Override
		public <T> List<T> call(RowMapper<T> mapper, Object... arguments) {
			var resolved = resolveArguments(arguments);
			return jdbcTemplate.query(sql, (results) -> {
				var values = new ArrayList<T>();
				while (results.next()) {
					values.add(mapper.mapRow(new Row(results)));
				}
				return values;
			}, resolved);
		}

		@Override
		public <T> Optional<T> call1(RowMapper<T> mapper, Object... arguments) {
			var resolved = resolveArguments(arguments);
			return jdbcTemplate.query(sql, (results) -> {
				if (results.next()) {
					// Allow nullable if there is a value
					return Optional.ofNullable(mapper.mapRow(new Row(results)));
				}
				return Optional.empty();
			}, resolved);
		}

		@Override
		public List<String> getColumns() {
			return jdbcTemplate.execute(sql,
					(PreparedStatementCallback<List<String>>) ps -> {
						var md = ps.getMetaData();
						var names = new ArrayList<String>();
						for (int i = 1; i <= md.getColumnCount(); i++) {
							names.add(md.getColumnLabel(i));
						}
						return names;
					});
		}
	}

	private final class UpdateImpl extends StatementImpl implements Update {
		UpdateImpl(String sql, JdbcTemplate updateJdbcTemplate) {
			super(sql, updateJdbcTemplate);
		}

		@Override
		public int call(Object... arguments) {
			var resolved = resolveArguments(arguments);
			return jdbcTemplate.update(sql, resolved);
		}

		@Override
		public Optional<Integer> key(Object... arguments) {
			var resolved = resolveArguments(arguments);
			var keyHolder = new GeneratedKeyHolder();
			jdbcTemplate.update(con -> {
				var stmt = con.prepareStatement(sql, RETURN_GENERATED_KEYS);
				for (int i = 0; i < resolved.length; i++) {
					stmt.setObject(i + 1, resolved[i]);
				}
				return stmt;
			}, keyHolder);
			return Optional.ofNullable(keyHolder.getKey())
					.map(Number::intValue);
		}
	}

	/**
	 * Simple reader that loads a complex SQL query from a file.
	 *
	 * @param resource
	 *			The resource to load from
	 * @return The content of the resource
	 * @throws UncategorizedScriptException
	 *			 If the resource can't be loaded.
	 */
	private String readSQL(Resource resource) {
		try (var is = resource.getInputStream()) {
			var s = IOUtils.toString(is, UTF_8);
			return s;
		} catch (IOException e) {
			throw new UncategorizedScriptException(
					"could not load SQL file from " + resource, e);
		}
	}

	@Override
	public Connection getConnection() {
		return new ConnectionImpl(jdbcTemplate);
	}

	@Override
	public boolean isHistoricalDBAvailable() {
		return tombstoneJdbcTemplate != null;
	}

	@Override
	public Connection getHistoricalConnection() {
		return new ConnectionImpl(tombstoneJdbcTemplate);
	}

	@Override
	public void executeVoid(boolean lockForWriting, Connected operation) {
		try (var conn = getConnection()) {
			conn.transaction(lockForWriting, () -> {
				operation.act(conn);
				return this;
			});
		}
	}

	@Override
	public <T> T execute(boolean lockForWriting,
			ConnectedWithResult<T> operation) {
		try (var conn = getConnection()) {
			return conn.transaction(lockForWriting, () -> operation.act(conn));
		}
	}
}
