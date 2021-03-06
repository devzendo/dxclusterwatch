package org.devzendo.dxclusterwatch.cmd;

import static org.devzendo.dxclusterwatch.cmd.LongCloseTo.closeTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.devzendo.commoncode.time.Sleeper;
import org.devzendo.dxclusterwatch.cmd.ActivityWatcher.MarkPublished;
import org.devzendo.dxclusterwatch.test.LoggingUnittest;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.ClientHandlerException;

@RunWith(MockitoJUnitRunner.class)
public class TestController {

	private static Logger LOGGER = LoggerFactory.getLogger(TestController.class);

	@BeforeClass
	public static void setupLogging() {
		LoggingUnittest.initialise();
	}

	@After
	public void stopController() {
		if (controller != null) {
			controller.stop();
		}
		if (controllerThread.isAlive()) {
			controllerThread.interrupt();
		}
	}

	private final ClusterRecord dbRecord1 = ClusterRecord.dbRecord(1, "GB4IMD", "M0CUV", secsFromEpoch(20), "14060", "Hi Matt");
	private final ClusterRecord dbRecord2 = ClusterRecord.dbRecord(2, "GB3IMD", "M0CUV", secsFromEpoch(25), "7035", "UP 20");
	private final ClusterRecord[] records = new ClusterRecord[] { dbRecord1, dbRecord2 };

	private Thread controllerThread; // controller start() is blocking, so need to start it elsewhere.
	private volatile Controller controller;
	
	private Sleeper sleeper = new Sleeper(100);

	@Mock
	private Config config;
	@Mock
	private Persister persister;
	@Mock
	private PageBuilder pageBuilder;
	@Mock
	private Tweeter tweeter;
	@Mock
	private SitePoller sitePoller;
	@Mock
	private ActivityWatcher activityWatcher;

	@Test
	public void startsAndStops() {
		configExpectations();
		when(sitePoller.poll()).thenReturn(new ClusterRecord[0]);
		when(persister.getNextRecordToTweet()).thenReturn(null);

		startController();

		sleeper.sleep(2500);
		controller.stop();

		verify(config, atLeast(1)).getPollMinutes();
	}

	@Test
	public void recordsReceivedFromSitePollerAreHandled() throws Exception {
		configExpectations();
		when(config.isFeedReadingEnabled()).thenReturn(true);
		when(config.isPageUpdatingEnabled()).thenReturn(true);
		when(sitePoller.poll()).thenReturn(records);
		when(persister.persistRecords(records)).thenReturn(2);
		when(persister.getNextRecordToTweet()).thenReturn(null);

		startController();

		sleeper.sleep(2500);
		controller.stop();

		verify(pageBuilder).rebuildPage(2, 2);
		verify(pageBuilder).publishPage();
	}

	@Test
	public void publishingCanBeDisabled() throws Exception {
		configExpectations();
		when(config.isFeedReadingEnabled()).thenReturn(true);
		when(config.isPageUpdatingEnabled()).thenReturn(false);
		when(sitePoller.poll()).thenReturn(records);
		when(persister.persistRecords(records)).thenReturn(2);
		when(persister.getNextRecordToTweet()).thenReturn(null);

		startController();

		sleeper.sleep(2500);
		controller.stop();

		verify(pageBuilder, never()).rebuildPage(Mockito.anyInt(), Mockito.anyInt());
		verify(pageBuilder, never()).publishPage();
	}

