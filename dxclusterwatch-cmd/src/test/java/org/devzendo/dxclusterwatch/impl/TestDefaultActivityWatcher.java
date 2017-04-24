package org.devzendo.dxclusterwatch.impl;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.devzendo.commoncode.time.Sleeper;
import org.devzendo.dxclusterwatch.cmd.ActivityWatcher;
import org.devzendo.dxclusterwatch.cmd.ActivityWatcher.MarkPublished;
import org.devzendo.dxclusterwatch.cmd.ClusterRecord;
import org.devzendo.dxclusterwatch.impl.DefaultActivityWatcher.Stuff;
import org.junit.Before;
import org.junit.Test;

public class TestDefaultActivityWatcher {
	private static final int SECONDS = 1000; // in ms
	private static final int MINUTES = 60 * SECONDS; // in ms
	private static final long IRRELEVANT_EXPIRY_TIME = 80 * MINUTES;
	private static final ClusterRecord IRRELEVANT_CLUSTER_RECORD = null;
	
	private final Sleeper sleeper = new Sleeper(100);
	private final ActivityWatcher watcher = new DefaultActivityWatcher(sleeper);
	
	private int count;
	private final MarkPublished doNothingToMarkTweeted = new MarkPublished() {
		@Override
		public void markPublished(final ClusterRecord record) {
			// do nothing
		}};
		
	@Before
	public void setUp() {
		count = 1;
	}

	private ClusterRecord gen(final String callsign, final Timestamp when, final String freq) {
		return ClusterRecord.dbRecord(count++, callsign, "M0CUV", when, freq, "Hi Matt");
	}

	@Test
	public void firstEntryIsReturned() {
		watcher.seen(gen("GB4IMD", minutesFromEpoch(20), "14060.3"), doNothingToMarkTweeted);
		assertThat(watcher.latestTweetableActivity(), equalTo("GB4IMD (14060 00:20)"));
		assertThat(watcher.numEntries(), equalTo(1));
	}

	@Test
	public void tweetedEntriesAreNotReturnedNextTime() {
		watcher.seen(gen("GB4IMD", minutesFromEpoch(20), "14060.3"), doNothingToMarkTweeted);
		watcher.latestTweetableActivity(); // ignore, this will mark dbRecord1 as tweeted.

		assertThat(watcher.latestTweetableActivity(), equalTo(""));
		assertThat(watcher.numEntries(), equalTo(1));
	}

	@Test
	public void newEntriesAreReturnedAfterSomeEmptiness() {
		watcher.seen(gen("GB4IMD", minutesFromEpoch(20), "14060.3"), doNothingToMarkTweeted);
		watcher.latestTweetableActivity(); // ignore, this will mark dbRecord1 as tweeted.

		watcher.seen(gen("W51XYZ", minutesFromEpoch(80), "3754.3"), doNothingToMarkTweeted);

		assertThat(watcher.latestTweetableActivity(), equalTo("W51XYZ (3754 01:20)"));
	}

	private static class ClusterRecordWithDetectablePublishing implements MarkPublished {
		private final ClusterRecord rec;
		private boolean published = false;
		
		public boolean isPublished() {
			return published;
		}

		public ClusterRecordWithDetectablePublishing(final ClusterRecord rec) {
			this.rec = rec;
		}
		
		@Override
		public void markPublished(final ClusterRecord record) {
			assert (record == rec);
			published = true;
		}
	}
	
	private ClusterRecordWithDetectablePublishing genDP(final String callsign, final Timestamp when, final String freq) {
		final ClusterRecord dbRecord = gen(callsign, when, freq);
		return new ClusterRecordWithDetectablePublishing(dbRecord);
	}

