package uk.ac.manchester.spinnaker.alloc.db;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.IOUtils.serialize;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.datasource.init.UncategorizedScriptException;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;
import org.springframework.stereotype.Service;

import uk.ac.manchester.spinnaker.utils.MappableIterable;

@Service
@Primary
public class DatabaseEngineJDBCImpl implements DatabaseAPI {
	private static final Logger log = getLogger(DatabaseEngineJDBCImpl.class);

	@Value("classpath:/spalloc-mysql.sql")
	private Resource sqlDDLFile;

	/** The list of files containing schema updates. */
	@Value("classpath*:spalloc-schema-update-*.sql")
	private Resource[] schemaUpdates;

	@Value("classpath:/spalloc-tombstone.sql")
	private Resource tombstoneDDLFile;

	@Value("classpath:/spalloc-static-data.sql")
	private Resource sqlInitDataFile;

	private final JdbcTemplate jdbcTemplate;

	private final JdbcTemplate tombstoneJdbcTemplate;

	private DatabaseEngineJDBCImpl() {
		DataSource ds = DataSourceBuilder.create()
				.url("jdbc:sqlite:mem:data").build();
		DataSource tombstoneDs = DataSourceBuilder.create()
				.url("jdbc:sqlite:mem:tombstone").build();
		jdbcTemplate = new JdbcTemplate(ds);
		tombstoneJdbcTemplate = new JdbcTemplate(tombstoneDs);
		setup();
	}

	@Autowired
	public DatabaseEngineJDBCImpl(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.tombstoneJdbcTemplate = null;
	}

	@PostConstruct
	private void setup() {
		// Skip for now - use sql directly if needed
		/* String sql = readSQL(sqlDDLFile);
		for (String part : sql.split(";")) {
		    jdbcTemplate.execute(part + ";");
		} */
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
			operation.act();
		}

		@Override
		public void transaction(Transacted operation) {
			operation.act();
		}

		@Override
		public <T> T transaction(TransactedWithResult<T> operation) {
			return operation.act();
		}

		@Override
		public <T> T transaction(boolean lockForWriting, TransactedWithResult<T> operation) {
			return operation.act();
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

	private final class IgnorableSQLException extends SQLException {
		private static final long serialVersionUID = 1L;

		public IgnorableSQLException() {
			super("Ignorable");
		}
	}

	private final class PreparedStatementSaver
			implements PreparedStatementCreator {

		int parameterCount;

		Set<String> rowColumnNames = new HashSet<>();

		private final PreparedStatementCreator delegate;

		PreparedStatementSaver(String sql, Object... arguments) {
			List<Object> resolved = resolveArguments(arguments);
			delegate = new PreparedStatementCreatorFactory(sql)
					.newPreparedStatementCreator(resolved);
		}

		@Override
		public PreparedStatement createPreparedStatement(
				java.sql.Connection con) throws SQLException {
			PreparedStatement stmt = delegate.createPreparedStatement(con);
			parameterCount = stmt.getParameterMetaData().getParameterCount();
			rowColumnNames = columnNames(stmt.getMetaData());
			stmt.close();
			// Throw this as we don't want to actually use anything!
			throw new IgnorableSQLException();
		}

		Set<String> columnNames(ResultSetMetaData md) throws SQLException {
			var names = new LinkedHashSet<String>();
			for (int i = 1; i <= md.getColumnCount(); i++) {
				names.add(md.getColumnName(i));
			}
			return names;
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
		public MappableIterable<Row> call(Object... arguments) {
			List<Object> resolved = resolveArguments(arguments);
			SqlRowSet rs = jdbcTemplate.queryForRowSet(sql, resolved.toArray());

			return () -> new Iterator<Row>() {
				private boolean finished = false;
				private boolean consumed = true;
				// Share this row wrapper; underlying row changes
				private final Row row = new RowSQLRowSetImpl(rs, sql);

				@Override
				public boolean hasNext() {
					if (finished) {
						return false;
					}
					if (!consumed) {
						return true;
					}
					boolean result = rs.next();
					if (result) {
						consumed = false;
					} else {
						finished = true;
					}
					return result;
				}

				@Override
				public Row next() {
					if (finished) {
						throw new NoSuchElementException();
					}
					consumed = true;
					return row;
				}
			};
		}

		@Override
		public Optional<Row> call1(Object... arguments) {
			List<Object> resolved = resolveArguments(arguments);
			SqlRowSet rs = jdbcTemplate.queryForRowSet(sql, resolved.toArray());
			if (rs.next()) {
				return Optional.of(new RowSQLRowSetImpl(rs, sql));
			} else {
				return Optional.empty();
			}
		}

		@Override
		public int getNumArguments() {
			return getParameterCount(sql);
		}

		@Override
		public Set<String> getRowColumnNames() {
			return getColumnNames(sql);
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
		public MappableIterable<Integer> keys(Object... arguments) {
			List<Object> resolved = resolveArguments(arguments);
			KeyHolder keyHolder = new GeneratedKeyHolder();
			var pss = new PreparedStatementCreatorImpl(sql, resolved);
			jdbcTemplate.update(pss, keyHolder);
			return () -> keyHolder.getKeyList().stream().mapToInt(
					map -> Integer.class.cast(
							map.values().iterator().next())).iterator();
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
		public int getNumArguments() {
			return getParameterCount(sql);
		}

		@Override
		public Set<String> getRowColumnNames() {
			return getColumnNames(sql);
		}

		@Override
		public List<String> explainQueryPlan() {
			// TODO Auto-generated method stub
			return null;
		}

	}

	private int getParameterCount(String sql) {
		var pss = new PreparedStatementSaver(sql);
		try {
			jdbcTemplate.query(pss, (rs) -> {});
		} catch (DataAccessException e) {
			if (!(e.getMostSpecificCause() instanceof
					IgnorableSQLException)) {
				throw e;
			}
		}
		return pss.parameterCount;
	}

	private Set<String> getColumnNames(String sql) {
		var pss = new PreparedStatementSaver(sql);
		try {
			jdbcTemplate.query(pss, (rs) -> {});
		} catch (DataAccessException e) {
			if (!(e.getMostSpecificCause() instanceof
					IgnorableSQLException)) {
				throw e;
			}
		}
		return pss.rowColumnNames;
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

	@Override
	public DatabaseAPI getInMemoryDB() {
		return new DatabaseEngineJDBCImpl();
	}

	static Set<String> columnNames(SqlRowSetMetaData md) throws SQLException {
		var names = new LinkedHashSet<String>();
		for (int i = 1; i <= md.getColumnCount(); i++) {
			names.add(md.getColumnName(i));
		}
		return names;
	}

}
