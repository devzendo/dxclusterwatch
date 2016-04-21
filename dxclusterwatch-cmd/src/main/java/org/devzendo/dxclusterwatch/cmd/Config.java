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
	private final Set<String> callsigns;
	private final File siteRepoPath;
	private final int pollMinutes;
	private final int tweetSeconds;
	private final File hgExecutablePath;

	public File getHgExecutablePath() {
		return hgExecutablePath;
	}

	public static Logger getLogger() {
		return LOGGER;
	}

	public File getSiteRepoPath() {
		return siteRepoPath;
	}

	public Set<String> getCallsigns() {
		return callsigns;
	}

	public int getPollMinutes() {
		return pollMinutes;
	}

	public int getTweetSeconds() {
		return tweetSeconds;
	}

	public Config(final File prefsFile) {
		final Properties properties = loadProperties(prefsFile);
		
		callsigns = getCallsigns(properties.getProperty("callsigns"));		
		siteRepoPath = mustBePath("siteRepoPath", properties.getProperty("siteRepoPath"));
		pollMinutes = mustBeInteger("pollMinutes", properties.getProperty("pollMinutes"));
		tweetSeconds = mustBeInteger("tweetSeconds", properties.getProperty("tweetSeconds"));
		hgExecutablePath = mustBeExecutablePath("hgExecutablePath", properties.getProperty("hgExecutablePath"));
	}

	static File mustBePath(final String propertyName, final String path) {
		LOGGER.debug("Checking property {} path {}", propertyName, path);
		if (path == null || path.trim().isEmpty()) {
			throw new IllegalArgumentException("Empty property '" + propertyName + "'");
		}
		final File pathFile = new File(path);
		if (!pathFile.exists()) {
			throw new IllegalArgumentException(path + " directory does not exist");
		}
		if (!pathFile.isDirectory()) {
			throw new IllegalArgumentException(path + " is a file but should be a directory");
		}
		return pathFile;
	}

	static File mustBeExecutablePath(final String propertyName, final String path) {
		LOGGER.debug("Checking property {} executable path {}", propertyName, path);
		if (path == null || path.trim().isEmpty()) {
			throw new IllegalArgumentException("Empty property '" + propertyName + "'");
		}
		final File exeFile = new File(path);
		if (!exeFile.exists()) {
			throw new IllegalArgumentException(path + " executable does not exist");
		}
		if (!exeFile.canExecute()) {
			throw new IllegalArgumentException(path + " is not executable");
		}
		return exeFile;
	}

	static int mustBeInteger(final String propertyName, final String intText) {
		LOGGER.debug("Checking property {} integer {}", propertyName, intText);
		if (intText == null || intText.trim().isEmpty()) {
			throw new IllegalArgumentException("Empty property '" + propertyName + "'");
		}
		try {
			return Integer.parseInt(intText);
		} catch (final NumberFormatException nfe) {
			throw new IllegalArgumentException(intText + " is not an integer");
		}
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
		for (final String callsign : split) {
			set.add(callsign.trim().toUpperCase());
		}
		return set;
 	}
	
	private Properties loadProperties(final File prefsFile) {
		final Properties props = new Properties();
		try {
			props.load(new FileInputStream(prefsFile));
		} catch (final IOException e) {
			final String msg = "Can't load DXClusterWatch config file " + prefsFile.getAbsolutePath() + ": " + e.getMessage();
			LOGGER.error(msg);
			throw new RuntimeException(msg);
		}
		return props;
	}
}