	@Test
	public void sitePollerFailureTriggersBackoff() throws Exception {
		configExpectations();
		when(config.isFeedReadingEnabled()).thenReturn(true);
		final long start = nowSeconds();
		final List<Long> pollTimeOffsets = new ArrayList<>();
		final CountDownLatch done = new CountDownLatch(1);
		when(sitePoller.poll()).thenAnswer(new Answer<ClusterRecord[]>() {
			@Override
			public ClusterRecord[] answer(final InvocationOnMock invocation) throws Throwable {
				final boolean failing = pollTimeOffsets.size() < 3;
				final long timeOffset = nowSeconds() - start;
				LOGGER.debug("Recording poll time offset of {}", timeOffset);
				pollTimeOffsets.add(timeOffset);
				if (pollTimeOffsets.size() == 5) {
					LOGGER.debug("All records collected; stopping");
					done.countDown();
				}
				if (failing) {
					LOGGER.debug("Simulating connection failure");
					throw new ClientHandlerException("could not connect");
				} else {
					LOGGER.debug("Poll ok, but no data to return");
					return new ClusterRecord[0];
				}
			}
		});
		when(persister.getNextRecordToTweet()).thenReturn(null);

		startController();

		done.await();
		controller.stop();

		assertThat(pollTimeOffsets, Matchers.hasSize(5));
		final long tolerance = 5L;
		assertThat(pollTimeOffsets.get(0), closeTo(0L, tolerance));
		assertThat(pollTimeOffsets.get(1), closeTo(60L, tolerance));  // 60
		assertThat(pollTimeOffsets.get(2), closeTo(180L, tolerance)); // 120
		assertThat(pollTimeOffsets.get(3), closeTo(360L, tolerance)); // 180
		assertThat(pollTimeOffsets.get(4), closeTo(420L, tolerance)); // back to 60
	}

	@Test
	public void sitePollerFailureBackoffHasALimit() throws Exception {
		sleeper = new Sleeper(750);
		configExpectations();
		when(config.isFeedReadingEnabled()).thenReturn(true);
		final List<Long> pollIntervals = new ArrayList<>();
		final CountDownLatch done = new CountDownLatch(1);

		when(sitePoller.poll()).thenAnswer(new Answer<ClusterRecord[]>() {
			final long start = nowSeconds();
			final List<Long> pollTimeOffsets = new ArrayList<>();
			@Override
			public ClusterRecord[] answer(final InvocationOnMock invocation) throws Throwable {
				final long timeOffset = nowSeconds() - start;
				LOGGER.debug("Recording poll time offset of {}", timeOffset);
				pollTimeOffsets.add(timeOffset);
				final long pollInterval = pollTimeOffsets.get(pollTimeOffsets.size() - 1) - (pollTimeOffsets.size() == 1 ? 0 : pollTimeOffsets.get(pollTimeOffsets.size() - 2));
				pollIntervals.add(pollInterval);
				LOGGER.debug("Time since last {}", pollInterval);
				if (pollIntervals.size() == 13) {
					LOGGER.debug("All records collected; stopping");
					done.countDown();
				}
				throw new ClientHandlerException("could not connect");
			}
		});

		when(persister.getNextRecordToTweet()).thenReturn(null);

		startController();

		done.await();
		controller.stop();

		assertThat(pollIntervals, Matchers.hasSize(13));
		final long tolerance = 5L;
		assertThat(pollIntervals.get(0), closeTo(0L, tolerance));
		assertThat(pollIntervals.get(1), closeTo(60L, tolerance));
		assertThat(pollIntervals.get(2), closeTo(120L, tolerance));
		assertThat(pollIntervals.get(3), closeTo(180L, tolerance));
		assertThat(pollIntervals.get(4), closeTo(240L, tolerance));
		assertThat(pollIntervals.get(5), closeTo(300L, tolerance));
		assertThat(pollIntervals.get(6), closeTo(360L, tolerance));
		assertThat(pollIntervals.get(7), closeTo(420L, tolerance));
		assertThat(pollIntervals.get(8), closeTo(480L, tolerance));
		assertThat(pollIntervals.get(9), closeTo(540L, tolerance));
		assertThat(pollIntervals.get(10), closeTo(600L, tolerance));
		assertThat(pollIntervals.get(11), closeTo(600L, tolerance));
		assertThat(pollIntervals.get(12), closeTo(600L, tolerance));
	}

