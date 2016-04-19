package org.devzendo.dxclusterwatch.cmd;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config {
	private static Logger LOGGER = LoggerFactory.getLogger(Config.class);
	private final Properties properties;

	public Config(final File prefsFile) {
		properties = loadProperties(prefsFile);
	}

	private Properties loadProperties(final File prefsFile) {
		final Properties props = new Properties();
		try {
			props.load(new FileInputStream(prefsFile));
		} catch (IOException e) {
			final String msg = "Can't load DXClusterWatch config file " + prefsFile.getAbsolutePath() + ": " + e.getMessage();
			LOGGER.error(msg);
			throw new RuntimeException(msg);
		}
		return props;
	}

}
