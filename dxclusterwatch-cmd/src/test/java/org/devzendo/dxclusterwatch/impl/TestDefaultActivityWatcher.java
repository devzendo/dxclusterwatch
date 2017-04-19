package org.devzendo.dxclusterwatch.impl;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.sql.Timestamp;
import java.util.List;

import org.devzendo.dxclusterwatch.cmd.ActivityWatcher;
import org.devzendo.dxclusterwatch.cmd.ClusterRecord;
import org.devzendo.dxclusterwatch.impl.DefaultActivityWatcher.Stuff;
import org.junit.Before;
import org.junit.Test;

public class TestDefaultActivityWatcher {

	private final ActivityWatcher watcher = new DefaultActivityWatcher();
	
	private int count;
	@Before
	public void setUp() {
		count = 1;
	}

	private ClusterRecord gen(final String callsign, final Timestamp when, final String freq) {
		return ClusterRecord.dbRecord(count++, callsign, "M0CUV", when, freq, "Hi Matt");
	}
	
	@Test
	public void firstEntryIsReturned() {
		watcher.seen(gen("GB4IMD", minutesFromEpoch(20), "14060.3"));
		assertThat(watcher.latestTweetableActivity(), equalTo("GB4IMD (14060 00:20)"));
	}

	@Test
	public void tweetedEntriesAreNotReturnedNextTime() {
		watcher.seen(gen("GB4IMD", minutesFromEpoch(20), "14060.3"));
		watcher.latestTweetableActivity(); // ignore, this will mark dbRecord1 as tweeted.

		assertThat(watcher.latestTweetableActivity(), equalTo(""));
	}

	@Test
	public void newEntriesAreReturnedAfterSomeEmptiness() {
		watcher.seen(gen("GB4IMD", minutesFromEpoch(20), "14060.3"));
		watcher.latestTweetableActivity(); // ignore, this will mark dbRecord1 as tweeted.

		watcher.seen(gen("W51XYZ", minutesFromEpoch(80), "3754.3"));

		assertThat(watcher.latestTweetableActivity(), equalTo("W51XYZ (3754 01:20)"));
	}

	@Test
	public void heardOnMultipleFrequenciesAndTimes() {
		watcher.seen(gen("GB4IMD", minutesFromEpoch(20), "14060.3"));
		watcher.seen(gen("GB4IMD", minutesFromEpoch(13), "7040.7"));
		watcher.seen(gen("GB4IMD", minutesFromEpoch(24), "3580.2"));

		assertThat(watcher.latestTweetableActivity(), equalTo("GB4IMD (7040 00:13, 14060 00:20, 3580 00:24)"));
	}

	@Test
	public void splitIntoMultipleTweets() {
		watcher.seen(gen("GB4IMD", minutesFromEpoch(20), "14060.3"));
		watcher.seen(gen("GB4IMD", minutesFromEpoch(13), "7040.7"));
		watcher.seen(gen("GB4IMD", minutesFromEpoch(24), "3580.2"));
		watcher.seen(gen("GB4IMD", minutesFromEpoch(40), "4580.2"));
		watcher.seen(gen("GB4IMD", minutesFromEpoch(42), "4680.2"));
		watcher.seen(gen("GB4IMD", minutesFromEpoch(44), "4780.2"));
		watcher.seen(gen("GB4IMD", minutesFromEpoch(46), "4880.2"));
		watcher.seen(gen("GB4IMD", minutesFromEpoch(48), "4980.2"));
		watcher.seen(gen("GB4IMD", minutesFromEpoch(50), "5000.2"));
		watcher.seen(gen("GB4IMD", minutesFromEpoch(52), "5010.2"));
		watcher.seen(gen("GB4IMD", minutesFromEpoch(53), "100.2"));
		// --
		watcher.seen(gen("GB4IMD", minutesFromEpoch(57), "2000.2"));
		watcher.seen(gen("GB4IMD", minutesFromEpoch(58), "5000.2"));

		//                                                     0        1         2         3         4         5         6         7         8         9         0         1         2         3         4
		//                                                     12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
		assertThat(watcher.latestTweetableActivity(), equalTo("GB4IMD (7040 00:13, 14060 00:20, 3580 00:24, 4580 00:40, 4680 00:42, 4780 00:44, 4880 00:46, 4980 00:48, 5000 00:50, 5010 00:52, 100 00:53)"));
		assertThat(watcher.latestTweetableActivity(), equalTo("GB4IMD (2000 00:57, 5000 00:58)"));

		assertThat(watcher.latestTweetableActivity(), equalTo(""));
	}