	@Test
	public void sitePollerCanBeDisabled() throws Exception {
		configExpectations();
		when(config.isFeedReadingEnabled()).thenReturn(true, true, false, false, true, true);

		final List<Long> pollTimeOffsets = new ArrayList<>();
		final CountDownLatch done = new CountDownLatch(1);

		when(sitePoller.poll()).thenAnswer(new Answer<ClusterRecord[]>() {
			final long start = nowSeconds();
			@Override
			public ClusterRecord[] answer(final InvocationOnMock invocation) throws Throwable {
				final long timeOffset = nowSeconds() - start;
				LOGGER.debug("Recording poll time offset of {}", timeOffset);
				pollTimeOffsets.add(timeOffset);
				if (pollTimeOffsets.size() == 4) {
					LOGGER.debug("All records collected; stopping");
					done.countDown();
				}
				return new ClusterRecord[0]; // nothing to return
			}
		});
		
		when(persister.getNextRecordToTweet()).thenReturn(null);

		startController();

		done.await();
		controller.stop();

		assertThat(pollTimeOffsets, Matchers.hasSize(4));
		final long tolerance = 5L;
		assertThat(pollTimeOffsets.get(0), closeTo(0L, tolerance));
		assertThat(pollTimeOffsets.get(1), closeTo(60L, tolerance));
		// ... a gap when polling was disabled ...
		assertThat(pollTimeOffsets.get(2), closeTo(240L, tolerance));
		assertThat(pollTimeOffsets.get(3), closeTo(300L, tolerance));
	}

	@Test
	public void tweetFailureTriggersBackoff() throws Exception {
		configExpectations();
		// lie a little - disable feed reading, but say there is a persisted
		// record to tweet.
		when(config.isFeedReadingEnabled()).thenReturn(false);
		when(config.isTweetingEnabled()).thenReturn(true);
		when(activityWatcher.latestTweetableActivity()).thenReturn("Tweet1", "Tweet2", "Tweet3", "Tweet4", "Tweet5", "Tweet6", "Tweet1", "Tweet2", "Tweet3", "Tweet4", "Tweet5", "Tweet6", "Tweet1", "Tweet2", "Tweet3", "Tweet4", "Tweet5", "Tweet6");
		when(persister.getNextRecordToTweet()).thenReturn(dbRecord1, dbRecord1, dbRecord1, dbRecord1, dbRecord1, null, null );
		final List<Long> tweetIntervals = new ArrayList<>();
		final CountDownLatch done = new CountDownLatch(1);

		Mockito.doAnswer(new Answer<Object>() {
			final long start = nowSeconds();
			final List<Long> tweetTimeOffsets = new ArrayList<>();
			@Override
			public Object answer(final InvocationOnMock invocation) throws Throwable {
				final boolean failing = tweetTimeOffsets.size() < 3;
				final long timeOffset = nowSeconds() - start;
				tweetTimeOffsets.add(timeOffset);
				final long tweetInterval = tweetTimeOffsets.get(tweetTimeOffsets.size() - 1) - (tweetTimeOffsets.size() == 1 ? 0 : tweetTimeOffsets.get(tweetTimeOffsets.size() - 2));
				tweetIntervals.add(tweetInterval);
				LOGGER.debug("Time since last tweet failure {}", tweetInterval);
				if (failing) {
					LOGGER.debug("Simulating connection failure");
					throw new RuntimeException("could not tweet");
				}
				// else, it's been tweeted, no exception, void return
				// but Mockito demands feeding
				LOGGER.debug("Tweet!");
				if (tweetIntervals.size() == 5) {
					LOGGER.debug("All records collected; stopping");
					done.countDown();
				}
				return null;
			}
		}).when(tweeter).tweetText(Mockito.anyString());

		startController();

		done.await();
		controller.stop();

		assertThat(tweetIntervals, Matchers.hasSize(5));
		final long tolerance = 5L;
		assertThat(tweetIntervals.get(0), closeTo(0L, tolerance));
		assertThat(tweetIntervals.get(1), closeTo(60L, tolerance));
		assertThat(tweetIntervals.get(2), closeTo(120L, tolerance));
		assertThat(tweetIntervals.get(3), closeTo(180L, tolerance));
		assertThat(tweetIntervals.get(4), closeTo(1L, tolerance)); // back to tweetSeconds (1)
	}

