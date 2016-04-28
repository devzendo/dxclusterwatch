package org.devzendo.dxclusterwatch.cmd;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigUnittest {

	private static final String LINE_SEP = System.getProperty("line.separator");

	public static File createSampleConfig(final File dir, final boolean feedReadingEnabled) throws IOException {
		final File properties = new File(dir, "test.properties");
		final File siteRepo = new File(dir, "site.repo");
		siteRepo.mkdir();
		final FileWriter fileWriter = new FileWriter(properties);
		try {
			fileWriter.write("callsigns=m0cuv,2e0sql" + LINE_SEP);
			fileWriter.write("pollMinutes=1" + LINE_SEP);
			fileWriter.write("tweetSeconds=30" + LINE_SEP);
			fileWriter.write("siteRepoPath=" + siteRepo.getAbsolutePath() + LINE_SEP);
			fileWriter.write("hgExecutablePath=/opt/local/bin/hg" + LINE_SEP);
			fileWriter.write("consumerKey=abc" + LINE_SEP);
			fileWriter.write("consumerSecret=def" + LINE_SEP);
			fileWriter.write("accessToken=123abc" + LINE_SEP);
			fileWriter.write("accessSecret=def987" + LINE_SEP);
			fileWriter.write("maxListingEntries=20" + LINE_SEP);
			fileWriter.write("enableFeedReading=" + feedReadingEnabled + LINE_SEP);
			fileWriter.write("enablePageUpdating=yes" + LINE_SEP);
			fileWriter.write("enableTweeting=No" + LINE_SEP);
			fileWriter.write("serverURI=http://localhost:5645" + LINE_SEP);
		} finally {
			fileWriter.close();
		}
		return properties;
	}

}
