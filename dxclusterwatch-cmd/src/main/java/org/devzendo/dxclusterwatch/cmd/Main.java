package org.devzendo.dxclusterwatch.cmd;

import org.devzendo.commoncode.logging.Logging;
import org.devzendo.commoncode.prefs.PrefsFactory;
import org.devzendo.dxclusterwatch.impl.BitbucketPagesPageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
	public static void main(final String[] args) {
		Logging.getInstance();

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
			final Persister persister = new H2Persister(prefsFactory.getPrefsDir(), config.getMaxListingEntries());
			try {
				final PageBuilder pageBuilder = new BitbucketPagesPageBuilder(config, persister);
				
				final DXClusterSitePoller sitePoller = new DXClusterSitePoller(prefsFactory.getPrefsDir(), config);

				new DXClusterWatch(prefsFactory.getPrefsDir(), config, persister, pageBuilder, tweeter, sitePoller).start();
			}
			finally {
				persister.close();
			}
		} catch (final Exception e) {
			LOGGER.error(e.getMessage(), e);
			System.exit(1);
		}
	}
}