	@Test
	public void tweetFailureBackoffHasALimit() throws Exception {
		sleeper = new Sleeper(500);
		configExpectations();
		// lie a little - disable feed reading, but say there is a persisted
		// record to tweet.
		when(config.isFeedReadingEnabled()).thenReturn(false);
		when(config.isTweetingEnabled()).thenReturn(true);
		when(activityWatcher.latestTweetableActivity()).thenReturn("Tweet1", "Tweet2", "Tweet3", "Tweet4", "Tweet5", "Tweet6", "Tweet1", "Tweet2", "Tweet3", "Tweet4", "Tweet5", "Tweet6", "Tweet1", "Tweet2", "Tweet3", "Tweet4", "Tweet5", "Tweet6");
		when(persister.getNextRecordToTweet()).thenReturn(dbRecord1);

		final List<Long> tweetIntervals = new ArrayList<>();
		final CountDownLatch done = new CountDownLatch(1);

		Mockito.doAnswer(new Answer<Object>() {
			final long start = nowSeconds();
			final List<Long> tweetTimeOffsets = new ArrayList<>();
			@Override
			public Object answer(final InvocationOnMock invocation) throws Throwable {
				final long timeOffset = nowSeconds() - start;
				tweetTimeOffsets.add(timeOffset);
				final long tweetInterval = tweetTimeOffsets.get(tweetTimeOffsets.size() - 1) - (tweetTimeOffsets.size() == 1 ? 0 : tweetTimeOffsets.get(tweetTimeOffsets.size() - 2));
				tweetIntervals.add(tweetInterval);
				if (tweetIntervals.size() == 13) {
					LOGGER.debug("All data collected; stopping");
					done.countDown();
				}
				LOGGER.debug("Time since last tweet failure {}", tweetInterval);
				throw new RuntimeException("could not tweet");
			}
		}).when(tweeter).tweetText(Mockito.anyString());

		startController();

		done.await();
		controller.stop();

		assertThat(tweetIntervals, Matchers.hasSize(13));
		final long tolerance = 5L;
		assertThat(tweetIntervals.get(0), closeTo(0L, tolerance));
		assertThat(tweetIntervals.get(1), closeTo(60L, tolerance));
		assertThat(tweetIntervals.get(2), closeTo(120L, tolerance));
		assertThat(tweetIntervals.get(3), closeTo(180L, tolerance));
		assertThat(tweetIntervals.get(4), closeTo(240L, tolerance));
		assertThat(tweetIntervals.get(5), closeTo(300L, tolerance));
		assertThat(tweetIntervals.get(6), closeTo(360L, tolerance));
		assertThat(tweetIntervals.get(7), closeTo(420L, tolerance));
		assertThat(tweetIntervals.get(8), closeTo(480L, tolerance));
		assertThat(tweetIntervals.get(9), closeTo(540L, tolerance));
		assertThat(tweetIntervals.get(10), closeTo(600L, tolerance));
		assertThat(tweetIntervals.get(11), closeTo(600L, tolerance));
		assertThat(tweetIntervals.get(12), closeTo(600L, tolerance));
	}

	@Test
	public void tweetingCanBeDisabled() throws Exception {
		sleeper = new Sleeper(32); // small delay, needs better time accuracy
		configExpectations();
		when(config.getTweetSeconds()).thenReturn(5); // need a greater delay to get more accuracy
		
		// lie a little - disable feed reading, but say there is a persisted
		// record to tweet.
		when(config.isFeedReadingEnabled()).thenReturn(false);
		when(persister.getNextRecordToTweet()).thenReturn(dbRecord1);
		activityWatcher.seen(dbRecord1, new MarkPublished() {
			@Override
			public void markPublished(final ClusterRecord record) {
				// do nothing
			}});
		when(activityWatcher.latestTweetableActivity()).thenReturn("Tweet1", "Tweet2", "Tweet3", "Tweet4", "Tweet5", "Tweet6");
		when(config.isTweetingEnabled()).thenReturn(true, true, false, false, true, true);

		final List<Long> tweetTimeOffsets = new ArrayList<>();
		final CountDownLatch done = new CountDownLatch(1);

		Mockito.doAnswer(new Answer<Object>() {
			final long start = nowSeconds();
			@Override
			public Object answer(final InvocationOnMock invocation) throws Throwable {
				final long timeOffset = nowSeconds() - start;
				LOGGER.debug("Tweet! At offset {}", timeOffset);
				tweetTimeOffsets.add(timeOffset);
				if (tweetTimeOffsets.size() == 4) {
					LOGGER.debug("Got enough collected times; stopping");
					done.countDown();
				}
				// it's been tweeted, no exception, void return
				// but Mockito demands feeding
				return null;			
			}
		}).when(tweeter).tweetText(Mockito.anyString());

		startController();

		done.await();
		controller.stop();

		assertThat(tweetTimeOffsets, Matchers.hasSize(4));
		final long tolerance = 5L;
		assertThat(tweetTimeOffsets.get(0), closeTo(0L, tolerance));
		assertThat(tweetTimeOffsets.get(1), closeTo(5L, tolerance));
		// ... a gap when tweeting was disabled ...
		assertThat(tweetTimeOffsets.get(2), closeTo(20L, tolerance));
		assertThat(tweetTimeOffsets.get(3), closeTo(25L, tolerance));
	}