	@Test
	public void heardOnMultipleFrequenciesAndTimes() {
		final ClusterRecordWithDetectablePublishing dp1 = genDP("GB4IMD", minutesFromEpoch(20), "14060.3");
		assertThat(dp1.isPublished(), equalTo(false));
		assertThat(watcher.seen(dp1.rec, dp1), equalTo(true));
		assertThat(dp1.isPublished(), equalTo(false));
		
		final ClusterRecordWithDetectablePublishing dp2 = genDP("GB4IMD", minutesFromEpoch(13), "7040.7");
		assertThat(dp2.isPublished(), equalTo(false));
		assertThat(watcher.seen(dp2.rec, dp2), equalTo(true));
		assertThat(dp2.isPublished(), equalTo(false));

		final ClusterRecordWithDetectablePublishing dp3 = genDP("GB4IMD", minutesFromEpoch(24), "3580.2");
		assertThat(dp3.isPublished(), equalTo(false));
		assertThat(watcher.seen(dp3.rec, dp3), equalTo(true));
		assertThat(dp3.isPublished(), equalTo(false));

		assertThat(watcher.latestTweetableActivity(), equalTo("GB4IMD (7040 00:13, 14060 00:20, 3580 00:24)"));
		assertThat(dp1.isPublished(), equalTo(true));
		assertThat(dp2.isPublished(), equalTo(true));
		assertThat(dp3.isPublished(), equalTo(true));
	}
	
	@Test
	public void heardOnMultipleTimesDoesNotReportAllButOnlyTheEarliestTime() {
		final ClusterRecordWithDetectablePublishing dp1 = genDP("GB4IMD", minutesFromEpoch(20), "14060.3");
		assertThat(dp1.isPublished(), equalTo(false));
		assertThat(watcher.seen(dp1.rec, dp1), equalTo(true));
		assertThat(dp1.isPublished(), equalTo(false));

		final ClusterRecordWithDetectablePublishing dp2 = genDP("GB4IMD", minutesFromEpoch(13), "14060.3");
		assertThat(dp2.isPublished(), equalTo(false));
		assertThat(watcher.seen(dp2.rec, dp2), equalTo(false));
		assertThat(dp2.isPublished(), equalTo(true)); // immediate discard duplicate
		
		final ClusterRecordWithDetectablePublishing dp3 = genDP("GB4IMD", minutesFromEpoch(24), "14060.3");
		assertThat(dp3.isPublished(), equalTo(false));
		assertThat(watcher.seen(dp3.rec, dp3), equalTo(false));
		assertThat(dp3.isPublished(), equalTo(true)); // immediate discard duplicate

		assertThat(watcher.latestTweetableActivity(), equalTo("GB4IMD (14060 00:20)"));
		assertThat(dp1.isPublished(), equalTo(true));
		assertThat(dp2.isPublished(), equalTo(true));
		assertThat(dp3.isPublished(), equalTo(true));
	}

	@Test
	public void splitIntoMultipleTweetsAndMarkAsPublishIndividualRecordsWhenTheyGoIntoATweet() {
		final TweetMarkPublishCounter firstTweetBatchCounter = new TweetMarkPublishCounter();
		watcher.seen(gen("GB4IMD", minutesFromEpoch(20), "14060.3"), firstTweetBatchCounter);
		watcher.seen(gen("GB4IMD", minutesFromEpoch(13), "7040.7"), firstTweetBatchCounter);
		watcher.seen(gen("GB4IMD", minutesFromEpoch(24), "3580.2"), firstTweetBatchCounter);
		watcher.seen(gen("GB4IMD", minutesFromEpoch(40), "4580.2"), firstTweetBatchCounter);
		watcher.seen(gen("GB4IMD", minutesFromEpoch(42), "4680.2"), firstTweetBatchCounter);
		watcher.seen(gen("GB4IMD", minutesFromEpoch(44), "4780.2"), firstTweetBatchCounter);
		watcher.seen(gen("GB4IMD", minutesFromEpoch(46), "4880.2"), firstTweetBatchCounter);
		watcher.seen(gen("GB4IMD", minutesFromEpoch(48), "4980.2"), firstTweetBatchCounter);
		watcher.seen(gen("GB4IMD", minutesFromEpoch(50), "5000.2"), firstTweetBatchCounter);
		watcher.seen(gen("GB4IMD", minutesFromEpoch(52), "5010.2"), firstTweetBatchCounter);
		watcher.seen(gen("GB4IMD", minutesFromEpoch(53), "100.2"), firstTweetBatchCounter);
		assertThat(firstTweetBatchCounter.count(), equalTo(0));
		// --
		final TweetMarkPublishCounter secondTweetBatchCounter = new TweetMarkPublishCounter();
		watcher.seen(gen("GB4IMD", minutesFromEpoch(57), "2000.2"), secondTweetBatchCounter);
		watcher.seen(gen("GB4IMD", minutesFromEpoch(58), "6000.2"), secondTweetBatchCounter);
		assertThat(firstTweetBatchCounter.count(), equalTo(0));
		assertThat(secondTweetBatchCounter.count(), equalTo(0));
		
		assertThat(watcher.numEntries(), equalTo(13));

		//                                                     0        1         2         3         4         5         6         7         8         9         0         1         2         3         4
		//                                                     12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
		assertThat(watcher.latestTweetableActivity(), equalTo("GB4IMD (7040 00:13, 14060 00:20, 3580 00:24, 4580 00:40, 4680 00:42, 4780 00:44, 4880 00:46, 4980 00:48, 5000 00:50, 5010 00:52, 100 00:53)"));
		assertThat(firstTweetBatchCounter.count(), equalTo(11)); // what set?
		assertThat(secondTweetBatchCounter.count(), equalTo(0));

		assertThat(watcher.latestTweetableActivity(), equalTo("GB4IMD (2000 00:57, 6000 00:58)"));

		assertThat(watcher.latestTweetableActivity(), equalTo(""));

		assertThat(firstTweetBatchCounter.count(), equalTo(11));
		assertThat(firstTweetBatchCounter.frequencies, containsInAnyOrder(7040, 14060, 3580, 4580, 4680, 4780, 4880, 4980, 5000, 5010, 100));
		assertThat(secondTweetBatchCounter.count(), equalTo(2));
		assertThat(secondTweetBatchCounter.frequencies, containsInAnyOrder(2000, 6000));
	}
	
