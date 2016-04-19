package org.devzendo.dxclusterwatch.cmd;

import java.io.File;

import org.devzendo.commoncode.concurrency.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DXClusterWatch {
	private static final long DXCLUSTER_POLL_INTERVAL_SECONDS = 60 * 5; // 5 mins
	private static final Logger LOGGER = LoggerFactory.getLogger(DXClusterWatch.class);
	
	private final Config config;
	private final Persister persister;
	private final PageBuilder pageBuilder;
	private final Tweeter tweeter;
	private final SitePoller sitePoller;
	
	private boolean running = true;


	
	public DXClusterWatch(final File prefsDir, final Config config, final Persister persister, final PageBuilder pageBuilder, final Tweeter tweeter) {
		this.config = config;
		this.persister = persister;
		this.pageBuilder = pageBuilder;
		this.tweeter = tweeter;
		sitePoller = new DXClusterSitePoller(prefsDir, "https://www.dxcluster.co.uk/index.php?/api/all");
	}
	
	public void start() {
		long backoffCount = 0;
		long nextPollTime = nowSeconds(); // force the first poll.
		long nextTweet = nowSeconds(); // force the first tweet to happen now
		int tweetNumber = 1;
		int pageRebuildNumber = 1;
		LOGGER.info("Starting....");
		while (running) {
			if (nowSeconds() >= nextPollTime) {
				try {
					final ClusterRecord[] records = sitePoller.poll();
					if (records.length > 0) {
						backoffCount = 0;
						nextPollTime = nowSeconds() + DXCLUSTER_POLL_INTERVAL_SECONDS;
						LOGGER.info("Next poll in " + DXCLUSTER_POLL_INTERVAL_SECONDS + " secs");

						LOGGER.debug("Persisting " + records.length + " records");
						persister.persistRecords(records);
						LOGGER.info("Rebuilding page #" + pageRebuildNumber);
						pageRebuildNumber++;
						pageBuilder.rebuildPage();
					}					
				} catch (final RuntimeException re) {
					backoffCount ++;
					final long secs = 60 * backoffCount;
					nextPollTime = nowSeconds() + secs;
					LOGGER.warn("Could not poll cluster: " + re.getMessage() + ": next attempt in " + secs + " seconds");
				}
			}
			
			if (nowSeconds() >= nextTweet) {
				final ClusterRecord nextRecordToTweet = persister.getNextRecordToTweet();
				if (nextRecordToTweet != null) {
					try {
						LOGGER.info("#" + tweetNumber + " - tweeting " + nextRecordToTweet);
						tweeter.tweet(nextRecordToTweet);
						tweetNumber++;
						persister.markTweeted(nextRecordToTweet);
						nextTweet = nowSeconds() + 10L;
					} catch (final RuntimeException re) {
						LOGGER.warn("Could not tweet " + nextRecordToTweet + ": " + re.getMessage());
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
