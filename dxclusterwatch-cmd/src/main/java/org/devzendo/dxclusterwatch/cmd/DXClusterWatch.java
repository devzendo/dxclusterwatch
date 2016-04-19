package org.devzendo.dxclusterwatch.cmd;

import java.io.File;

import org.devzendo.commoncode.concurrency.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DXClusterWatch {
	private static final long DXCLUSTER_POLL_INTERVAL_SECONDS = 60 * 5; // 5 mins
	
	private static Logger LOGGER = LoggerFactory.getLogger(DXClusterWatch.class);
	private final SitePoller sitePoller;
	
	private boolean running = true;
	
	public DXClusterWatch(File prefsDir, Config config) {
		sitePoller = new DXClusterSitePoller(prefsDir, "https://www.dxcluster.co.uk/index.php?/api/all");
		
	}
	
	public void start() {
		long backoffCount = 0;
		long nextPollTime = nowSeconds(); // force the first poll.
		while (running) {
			if (nowSeconds() >= nextPollTime) {
				try {
					final ClusterRecord[] records = sitePoller.poll();
					backoffCount = 0;
					nextPollTime = nowSeconds() + DXCLUSTER_POLL_INTERVAL_SECONDS;
					LOGGER.info("Next poll in " + DXCLUSTER_POLL_INTERVAL_SECONDS + " secs");
				} catch (RuntimeException re) {
					backoffCount ++;
					final long secs = 60 * backoffCount;
					nextPollTime = nowSeconds() + secs;
					LOGGER.warn("Could not poll cluster: " + re.getMessage() + ": next attempt in " + secs + " seconds");
				}
			}

			ThreadUtils.waitNoInterruption(1000L);
		}
	}

	private long nowSeconds() {
		return System.currentTimeMillis() / 1000;
	}

}
