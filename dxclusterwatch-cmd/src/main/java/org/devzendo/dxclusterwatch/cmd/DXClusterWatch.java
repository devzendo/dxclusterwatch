package org.devzendo.dxclusterwatch.cmd;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DXClusterWatch {
	private static Logger LOGGER = LoggerFactory.getLogger(DXClusterWatch.class);
	private final SitePoller sitePoller;
	
	public DXClusterWatch(File prefsDir, Config config) {
		sitePoller = new DXClusterSitePoller(prefsDir, "https://www.dxcluster.co.uk/index.php?/api/all");
		
	}
	
	public void start() {
		
		final ClusterRecord[] records = sitePoller.poll();
	}

}
