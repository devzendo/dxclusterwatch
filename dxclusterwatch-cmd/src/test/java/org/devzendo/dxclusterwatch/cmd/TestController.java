package org.devzendo.dxclusterwatch.cmd;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;

import org.devzendo.commoncode.concurrency.ThreadUtils;
import org.devzendo.dxclusterwatch.test.LoggingUnittest;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private final ClusterRecord[] records = new ClusterRecord[] {dbRecord1, dbRecord2};

	private Thread controllerThread; // controller start() is blocking, so need to start it elsewhere.
	private volatile Controller controller;

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
		
		ThreadUtils.waitNoInterruption(2500);
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

		ThreadUtils.waitNoInterruption(2500);
		controller.stop();
		
		verify(pageBuilder).rebuildPage(2, 2);
		verify(pageBuilder).publishPage();
	}

	@Test
	public void recordsReceivedFromSitePollerAreTweeted() throws Exception {
		configExpectations();
		when(sitePoller.poll()).thenReturn(records);
		when(persister.persistRecords(records)).thenReturn(2);
		when(persister.getNextRecordToTweet()).thenReturn(dbRecord1, dbRecord2, null);

		startController();

		ThreadUtils.waitNoInterruption(4000);
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

	private Timestamp secsFromEpoch(final long secondsFromEpoch) {
		final Timestamp when = new Timestamp(secondsFromEpoch * 1000);
		return when;
	}

	private void startController() {
		controllerThread = new Thread(new Runnable() {
		
			@Override
			public void run() {
				controller = new Controller(config, persister, pageBuilder, tweeter, sitePoller);
				controller.start();
			}});
		controllerThread.setDaemon(true);
		controllerThread.start();
	}

}
