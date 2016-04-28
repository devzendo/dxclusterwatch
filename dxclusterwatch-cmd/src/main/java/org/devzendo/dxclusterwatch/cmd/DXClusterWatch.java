package org.devzendo.dxclusterwatch.cmd;

import java.io.File;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.devzendo.commoncode.concurrency.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DXClusterWatch {
	private static final Logger LOGGER = LoggerFactory.getLogger(DXClusterWatch.class);
	
	private final Persister persister;
	private final PageBuilder pageBuilder;
	private final Tweeter tweeter;
	private final SitePoller sitePoller;
	
	private final AtomicBoolean running = new AtomicBoolean(true);

	private final Config config;


	public DXClusterWatch(final File prefsDir, final Config config, final Persister persister, final PageBuilder pageBuilder, final Tweeter tweeter) {
		this.config = config;
		this.persister = persister;
		this.pageBuilder = pageBuilder;
		this.tweeter = tweeter;
		final Set<String> callsigns = config.getCallsigns();
		if (callsigns.isEmpty()) {
			throw new IllegalStateException("No callsigns configured");
		}
		sitePoller = new DXClusterSitePoller(prefsDir, config);
		Signals.withHandler(new Runnable() {
			@Override
			public void run() {
	            LOGGER.info("Interrupt received, stopping");
	            running.set(false);
			}}, Signals.SignalName.INT);
	}
	
	public void start() {
		long backoffCount = 0;
		long tweetBackoffCount = 0;
		long nextPollTime = nowSeconds(); // force the first poll.
		long nextTweet = nowSeconds(); // force the first tweet to happen now
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
