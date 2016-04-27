package org.devzendo.dxclusterwatch.cmd;

import java.io.File;
import java.util.Set;

public interface Config {

	String getConsumerKey();

	String getConsumerSecret();

	String getAccessToken();

	String getAccessSecret();

	File getHgExecutablePath();

	File getSiteRepoPath();

	Set<String> getCallsigns();

	int getPollMinutes();

	int getTweetSeconds();

	int getMaxListingEntries();

	boolean isFeedReadingEnabled();

	boolean isPageUpdatingEnabled();

	boolean isTweetingEnable();

}