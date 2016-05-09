package org.devzendo.dxclusterwatch.cmd;

import static org.devzendo.dxclusterwatch.cmd.TestController.LongCloseTo.closeTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.devzendo.commoncode.time.Sleeper;
import org.devzendo.dxclusterwatch.test.LoggingUnittest;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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
	
	private final Sleeper sleeper = new Sleeper(100);

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
	public void sitePollerFailureTriggersBackoff() throws Exception {
		configExpectations();
		final long start = nowSeconds();
		final List<Long> pollTimeOffsets = new ArrayList<>();
		when(sitePoller.poll()).thenAnswer(new Answer<ClusterRecord[]>() {
			@Override
			public ClusterRecord[] answer(final InvocationOnMock invocation) throws Throwable {
				final boolean failing = pollTimeOffsets.size() < 3;
				final long timeOffset = nowSeconds() - start;
				LOGGER.debug("Recording poll time offset of {}", timeOffset);
				pollTimeOffsets.add(timeOffset);
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

		sleeper.sleep(440000);
		controller.stop();

		assertThat(pollTimeOffsets, Matchers.hasSize(5));
		final long tolerance = 5L;
		assertThat(pollTimeOffsets.get(0), closeTo(0L, tolerance));
		assertThat(pollTimeOffsets.get(1), closeTo(60L, tolerance));  // 60
		assertThat(pollTimeOffsets.get(2), closeTo(180L, tolerance)); // 120
		assertThat(pollTimeOffsets.get(3), closeTo(360L, tolerance)); // 180
		assertThat(pollTimeOffsets.get(4), closeTo(420L, tolerance)); // back to 60
	}

	public static class LongCloseTo extends TypeSafeMatcher<Long> {
		private final Long value;
		private final Long delta;

		/**
		 * Creates a matcher of Long that matches when an examined Long is equal
		 * to the specified <code>operand</code>, within a range of +/-
		 * <code>error</code>.
		 * @param value
		 *            the expected value of matching Long
		 * @param error
		 *            the delta (+/-) within which matches will be allowed
		 */
		public static org.hamcrest.Matcher<Long> closeTo(final Long value, final Long error) {
			return new TestController.LongCloseTo(value, error);
		}

		public LongCloseTo(final Long value, final Long error) {
			this.value = value;
			this.delta = error;
		}

		@Override
		public void describeMismatchSafely(final Long item, final Description mismatchDescription) {
			mismatchDescription.appendValue(item).appendText(" differed by ").appendValue(actualDelta(item))
					.appendText(" more than delta ").appendValue(delta);
		}

		@Override
		public void describeTo(final Description description) {
			description.appendText("a numeric value within ").appendValue(delta).appendText(" of ").appendValue(value);
		}

		private Long actualDelta(final Long item) {
			return Math.abs(item - value) - delta;
		}

		@Override
		protected boolean matchesSafely(final Long item) {
		      return actualDelta(item) <= 0;
		}
	}

	@Test
	public void recordsReceivedFromSitePollerAreTweeted() throws Exception {
		configExpectations();
		when(sitePoller.poll()).thenReturn(records);
		when(persister.persistRecords(records)).thenReturn(2);
		when(persister.getNextRecordToTweet()).thenReturn(dbRecord1, dbRecord2, null);

		startController();

		sleeper.sleep(4000);
		controller.stop();

		verify(tweeter).tweet(dbRecord1);
		verify(persister).markTweeted(dbRecord1);
		verify(tweeter).tweet(dbRecord2);
		verify(persister).markTweeted(dbRecord2);
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
				controller = new Controller(config, persister, pageBuilder, tweeter, sitePoller, sleeper);
				controller.start();
			}
		});
		controllerThread.setDaemon(true);
		controllerThread.start();
	}

}
