package org.devzendo.dxclusterwatch.cmd;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config {
	private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);
	private Set<String> callsigns;

	public static Logger getLogger() {
		return LOGGER;
	}

	public Set<String> getCallsigns() {
		return callsigns;
	}

	public Config(final File prefsFile) {
		final Properties properties = loadProperties(prefsFile);
		
		callsigns = getCallsigns(properties.getProperty("callsigns"));
	}

	static Set<String> getCallsigns(final String prop) {
		if (prop == null) {
			return Collections.emptySet();
		}
		final String trim = prop.trim();
		if (trim.isEmpty()) {
			return Collections.emptySet();
		}
		final String[] split = StringUtils.defaultString(trim).split(",");
		final Set<String> set = new HashSet<>();
		for (String callsign : split) {
			set.add(callsign.trim().toUpperCase());
		}
		return set;
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
