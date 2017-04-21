package org.devzendo.dxclusterwatch.cmd;

import org.devzendo.commoncode.logging.Logging;
import org.devzendo.commoncode.prefs.PrefsFactory;
import org.devzendo.commoncode.time.Sleeper;
import org.devzendo.dxclusterwatch.impl.BitbucketPagesPageBuilder;
import org.devzendo.dxclusterwatch.impl.ConfigConfiguredTwitterFactory;
import org.devzendo.dxclusterwatch.impl.DXClusterSitePoller;
import org.devzendo.dxclusterwatch.impl.DefaultActivityWatcher;
import org.devzendo.dxclusterwatch.impl.H2Persister;
import org.devzendo.dxclusterwatch.impl.PropertiesConfig;
import org.devzendo.dxclusterwatch.impl.Twitter4JTweeter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
	private enum Mode {
		DO_IT, TEST_TWEET
	}
	private static final String TEST_TWEET_TEXT = "This is a test tweet, please ignore.";
	
	public static void main(final String[] args) {
		Logging.getInstance();
		
		Mode mode = Mode.DO_IT;
		for (int i = 0; i < args.length; i++) {
			final String arg = args[i];
			if (arg.equalsIgnoreCase("--testTweet")) {
				mode = Mode.TEST_TWEET;
				continue;
			}
		}

		try {
			final PrefsFactory prefsFactory = new PrefsFactory("dxclusterwatch", "dxclusterwatch.properties");
			if (!prefsFactory.prefsDirectoryExists()) {
				if (!prefsFactory.createPrefsDirectory()) {
					throw new IllegalStateException("Can't create preferences directory '" + prefsFactory.getPrefsDir() + "'");
				}
			}
			
			final Config config = new PropertiesConfig(prefsFactory.getPrefsFile());
			final ConfigConfiguredTwitterFactory configuredTwitterFactory = new ConfigConfiguredTwitterFactory(config);
			final Tweeter tweeter = new Twitter4JTweeter(configuredTwitterFactory);
			
			switch (mode) {
			case DO_IT:
				LOGGER.info("Starting DXClusterWatch...");
				final Persister persister = new H2Persister(prefsFactory.getPrefsDir(), config.getMaxListingEntries());
				try {
					final PageBuilder pageBuilder = new BitbucketPagesPageBuilder(config, persister);
					
					final DXClusterSitePoller sitePoller = new DXClusterSitePoller(prefsFactory.getPrefsDir(), config);

					final Sleeper sleeper = new Sleeper();
					final ActivityWatcher activityWatcher = new DefaultActivityWatcher(sleeper);
					new Controller(config, persister, pageBuilder, tweeter, sitePoller, sleeper, activityWatcher).start();
				}
				finally {
					persister.close();
				}
				break;
			case TEST_TWEET:
				LOGGER.info("Attempting to send test tweet...");
				tweeter.tweetText(TEST_TWEET_TEXT);
				break;
			default:
				break;			
			}
		} catch (final Exception e) {
			LOGGER.error(e.getMessage(), e);
			System.exit(1);
		}
	}
}
