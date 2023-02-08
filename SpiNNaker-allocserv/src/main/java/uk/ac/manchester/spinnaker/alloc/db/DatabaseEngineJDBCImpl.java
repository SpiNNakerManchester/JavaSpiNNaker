/*
 * Copyright (c) 2023 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.alloc.db;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.IOUtils.serialize;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.datasource.init.UncategorizedScriptException;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import uk.ac.manchester.spinnaker.utils.MappableIterable;

@Service
@Primary
public class DatabaseEngineJDBCImpl implements DatabaseAPI {
	private static final Logger log = getLogger(DatabaseEngineJDBCImpl.class);

	@Value("classpath:/spalloc-mysql.sql")
	private Resource sqlDDLFile;

	@Value("classpath:/spalloc-tombstone.sql")
	private Resource tombstoneDDLFile;

	@Value("classpath:/spalloc-static-data.sql")
	private Resource sqlInitDataFile;

	private final JdbcTemplate jdbcTemplate;

	private final JdbcTemplate tombstoneJdbcTemplate;

	private final TransactionTemplate transactionTemplate;

	@Autowired
	public DatabaseEngineJDBCImpl(JdbcTemplate jdbcTemplate,
			PlatformTransactionManager transactionManager) {
		this.jdbcTemplate = jdbcTemplate;
		this.tombstoneJdbcTemplate = null;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@PostConstruct
	private void setup() {
		log.info("Running Database setup...");
		// Skip for now - use sql directly if needed
		String sql = readSQL(sqlDDLFile);
		String[] lines = sql.split("\n");
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
			StringBuilder stmt = new StringBuilder();
			while (i < lines.length && !lines[i].startsWith("-- STMT")
					&& !lines[i].startsWith("-- IGNORE")) {
				String line = lines[i].trim();
				if (!line.startsWith("--") && !line.isEmpty()) {
					log.trace("DDL statement line {}", line);
					stmt.append(line);
					stmt.append('\n');
				}
				i++;
			}
			if (stmt.length() != 0) {
				String statement = stmt.toString();
				log.debug("Executing DDL Statement: {}", statement);
				jdbcTemplate.execute(statement);
			}
		}
	}

	private final class ConnectionImpl implements Connection {

		@Override
		public void close() {
			// Does nothing
		}

		@Override
		public Query query(String sql) {
			return new QueryImpl(sql);
		}

		@Override
		public Query query(Resource sqlResource) {
			return new QueryImpl(readSQL(sqlResource));
		}

		@Override
		public Update update(String sql) {
			return new UpdateImpl(sql);
		}

		@Override
		public Update update(Resource sqlResource) {
			return new UpdateImpl(readSQL(sqlResource));
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
		public <T> T transaction(boolean lockForWriting,
				TransactedWithResult<T> operation) {
			// TODO: Lock for writing is currently ignored
			return transactionTemplate.execute(status -> {
				return operation.act();
			});
		}

		@Override
		public boolean isHistoricalDBAvailable() {
			return tombstoneJdbcTemplate != null;
		}

		@Override
		public boolean isReadOnly() {
			return false;
		}

		@Override
		public void rollback() {
			// Does nothing
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

	private final class PreparedStatementCreatorImpl
	        implements PreparedStatementCreator {

		private final String query;

		private final List<Object> values;

		private PreparedStatementCreatorImpl(String query,
				List<Object> values) {
			this.query = query;
			this.values = values;
		}

		@Override
		public PreparedStatement createPreparedStatement(
				java.sql.Connection con) throws SQLException {
		    var stmt = con.prepareStatement(query,
		    		Statement.RETURN_GENERATED_KEYS);
		    for (int i = 0; i < values.size(); i++) {
		    	stmt.setObject(i + 1, values.get(i));
		    }
		    return stmt;
		}

	}

	private final class QueryImpl implements Query {

		private final String sql;

		QueryImpl(String sql) {
			this.sql = NamedParameterUtils.parseSqlStatementIntoString(sql);
		}

		@Override
		public void close() {
			// Does Nothing
		}

		@Override
		public <T> List<T> call(RowMapper<T> mapper, Object... arguments) {
			List<Object> resolved = resolveArguments(arguments);
			return jdbcTemplate.query(sql, (results) -> {
				List<T> values = new ArrayList<T>();
				while (results.next()) {
					values.add(mapper.mapRow(new Row(results)));
				}
				return values;
			}, resolved.toArray());
		}

		@Override
		public <T> Optional<T> call1(RowMapper<T> mapper, Object... arguments) {
			List<Object> resolved = resolveArguments(arguments);
			return jdbcTemplate.query(sql, (results) -> {
				if (results.next()) {
					// Allow nullable if there is a value
					return Optional.ofNullable(mapper.mapRow(new Row(results)));
				}
				return Optional.empty();
			}, resolved.toArray());
		}

		@Override
		public List<String> explainQueryPlan() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	private final class UpdateImpl implements Update {

		private String sql;

		UpdateImpl(String sql) {
			this.sql = NamedParameterUtils.parseSqlStatementIntoString(sql);
		}

		@Override
		public void close() {
			// Does nothing
		}

		@Override
		public int call(Object... arguments) {
			List<Object> resolved = resolveArguments(arguments);
			return jdbcTemplate.update(sql, resolved.toArray());
		}

		@Override
		public Optional<Integer> key(Object... arguments) {
			List<Object> resolved = resolveArguments(arguments);
			KeyHolder keyHolder = new GeneratedKeyHolder();
			var pss = new PreparedStatementCreatorImpl(sql, resolved);
			jdbcTemplate.update(pss, keyHolder);
			Number key = keyHolder.getKey();
			if (key == null) {
				return Optional.empty();
			}
			return Optional.of(key.intValue());
		}

		@Override
		public List<String> explainQueryPlan() {
			// TODO Auto-generated method stub
			return null;
		}

	}

	private List<Object> resolveArguments(Object[] arguments) {
		List<Object> resolved = new ArrayList<Object>();
		for (var arg : arguments) {
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
			resolved.add(arg);
		}
		return resolved;
	}

	/**
	 * Simple reader that loads a complex SQL query from a file.
	 *
	 * @param resource
	 *            The resource to load from
	 * @return The content of the resource
	 * @throws UncategorizedScriptException
	 *             If the resource can't be loaded.
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
		return new ConnectionImpl();
	}

	@Override
	public void executeVoid(boolean lockForWriting, Connected operation) {
		try (var conn = getConnection()) {
			operation.act(conn);
		}
	}

	@Override
	public <T> T execute(boolean lockForWriting, ConnectedWithResult<T> operation) {
		try (var conn = getConnection()) {
			return operation.act(conn);
		}
	}

	@Override
	public void createBackup(File backupFilename) {
		// TODO Auto-generated method stub

	}

	@Override
	public void restoreFromBackup(File backupFilename) {
		// TODO Auto-generated method stub

	}

	static Set<String> columnNames(SqlRowSetMetaData md) throws SQLException {
		var names = new LinkedHashSet<String>();
		for (int i = 1; i <= md.getColumnCount(); i++) {
			names.add(md.getColumnName(i));
		}
		return names;
	}

}
