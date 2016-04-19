package org.devzendo.dxclusterwatch.cmd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DXClusterWatch {
	private static Logger LOGGER = LoggerFactory.getLogger(DXClusterWatch.class);
	
	public DXClusterWatch(File prefsDir, File prefsFile) {
		Properties props = new Properties();
		try {
			props.load(new FileInputStream(prefsFile));
		} catch (IOException e) {
			final String msg = "Can't load DXClusterWatch config file " + prefsFile.getAbsolutePath() + ": " + e.getMessage();
			LOGGER.error(msg);
			throw new RuntimeException(msg);
		}
		
		SitePoller sitePoller = new SitePoller(prefsDir, "https://www.dxcluster.co.uk/index.php?/api/all");
		final ClusterRecord[] records = sitePoller.poll();
	}
}