	@Test
	public void recordsReceivedFromSitePollerAreTranslatedMarkedAndTweeted() throws Exception {
		configExpectations();
		when(sitePoller.poll()).thenReturn(records);
		when(config.isTweetingEnabled()).thenReturn(true);
		when(persister.persistRecords(records)).thenReturn(2);
		when(activityWatcher.latestTweetableActivity()).thenReturn("Tweet1", "Tweet2", "Tweet3", "Tweet4", "Tweet5", "Tweet6");
		when(persister.getNextRecordToTweet()).thenReturn(dbRecord1, dbRecord2, null);
		setupActivityWatcherToMarkPublishedWhenSeen();
		startController();

		sleeper.sleep(4000);
		controller.stop();

		verify(tweeter).tweetText("Tweet1");
		// as in the next test, can't test that the persister is called, but that's tested elsewhere.
		//verify(persister).markTweeted(dbRecord1);
		verify(tweeter).tweetText("Tweet2");
		//verify(persister).markTweeted(dbRecord2);
	}

	@Test
	public void dontTweetTheSameAsLastTime() throws Exception {
		configExpectations();
		when(sitePoller.poll()).thenReturn(records);
		when(config.isTweetingEnabled()).thenReturn(true);
		when(persister.persistRecords(records)).thenReturn(2);
		when(activityWatcher.latestTweetableActivity()).thenReturn("Tweet", "Tweet");
		when(persister.getNextRecordToTweet()).thenReturn(dbRecord1, dbRecord2, null);
		setupActivityWatcherToMarkPublishedWhenSeen();
		startController();

		sleeper.sleep(4000);
		controller.stop();

		verify(tweeter).tweetText("Tweet");
		// can't test for this... but the calling of the MarkPublisher by the DefaultActivityWatcher is tested in its test.
//		verify(persister).markTweeted(dbRecord1);
//		verify(persister).markTweeted(dbRecord2);
	}

	private void setupActivityWatcherToMarkPublishedWhenSeen() {
		// do the callback
		Mockito.doAnswer(new Answer<Object>() {
			@Override
			public Object answer(final InvocationOnMock invocation) throws Throwable {
				final Object[] args = invocation.getArguments();
				final ClusterRecord rec = (ClusterRecord) args[0];
				final MarkPublished pub = (MarkPublished) args[1];
				pub.markPublished(rec);
				// and replicate the behaviour of the activitywatcher...
				System.out.println("persister mark tweeted " + rec);
				persister.markTweeted(rec);
				// no exception, void return
				// but Mockito demands feeding
				return null;			
			}
		}).when(activityWatcher).seen(Mockito.any(ClusterRecord.class), Mockito.any(MarkPublished.class));
	}

	private void configExpectations() {
		when(config.getPollMinutes()).thenReturn(1);
		when(config.getTweetSeconds()).thenReturn(1);
	}

	private long nowSeconds() {
		return sleeper.currentTimeMillis() / 1000;
	}

	private Timestamp secsFromEpoch(final long secondsFromEpoch) {
		final Timestamp when = new Timestamp(secondsFromEpoch * 1000);
		return when;
	}

	private void startController() {
		controllerThread = new Thread(new Runnable() {

			@Override
			public void run() {
				controller = new Controller(config, persister, pageBuilder, tweeter, sitePoller, sleeper, activityWatcher);
				controller.start();
			}
		});
		controllerThread.setDaemon(true);
		controllerThread.start();
	}

}
