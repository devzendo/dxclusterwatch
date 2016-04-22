package org.devzendo.dxclusterwatch.cmd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

import org.h2.engine.ExistenceChecker;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestH2Persister {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestH2Persister.class);

	@Rule
	public final TemporaryFolder tempDir = new TemporaryFolder();
	private File root;
	private Persister store;

	final ClusterRecord dbRecord1 = ClusterRecord.dbRecord(1, "GB4IMD", "M0CUV", when(20), "14060", "Hi Matt");
	final ClusterRecord dbRecord2 = ClusterRecord.dbRecord(2, "GB3IMD", "M0CUV", when(25), "7035", "UP 20");
	final ClusterRecord dbRecord3 = ClusterRecord.dbRecord(3, "GB3MRS", "G0VAR", when(35), "10118", "VY 73 OM");
	final ClusterRecord dbRecord4 = ClusterRecord.dbRecord(4, "IY0GM", "F6IIS", when(40), "3580", "Ciao Guglielmo");
	final ClusterRecord dbRecord5 = ClusterRecord.dbRecord(5, "DL4IMD", "F6IIS", when(45), "7040", "Sehr gut");
	final ClusterRecord dbRecord6 = ClusterRecord.dbRecord(6, "GB4IMD", "G7JFJ", when(47), "10220", "QRM");
	final ClusterRecord dbRecord7 = ClusterRecord.dbRecord(7, "IY0GM", "G7JFJ", when(50), "3585", "TNX FER QSL, OM");

	@BeforeClass
	public static void setupLogging() {
		LoggingUnittest.initialise();
	}

	@Before
	public void setupTempfile() throws IOException {
		tempDir.create();
		root = tempDir.getRoot();
		LOGGER.info("db is at " + root.getAbsolutePath());
		store = new H2Persister(root, 5);
	}

	@After
	public void closeDb() {
		store.close();
	}

	@Test
	public void dbFileExists() {
		assertThat(ExistenceChecker.exists(new File(root, "dxclusterwatch").getAbsolutePath()), equalTo(true));
	}

	@Test
	public void newRecordIsTrueOnFirstPersist() throws Exception {
		assertThat(store.persistRecords(new ClusterRecord[] { dbRecord1 }), equalTo(1));
	}

	@Test
	public void newRecordIsFalseOnSecondPersist() throws Exception {
		assertThat(store.persistRecords(new ClusterRecord[] { dbRecord1 }), equalTo(1));
		assertThat(store.persistRecords(new ClusterRecord[] { dbRecord1 }), equalTo(0));
	}

	@Test
	public void countOfNewRecords() throws Exception {
		assertThat(store.persistRecords(new ClusterRecord[] { dbRecord1, dbRecord2, dbRecord3, dbRecord3, dbRecord4 }), equalTo(4));
		assertThat(store.persistRecords(new ClusterRecord[] { dbRecord1 }), equalTo(0));
	}

	@Test
	public void tweetSequence() throws Exception {
		store.persistRecords(new ClusterRecord[] { dbRecord1, dbRecord4, dbRecord2, dbRecord3 });
		
		final ClusterRecord t1 = store.getNextRecordToTweet();
		assertThat(t1.getNr(), equalTo("1"));
		
		store.markTweeted(t1);
		
		final ClusterRecord t2 = store.getNextRecordToTweet();
		assertThat(t2.getNr(), equalTo("2"));

		store.markTweeted(t2);
		
		final ClusterRecord t3 = store.getNextRecordToTweet();
		assertThat(t3.getNr(), equalTo("3"));

		store.markTweeted(t3);
		
		final ClusterRecord t4a = store.getNextRecordToTweet();
		assertThat(t4a.getNr(), equalTo("4"));

		// not marked, get it again
		final ClusterRecord t4b = store.getNextRecordToTweet();
		assertThat(t4b.getNr(), equalTo("4"));

		store.markTweeted(t4b);

		assertThat(store.getNextRecordToTweet(), nullValue());
	}

	@Test
	public void readRecords() throws Exception {
		store.persistRecords(new ClusterRecord[] { dbRecord1, dbRecord3, dbRecord2, dbRecord4 });
		
		final List<ClusterRecord> records = store.getRecords();
		assertThat(records, hasSize(4));
		
		assertThat(records.get(0).getNr(), equalTo("4"));
		assertThat(records.get(1).getNr(), equalTo("3"));
		assertThat(records.get(2).getNr(), equalTo("2"));
		assertThat(records.get(3).getNr(), equalTo("1"));
	}

	@Test
	public void readRecordsIsLimitedByConfig() throws Exception {
		store.persistRecords(new ClusterRecord[] { dbRecord1, dbRecord3, dbRecord2, dbRecord4, dbRecord5, dbRecord6, dbRecord7 });
		
		final List<ClusterRecord> records = store.getRecords();
		assertThat(records, hasSize(5));
		
		assertThat(records.get(0).getNr(), equalTo("7"));
		assertThat(records.get(1).getNr(), equalTo("6"));
		assertThat(records.get(2).getNr(), equalTo("5"));
		assertThat(records.get(3).getNr(), equalTo("4"));
		assertThat(records.get(4).getNr(), equalTo("3"));
	}

	private Timestamp when(final long secondsFromEpoch) {
		final Timestamp when = new Timestamp(secondsFromEpoch * 1000);
		return when;
	}
}
