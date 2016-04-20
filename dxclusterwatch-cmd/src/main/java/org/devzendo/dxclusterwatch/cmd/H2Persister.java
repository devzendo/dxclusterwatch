package org.devzendo.dxclusterwatch.cmd;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.h2.engine.ExistenceChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class H2Persister implements Persister {

	private static final Logger LOGGER = LoggerFactory.getLogger(H2Persister.class);

	private final File dbFile;
	private final SimpleJdbcTemplate template;
	private final SingleConnectionDataSource dataSource;
	private final RowMapper<ClusterRecord> rowMapper;

	public H2Persister(final File storeDir) {
		dbFile = new File(storeDir, "dxclusterwatch");
		final boolean needToCreate = !exists();
		final String dbURL = "jdbc:h2:" + dbFile.getAbsolutePath();
		LOGGER.debug("Opening database at {}", dbFile.getAbsolutePath());
		dataSource = new SingleConnectionDataSource(dbURL, "sa", "", false);
		template = new SimpleJdbcTemplate(dataSource);
		if (needToCreate) {
			LOGGER.debug("Database not initialised; creating tables...");
			create();
		} else {
			LOGGER.debug("Database open");
		}
		rowMapper = new RowMapper<ClusterRecord>() {
			@Override
			public ClusterRecord mapRow(final ResultSet rs, final int rowNum) throws SQLException {
				final int nr = rs.getInt("nr");
				final String dxcall = rs.getString("dxcall");
				final String call = rs.getString("call");
				final Timestamp when = rs.getTimestamp("when");
				final int freq = rs.getInt("freq");
				final String comment = rs.getString("comment");
				return ClusterRecord.dbRecord(nr, dxcall, call, when.toLocalDateTime(), freq, comment);
			}
		};

	}

	private boolean exists() {
		return ExistenceChecker.exists(dbFile.getAbsolutePath());
	}

	private void create() {
		LOGGER.info("Creating database...");
		final String[] ddls = new String[] {
			"CREATE TABLE Spots(nr INT, dxcall VARCHAR(25), call VARCHAR(25), when TIMESTAMP, freq INT, comment VARCHAR(128), tweeted BOOLEAN, PRIMARY KEY(nr))",
		};
		for (final String ddl : ddls) {
			template.getJdbcOperations().execute(ddl);
		}
	}

	@Override
	public boolean persistRecords(final ClusterRecord[] records) {
		int newRecords = 0;
		for (final ClusterRecord record : records) {
			if (!recordExists(record)) {
				newRecords++;
				storeRecord(record);
			}
		}
		LOGGER.info("{} records persisted, {} new", records.length, newRecords);
		return newRecords > 0;
	}

	private void storeRecord(final ClusterRecord record) {
		LOGGER.debug("Storing record {}", record.toDbString());
		final String sql = "INSERT INTO Spots (nr, dxcall, call, when, freq, comment, tweeted) VALUES (?, ?, ?, ?, ?, ?, ?)";
		template.getJdbcOperations().update(
				sql, 
				Integer.parseInt(record.getNr()), 
				StringUtils.defaultString(record.getDxcall()),
				StringUtils.defaultString(record.getCall()),
				StringUtils.defaultString(record.getTime()),
				Integer.parseInt(record.getFreq()),
				StringUtils.defaultString(record.getComment()),
				false);
	}

	private boolean recordExists(final ClusterRecord record) {
		final int nr = Integer.parseInt(record.getNr());
		LOGGER.debug("Does record {} exist?", nr);
		final boolean exists = template.queryForInt("SELECT COUNT(*) FROM Spots WHERE nr = ?", nr) == 1;
		LOGGER.debug("Record {} {}", nr, exists ? "exists" : "does not exist");
		return exists;
	}

	@Override
	public ClusterRecord getNextRecordToTweet() {
		final String sql = "SELECT TOP 1 * FROM Spots WHERE tweeted = FALSE ORDER BY when";
		try {
			return template.queryForObject(sql, rowMapper);
		} catch (final IncorrectResultSizeDataAccessException e) {
			return null;
		}
	}

	@Override
	public void markTweeted(final ClusterRecord tweetedRecord) {
		final int nr = Integer.parseInt(tweetedRecord.getNr());
		LOGGER.debug("Marking record #{} as tweeted", nr);
		if (recordExists(tweetedRecord)) {
			final String sql = "UPDATE Spots SET tweeted = TRUE WHERE nr = ?";
			template.update(sql, nr);
		} else {
			LOGGER.warn("Record #{} does not exist to mark as tweeted", nr);
		}
	}

	@Override
	public void close() {
		LOGGER.debug("Closing db");
		try {
			 DataSourceUtils.getConnection(dataSource).close();
		} catch (CannotGetJdbcConnectionException | SQLException e) {
			LOGGER.warn("Failed to close db: {}", e.getMessage());
		}
	}

	@Override
	public List<ClusterRecord> getRecords() {
		final String sql = "SELECT * FROM Spots ORDER BY when DESC";
		return template.query(sql, rowMapper);
	}
}
