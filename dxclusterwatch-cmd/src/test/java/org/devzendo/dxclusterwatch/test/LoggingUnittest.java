package org.devzendo.dxclusterwatch.test;

import org.apache.log4j.BasicConfigurator;

public class LoggingUnittest {
	private static boolean initialised = false;

	public static synchronized void initialise() {
		if (!initialised) {
			initialised = true;
			BasicConfigurator.resetConfiguration();
			BasicConfigurator.configure();
		}
	}
}