	private class TweetMarkPublishCounter implements MarkPublished {
		final Set<Integer> frequencies = new HashSet<>();

		public int count() {
			return frequencies.size();
		}
		
		@Override
		public void markPublished(final ClusterRecord record) {
			System.out.println("Marking as published: " + record);
			frequencies.add(DefaultActivityWatcher.toInt(record.getFreq()));
		}
	}
	
	@Test
	public void splitMultipleCallsIntoMultipleTweets() {
		watcher.seen(gen("GB4IMD", minutesFromEpoch(20), "14060.3"), doNothingToMarkTweeted);
		watcher.seen(gen("GB4IMD", minutesFromEpoch(13), "7040.7"), doNothingToMarkTweeted);
		watcher.seen(gen("GB4IMD", minutesFromEpoch(24), "3580.2"), doNothingToMarkTweeted);
		watcher.seen(gen("GB4IMD", minutesFromEpoch(40), "4580.2"), doNothingToMarkTweeted);
		watcher.seen(gen("GB4IMD", minutesFromEpoch(42), "4680.2"), doNothingToMarkTweeted);
		watcher.seen(gen("GB4IMD", minutesFromEpoch(44), "4780.2"), doNothingToMarkTweeted);
		watcher.seen(gen("IZ1IMD", minutesFromEpoch(46), "4880.2"), doNothingToMarkTweeted);
		watcher.seen(gen("IZ1IMD", minutesFromEpoch(48), "4980.2"), doNothingToMarkTweeted);
		watcher.seen(gen("IZ1IMD", minutesFromEpoch(50), "5000.2"), doNothingToMarkTweeted);
		watcher.seen(gen("IZ1IMD", minutesFromEpoch(52), "5010.2"), doNothingToMarkTweeted);
		watcher.seen(gen("IZ1IMD", minutesFromEpoch(53), "100.2"), doNothingToMarkTweeted);
		watcher.seen(gen("W1IMD", minutesFromEpoch(15), "2000.2"), doNothingToMarkTweeted);
		watcher.seen(gen("W1IMD", minutesFromEpoch(8), "5000.2"), doNothingToMarkTweeted);

		//                                                     0        1         2         3         4         5         6         7         8         9         0         1         2         3         4
		//                                                     12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
		assertThat(watcher.latestTweetableActivity(), equalTo("W1IMD (5000 00:08, 2000 00:15)\nGB4IMD (7040 00:13, 14060 00:20, 3580 00:24, 4580 00:40, 4680 00:42, 4780 00:44)\nIZ1IMD (4880 00:46)"));
		assertThat(watcher.latestTweetableActivity(), equalTo("IZ1IMD (4980 00:48, 5000 00:50, 5010 00:52, 100 00:53)"));

		assertThat(watcher.latestTweetableActivity(), equalTo(""));
	}

