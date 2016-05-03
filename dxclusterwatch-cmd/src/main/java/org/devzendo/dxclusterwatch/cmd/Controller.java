package org.devzendo.dxclusterwatch.cmd;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.devzendo.commoncode.concurrency.ThreadUtils;
import org.devzendo.dxclusterwatch.util.Signals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.misc.SignalHandler;
@SuppressWarnings("restriction")

public class Controller {
	private static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);
	
	private final AtomicBoolean running = new AtomicBoolean(true);
	private final CountDownLatch finished = new CountDownLatch(1);
	private final Config config;
	private final Persister persister;
	private final PageBuilder pageBuilder;
	private final Tweeter tweeter;
	private final SitePoller sitePoller;

	private final SignalHandler oldIntHandler;

	public Controller(final Config config, final Persister persister, final PageBuilder pageBuilder, final Tweeter tweeter, final SitePoller sitePoller) {
		this.config = config;
		this.persister = persister;
		this.pageBuilder = pageBuilder;
		this.tweeter = tweeter;
		this.sitePoller = sitePoller;

		this.oldIntHandler = Signals.withHandler(new Runnable() {
			@Override
			public void run() {
	            LOGGER.info("Interrupt received, stopping");
	            running.set(false);
			}}, Signals.SignalName.INT);
	}
	
	public void stop() {
		LOGGER.debug("Requesting stop");
		running.set(false);
		try {
			LOGGER.debug("Waiting for stop");
			finished.await();
			LOGGER.debug("Stopped");
		} catch (final InterruptedException e) {
			LOGGER.warn("Could not wait for stop: " + e.getMessage());
		}
		LOGGER.debug("Reinstating old signal handler");
		Signals.handle(oldIntHandler, Signals.SignalName.INT);
	}
	
	public void start() {
		long backoffCount = 0;
		long tweetBackoffCount = 0;
		long nextPollTime = nowSeconds(); // force the first poll.
		long nextTweet = nowSeconds(); // force the first tweet to happen now
		// transient counts of tweets and rebuilds...
		int tweetNumber = 1;
		int pageRebuildNumber = 1;
		
		LOGGER.info("Starting....");
		while (running.get()) {
			final int pollSeconds = config.getPollMinutes() * 60;
			if (nowSeconds() >= nextPollTime) {
				try {
					LOGGER.info("Polling DXCluster...");
					final ClusterRecord[] records = sitePoller.poll();
					backoffCount = 0;
					nextPollTime = nowSeconds() + pollSeconds;
					LOGGER.debug("Next poll in " + pollSeconds + " secs");
					if (records.length > 0) {
						LOGGER.debug("Persisting " + records.length + " records");
						final int newRecords = persister.persistRecords(records);
						if (newRecords > 0) {
							LOGGER.info("Rebuilding page #" + pageRebuildNumber);
							pageRebuildNumber++;
							pageBuilder.rebuildPage(records.length, newRecords);
							LOGGER.info("Publishing page...");
							pageBuilder.publishPage();
						}
					}					
				} catch (final RuntimeException re) {
					backoffCount ++;
					final long secs = 60 * backoffCount;
					nextPollTime = nowSeconds() + secs;
					LOGGER.warn("Could not poll cluster: " + re.getMessage() + ": next attempt in " + secs + " seconds");
				}
			}
			
			final int tweetSeconds = config.getTweetSeconds();
			if (nowSeconds() >= nextTweet) {
				final ClusterRecord nextRecordToTweet = persister.getNextRecordToTweet();
				if (nextRecordToTweet != null) {
					try {
						LOGGER.info("#" + tweetNumber + " - tweeting " + nextRecordToTweet.toDbString());
						tweeter.tweet(nextRecordToTweet);
						tweetBackoffCount = 0;
						tweetNumber++;
						persister.markTweeted(nextRecordToTweet);
						nextTweet = nowSeconds() + tweetSeconds;
					} catch (final RuntimeException re) {
						tweetBackoffCount ++;
						final long secs = 60 * tweetBackoffCount;
						nextTweet = nowSeconds() + secs;
						LOGGER.warn("Could not tweet " + nextRecordToTweet + ": " + re.getMessage() + ": next attempt in " + secs + " seconds");
					}
				}
			}

			ThreadUtils.waitNoInterruption(1000L);
		}
		
		LOGGER.info("Finishing");
	}

	private long nowSeconds() {
		return System.currentTimeMillis() / 1000;
	}
}
