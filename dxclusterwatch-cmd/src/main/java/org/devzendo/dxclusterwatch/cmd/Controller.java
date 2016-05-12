package org.devzendo.dxclusterwatch.cmd;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.devzendo.commoncode.time.Sleeper;
import org.devzendo.dxclusterwatch.util.Signals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.misc.SignalHandler;
@SuppressWarnings("restriction")

public class Controller {
	private static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);
	
	private final AtomicBoolean running = new AtomicBoolean(true);
	private final CountDownLatch stopEnded = new CountDownLatch(1);
	private final Config config;
	private final Persister persister;
	private final PageBuilder pageBuilder;
	private final Tweeter tweeter;
	private final SitePoller sitePoller;
	private final Sleeper sleeper;

	private final SignalHandler oldIntHandler;

	public Controller(final Config config, final Persister persister, final PageBuilder pageBuilder, final Tweeter tweeter, final SitePoller sitePoller, final Sleeper sleeper) {
		this.config = config;
		this.persister = persister;
		this.pageBuilder = pageBuilder;
		this.tweeter = tweeter;
		this.sitePoller = sitePoller;
		this.sleeper = sleeper;

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
			stopEnded.await();
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
				if (config.isFeedReadingEnabled()) {
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
								if (config.isPageUpdatingEnabled()) {
									LOGGER.info("Rebuilding page #" + pageRebuildNumber);
									pageRebuildNumber++;
									pageBuilder.rebuildPage(records.length, newRecords);
									LOGGER.info("Publishing page...");
									pageBuilder.publishPage();
								} else {
									LOGGER.info("Publishing of updated pages is disabled");
								}
							}
						}					
					} catch (final RuntimeException re) {
						// Don't increase backoff without bound
						if (backoffCount < 10) {
							backoffCount ++;
							LOGGER.debug("Poll backoff count now {}", backoffCount);
						}
						final long secs = 60 * backoffCount;
						nextPollTime = nowSeconds() + secs;
						LOGGER.warn("Could not poll cluster: " + re.getMessage() + ": next attempt in " + secs + " seconds");
					}
				} else {
					LOGGER.info("Polling of DXCluster is disabled");
					nextPollTime = nowSeconds() + pollSeconds;
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
						// Don't increase backoff without bound
						if (tweetBackoffCount < 10) {
							tweetBackoffCount ++;
							LOGGER.debug("Tweet backoff count now {}", tweetBackoffCount);
						}
						final long secs = 60 * tweetBackoffCount;
						nextTweet = nowSeconds() + secs;
						LOGGER.warn("Could not tweet " + nextRecordToTweet + ": " + re.getMessage() + ": next attempt in " + secs + " seconds");
					}
				}
			}

			sleeper.sleep(1000L);
		}
		
		stopEnded.countDown();
		LOGGER.info("Finished");
	}

	private long nowSeconds() {
		return sleeper.currentTimeMillis() / 1000;
	}
}
