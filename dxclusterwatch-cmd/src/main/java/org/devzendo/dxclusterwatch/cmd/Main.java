package org.devzendo.dxclusterwatch.cmd;

import java.util.Arrays;
import java.util.List;

import org.devzendo.commoncode.logging.Logging;
import org.devzendo.commoncode.prefs.PrefsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
	public static void main(final String[] args) {
		final Logging logging = Logging.getInstance();
		final List<String> finalArgList = logging.setupLoggingFromArgs(Arrays.asList(args));

		try {
			final PrefsFactory prefsFactory = new PrefsFactory("dxclusterwatch", "dxclusterwatch.properties");
			if (!prefsFactory.prefsDirectoryExists()) {
				if (!prefsFactory.createPrefsDirectory()) {
					throw new IllegalStateException("Can't create preferences directory '" + prefsFactory.getPrefsDir() + "'");
				}
			}
			
			final Config config = new Config(prefsFactory.getPrefsFile());
			new DXClusterWatch(prefsFactory.getPrefsDir(), config);

		} catch (final Exception e) {
			LOGGER.error(e.getMessage(), e);
			System.exit(1);
		}
	}
}
