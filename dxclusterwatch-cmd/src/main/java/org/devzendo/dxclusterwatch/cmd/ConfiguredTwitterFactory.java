package org.devzendo.dxclusterwatch.cmd;

import twitter4j.Twitter;

public interface ConfiguredTwitterFactory {

	Twitter createTwitter();

}