	@Test
	public void splitMultipleCallsIntoMultipleTweets() {
		watcher.seen(gen("GB4IMD", minutesFromEpoch(20), "14060.3"));
		watcher.seen(gen("GB4IMD", minutesFromEpoch(13), "7040.7"));
		watcher.seen(gen("GB4IMD", minutesFromEpoch(24), "3580.2"));
		watcher.seen(gen("GB4IMD", minutesFromEpoch(40), "4580.2"));
		watcher.seen(gen("GB4IMD", minutesFromEpoch(42), "4680.2"));
		watcher.seen(gen("GB4IMD", minutesFromEpoch(44), "4780.2"));
		watcher.seen(gen("IZ1IMD", minutesFromEpoch(46), "4880.2"));
		watcher.seen(gen("IZ1IMD", minutesFromEpoch(48), "4980.2"));
		watcher.seen(gen("IZ1IMD", minutesFromEpoch(50), "5000.2"));
		watcher.seen(gen("IZ1IMD", minutesFromEpoch(52), "5010.2"));
		watcher.seen(gen("IZ1IMD", minutesFromEpoch(53), "100.2"));
		watcher.seen(gen("W1IMD", minutesFromEpoch(15), "2000.2"));
		watcher.seen(gen("W1IMD", minutesFromEpoch(8), "5000.2"));

		//                                                     0        1         2         3         4         5         6         7         8         9         0         1         2         3         4
		//                                                     12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
		assertThat(watcher.latestTweetableActivity(), equalTo("W1IMD (5000 00:08, 2000 00:15)\nGB4IMD (7040 00:13, 14060 00:20, 3580 00:24, 4580 00:40, 4680 00:42, 4780 00:44)\nIZ1IMD (4880 00:46)"));
		assertThat(watcher.latestTweetableActivity(), equalTo("IZ1IMD (4980 00:48, 5000 00:50, 5010 00:52, 100 00:53)"));

		assertThat(watcher.latestTweetableActivity(), equalTo(""));
	}

	@Test
	public void joinWhileStillFitsDoesNotFit() {
		final Stuff s1 = new Stuff(minutesFromEpoch(1), 0);
		
		assertThat(DefaultActivityWatcher.joinWhileStillFitsAndMarkTweeted(6, asList(s1)), equalTo(""));
		assertThat(s1.tweeted, equalTo(false));
	}

	@Test
	public void joinWhileStillFitsFits() {
		final Stuff s1 = new Stuff(minutesFromEpoch(1), 1);
		final Stuff s2 = new Stuff(minutesFromEpoch(2), 2);
		
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
		final Stuff s1 = new Stuff(minutesFromEpoch(1), 1);
		final Stuff s2 = new Stuff(minutesFromEpoch(2), 2);
		
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
		final Stuff s1 = new Stuff(minutesFromEpoch(1), 1);
		final Stuff s2 = new Stuff(minutesFromEpoch(2), 2);
		
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
	}
	
	@Test
	public void stuffTime() {
		assertThat(stuffStr(20), equalTo("0 00:20"));
		assertThat(stuffStr(60), equalTo("0 01:00"));
		assertThat(stuffStr(60 * 24 - 1), equalTo("0 23:59"));
	}

	private String stuffStr(final int minutesFromEpoch) {
		final Timestamp t = minutesFromEpoch(minutesFromEpoch);
		final DefaultActivityWatcher.Stuff stuff = new DefaultActivityWatcher.Stuff(t, 0);
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
}
