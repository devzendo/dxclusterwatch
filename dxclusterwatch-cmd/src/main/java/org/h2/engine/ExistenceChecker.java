package org.h2.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A brutal hack - Database.exists is not public any more, but package-private.
 */
public class ExistenceChecker {
	private static Logger LOGGER = LoggerFactory.getLogger(ExistenceChecker.class);

	public static boolean exists(final String path) {
		LOGGER.debug("Checking for existence of " + path);
		return Database.exists(path);
	}
}
