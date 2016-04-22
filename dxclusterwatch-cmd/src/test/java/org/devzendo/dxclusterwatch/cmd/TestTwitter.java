package org.devzendo.dxclusterwatch.cmd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.*;

import java.sql.Timestamp;

import org.apache.log4j.BasicConfigurator;
import org.devzendo.commoncode.prefs.PrefsFactory;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestTwitter {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestTwitter.class);

	private static Tweeter tweeter;
	private static final ClusterRecord dbRecord = ClusterRecord.dbRecord(1, "GB4IMD", "M0CUV", new Timestamp(System.currentTimeMillis()), "14060", "This tweet was posted by an integration test.");

	@BeforeClass
	public static void setupLogging() {
		BasicConfigurator.resetConfiguration();
		BasicConfigurator.configure();
	}

	@BeforeClass
	public static void setupTwitter() {
		final PrefsFactory prefsFactory = new PrefsFactory("dxclusterwatch", "dxclusterwatch.properties");
		if (!prefsFactory.prefsDirectoryExists()) {
			if (!prefsFactory.createPrefsDirectory()) {
				throw new IllegalStateException("Can't create preferences directory '" + prefsFactory.getPrefsDir() + "'");
			}
		}
		
		final Config config = new Config(prefsFactory.getPrefsFile());
		tweeter = new Twitter4JTweeter(config);
	}
	
	@Test
	@Ignore
	public void testTweet() {
		tweeter.tweet(dbRecord);
	}
	
	@Test
	public void testToTime() {
		assertThat(Twitter4JTweeter.toTime("2016-04-22 05:49:00.0"), equalTo("05:49"));
	}
	
	@Test
	public void postMessageLooksRight() throws Exception {
		final String post = Twitter4JTweeter.convertToTweet(dbRecord);
		LOGGER.info("Post [{}]", post);
		MatcherAssert.assertThat(post.length(), lessThanOrEqualTo(140));
	}
}