	@Test
	public void joinWhileStillFitsDoesNotFit() {
		final Stuff s1 = new Stuff(IRRELEVANT_CLUSTER_RECORD, minutesFromEpoch(1), 0, IRRELEVANT_EXPIRY_TIME, doNothingToMarkTweeted);
		
		assertThat(DefaultActivityWatcher.joinWhileStillFitsAndMarkTweeted(6, asList(s1)), equalTo(""));
		assertThat(s1.tweeted, equalTo(false));
	}

	@Test
	public void joinWhileStillFitsFits() {
		final Stuff s1 = new Stuff(IRRELEVANT_CLUSTER_RECORD, minutesFromEpoch(1), 1, IRRELEVANT_EXPIRY_TIME, doNothingToMarkTweeted);
		final Stuff s2 = new Stuff(IRRELEVANT_CLUSTER_RECORD, minutesFromEpoch(2), 2, IRRELEVANT_EXPIRY_TIME, doNothingToMarkTweeted);
		
		final List<Stuff> list = asList(s1, s2);
		
		assertThat(DefaultActivityWatcher.joinWhileStillFitsAndMarkTweeted(7, list), equalTo("1 00:01"));
		assertThat(s1.tweeted, equalTo(true));
		assertThat(s2.tweeted, equalTo(false));

		assertThat(DefaultActivityWatcher.joinWhileStillFitsAndMarkTweeted(7, list), equalTo("2 00:02"));
		assertThat(s1.tweeted, equalTo(true));
		assertThat(s2.tweeted, equalTo(true));
	}

	@Test
	public void joinWhileStillFitsCantQuiteJoin() {
		final Stuff s1 = new Stuff(IRRELEVANT_CLUSTER_RECORD, minutesFromEpoch(1), 1, IRRELEVANT_EXPIRY_TIME, doNothingToMarkTweeted);
		final Stuff s2 = new Stuff(IRRELEVANT_CLUSTER_RECORD, minutesFromEpoch(2), 2, IRRELEVANT_EXPIRY_TIME, doNothingToMarkTweeted);
		
		final List<Stuff> list = asList(s1, s2);
		
		assertThat(DefaultActivityWatcher.joinWhileStillFitsAndMarkTweeted(15, list), equalTo("1 00:01"));
		assertThat(s1.tweeted, equalTo(true));
		assertThat(s2.tweeted, equalTo(false));

		assertThat(DefaultActivityWatcher.joinWhileStillFitsAndMarkTweeted(15, list), equalTo("2 00:02"));
		assertThat(s1.tweeted, equalTo(true));
		assertThat(s2.tweeted, equalTo(true));
	}


	@Test
	public void joinWhileStillFitsJoins() {
		final Stuff s1 = new Stuff(IRRELEVANT_CLUSTER_RECORD, minutesFromEpoch(1), 1, IRRELEVANT_EXPIRY_TIME, doNothingToMarkTweeted);
		final Stuff s2 = new Stuff(IRRELEVANT_CLUSTER_RECORD, minutesFromEpoch(2), 2, IRRELEVANT_EXPIRY_TIME, doNothingToMarkTweeted);
		
		final List<Stuff> list = asList(s1, s2);
		
		assertThat(DefaultActivityWatcher.joinWhileStillFitsAndMarkTweeted(16, list), equalTo("1 00:01, 2 00:02"));
		assertThat(s1.tweeted, equalTo(true));
		assertThat(s2.tweeted, equalTo(true));

		assertThat(DefaultActivityWatcher.joinWhileStillFitsAndMarkTweeted(15, list), equalTo(""));
		assertThat(s1.tweeted, equalTo(true));
		assertThat(s2.tweeted, equalTo(true));
	}

	@Test
	public void emptyWatcher() {
		assertThat(watcher.latestTweetableActivity(), equalTo(""));
		assertThat(watcher.numEntries(), equalTo(0));
	}
	
	@Test
	public void stuffTime() {
		assertThat(stuffStr(20), equalTo("0 00:20"));
		assertThat(stuffStr(60), equalTo("0 01:00"));
		assertThat(stuffStr(60 * 24 - 1), equalTo("0 23:59"));
	}

