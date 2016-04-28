package org.devzendo.dxclusterwatch.cmd;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesConfig implements Config {
	private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesConfig.class);
	private static final Pattern TRUE_PATTERN = Pattern.compile("^(true|yes)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern FALSE_PATTERN = Pattern.compile("^(false|no)$", Pattern.CASE_INSENSITIVE);

	private final File prefsFile;

	private long lastModificationTime = 0L;
	private Set<String> callsigns;
	private File siteRepoPath;
	private int pollMinutes;
	private int tweetSeconds;
	private File hgExecutablePath;
	private String consumerKey;
	private String consumerSecret;
	private String accessToken;
	private String accessSecret;
	private int maxListingEntries;
	private boolean enableFeedReading;
	private boolean enablePageUpdating;
	private boolean enableTweeting;
	private URI serverURI;

	public static Logger getLogger() {
		return LOGGER;
	}

	public PropertiesConfig(final File prefsFile) {
		this.prefsFile = prefsFile;
		readConfigurationFromPropertiesFile();
	}

	@Override
	public boolean hasChanged() {
		final long currentModificationTime = prefsFile.lastModified();
		return lastModificationTime != currentModificationTime;
	}

	@Override
	public String getConsumerKey() {
		readConfigurationFromPropertiesFile();
		return consumerKey;
	}

	@Override
	public String getConsumerSecret() {
		readConfigurationFromPropertiesFile();
		return consumerSecret;
	}

	@Override
	public String getAccessToken() {
		readConfigurationFromPropertiesFile();
		return accessToken;
	}

	@Override
	public String getAccessSecret() {
		readConfigurationFromPropertiesFile();
		return accessSecret;
	}

	@Override
	public File getHgExecutablePath() {
		readConfigurationFromPropertiesFile();
		return hgExecutablePath;
	}

	@Override
	public File getSiteRepoPath() {
		readConfigurationFromPropertiesFile();
		return siteRepoPath;
	}

	@Override
	public Set<String> getCallsigns() {
		readConfigurationFromPropertiesFile();
		return callsigns;
	}

	@Override
	public int getPollMinutes() {
		readConfigurationFromPropertiesFile();
		return pollMinutes;
	}

	@Override
	public int getTweetSeconds() {
		readConfigurationFromPropertiesFile();
		return tweetSeconds;
	}

	@Override
	public int getMaxListingEntries() {
		readConfigurationFromPropertiesFile();
		return maxListingEntries;
	}

	@Override
	public boolean isFeedReadingEnabled() {
		readConfigurationFromPropertiesFile();
		return enableFeedReading;
	}

	@Override
	public boolean isPageUpdatingEnabled() {
		readConfigurationFromPropertiesFile();
		return enablePageUpdating;
	}

	@Override
	public boolean isTweetingEnable() {
		readConfigurationFromPropertiesFile();
		return enableTweeting;
	}

	@Override
	public URI getServerURI() {
		readConfigurationFromPropertiesFile();
		return serverURI;
	}

	private void readConfigurationFromPropertiesFile() {
		final long currentModificationTime = prefsFile.lastModified();
		if (lastModificationTime == currentModificationTime) {
			return;
		}
		LOGGER.info("Reading configuration file " + prefsFile.getAbsolutePath());
		lastModificationTime = currentModificationTime;
		final Properties properties = loadProperties(prefsFile);
		
		callsigns = getCallsigns(properties.getProperty("callsigns"));		
		siteRepoPath = mustBePath("siteRepoPath", properties.getProperty("siteRepoPath"));
		pollMinutes = mustBeInteger("pollMinutes", properties.getProperty("pollMinutes"));
		tweetSeconds = mustBeInteger("tweetSeconds", properties.getProperty("tweetSeconds"));
		hgExecutablePath = mustBeExecutablePath("hgExecutablePath", properties.getProperty("hgExecutablePath"));
		consumerKey = mustBeString("consumerKey", properties.getProperty("consumerKey"));
		consumerSecret = mustBeString("consumerSecret", properties.getProperty("consumerSecret"));
		accessToken = mustBeString("accessToken", properties.getProperty("accessToken"));
		accessSecret = mustBeString("accessSecret", properties.getProperty("accessSecret"));
		maxListingEntries = mustBeInteger("maxListingEntries", properties.getProperty("maxListingEntries"));
		enableFeedReading = mustBeBoolean("enableFeedReading", properties.getProperty("enableFeedReading"));
		enablePageUpdating = mustBeBoolean("enablePageUpdating", properties.getProperty("enablePageUpdating"));
		enableTweeting = mustBeBoolean("enableTweeting", properties.getProperty("enableTweeting"));
		serverURI = mustBeURI("serverURI", properties.getProperty("serverURI"));
	}

	static String mustBeString(final String propertyName, final String value) {
		LOGGER.debug("Checking property {} string {}", propertyName, value);
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException("Empty property '" + propertyName + "'");
		}
		return value.trim();
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

	static boolean mustBeBoolean(final String propertyName, final String boolText) {
		LOGGER.debug("Checking property {} boolean {}", propertyName, boolText);
		if (boolText == null || boolText.trim().isEmpty()) {
			throw new IllegalArgumentException("Empty property '" + propertyName + "'");
		}
		if (TRUE_PATTERN.matcher(boolText).matches()) {
			return true;
		}
		if (FALSE_PATTERN.matcher(boolText).matches()) {
			return false;
		}
		throw new IllegalArgumentException(boolText + " is not a boolean");
	}

	static URI mustBeURI(final String propertyName, final String uriText) {
		LOGGER.debug("Checking property {} URI {}", propertyName, uriText);
		if (uriText == null || uriText.trim().isEmpty()) {
			throw new IllegalArgumentException("Empty property '" + propertyName + "'");
		}
		return URI.create(uriText.trim());
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