	private String stuffStr(final int minutesFromEpoch) {
		final Timestamp t = minutesFromEpoch(minutesFromEpoch);
		final DefaultActivityWatcher.Stuff stuff = new DefaultActivityWatcher.Stuff(IRRELEVANT_CLUSTER_RECORD, t, 0, IRRELEVANT_EXPIRY_TIME, doNothingToMarkTweeted);
		return stuff.toString();
	}
	
	@Test
	public void toIntTests() {
		assertThat(DefaultActivityWatcher.toInt("12"), equalTo(12));
		assertThat(DefaultActivityWatcher.toInt(""), equalTo(0));
		assertThat(DefaultActivityWatcher.toInt(null), equalTo(0));
		assertThat(DefaultActivityWatcher.toInt("  "), equalTo(0));
		assertThat(DefaultActivityWatcher.toInt("12.4"), equalTo(12));
	}
	
	private Timestamp minutesFromEpoch(final long minutesFromEpoch) {
		final Timestamp when = new Timestamp(minutesFromEpoch * 60000);
		return when;
	}
	
	@Test
	public void purgeOfTweetedEntriesHappensAfter30Mins() {
		// purge time is 30 mins after calling seen (based on the Sleeper's time),
		// NOT 30 mins after the timestamp in the ClusterRecord.
		// purging is done on each new seen(..) or latestTweetableActivity()
		watcher.seen(gen("GB4IMD", minutesFromEpoch(20), "14060.3"), doNothingToMarkTweeted);
		watcher.seen(gen("GB4IMD", minutesFromEpoch(13), "7040.7"), doNothingToMarkTweeted);
		watcher.seen(gen("GB4IMD", minutesFromEpoch(24), "3580.2"), doNothingToMarkTweeted);

		assertThat(watcher.numEntries(), equalTo(3));

		sleeper.sleep(29 * MINUTES);		
		watcher.latestTweetableActivity(); // ignore result
		assertThat(watcher.numEntries(), equalTo(3)); // still there
		assertThat(watcher.numCallsigns(), equalTo(1));
		
		watcher.seen(gen("XYZIMD", minutesFromEpoch(40), "4580.2"), doNothingToMarkTweeted);
		assertThat(watcher.numEntries(), equalTo(4));
		assertThat(watcher.numCallsigns(), equalTo(2));
		
		sleeper.sleep(8 * MINUTES);
		watcher.latestTweetableActivity(); // ignore result
		assertThat(watcher.numEntries(), equalTo(1)); // original 3 gone
		assertThat(watcher.numCallsigns(), equalTo(1));

		sleeper.sleep(23 * MINUTES);
		watcher.latestTweetableActivity(); // ignore result
		assertThat(watcher.numEntries(), equalTo(0)); // one-off gone
		assertThat(watcher.numCallsigns(), equalTo(0));
	}

	@Test
	public void purgeOfUntweetedEntriesDoesNotHappensAfter30Mins() {
		// purge time is 30 mins after calling seen (based on the Sleeper's time),
		// NOT 30 mins after the timestamp in the ClusterRecord.
		// purging is called explicitly here. Note same timing structure as above.
		// but latestTweetableActivity is not called in this test, so entries do not
		// get purged.
		watcher.seen(gen("GB4IMD", minutesFromEpoch(20), "14060.3"), doNothingToMarkTweeted);
		watcher.seen(gen("GB4IMD", minutesFromEpoch(13), "7040.7"), doNothingToMarkTweeted);
		watcher.seen(gen("GB4IMD", minutesFromEpoch(24), "3580.2"), doNothingToMarkTweeted);

		assertThat(watcher.numEntries(), equalTo(3));

		sleeper.sleep(29 * MINUTES);		
		watcher.purge();
		assertThat(watcher.numEntries(), equalTo(3)); // still there

		watcher.seen(gen("GB4IMD", minutesFromEpoch(40), "4580.2"), doNothingToMarkTweeted);
		assertThat(watcher.numEntries(), equalTo(4));

		sleeper.sleep(8 * MINUTES);
		watcher.purge();
		assertThat(watcher.numEntries(), equalTo(4));

		sleeper.sleep(23 * MINUTES);
		watcher.purge();
		assertThat(watcher.numEntries(), equalTo(4));
		
		assertThat(watcher.numCallsigns(), equalTo(1));
	}
}
