package org.devzendo.dxclusterwatch.cmd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import java.sql.Timestamp;

import org.apache.log4j.BasicConfigurator;
import org.devzendo.commoncode.prefs.PrefsFactory;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class TestTwitter {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestTwitter.class);

	private static Tweeter tweeter;
	private static long time = 1461324273856L; // 12:24 on 22/04/2016

	private static final ClusterRecord dbRecord = ClusterRecord.dbRecord(1, "GB4IMD", "M0CUV", new Timestamp(time), "14060", "This tweet was posted by an integration test.");

	@Mock
	private static ConfiguredTwitterFactory configuredTwitterFactory;

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

		// TODO mock up configuredTwitterFactory behaviour
		tweeter = new Twitter4JTweeter(configuredTwitterFactory);
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
	public void postMessageWithNormalComment() throws Exception {
		final String post = tweetPost(dbRecord);
		assertThat(post, equalTo("GB4IMD heard on 14060 by M0CUV at 12:24 \"This tweet was posted by an integration test.\""));
	}
	
	@Test
	public void postNoComment() throws Exception {
		final String post = tweetPost(ClusterRecord.dbRecord(1, "GB4IMD", "M0CUV", new Timestamp(time), "14060", ""));
		assertThat(post, equalTo("GB4IMD heard on 14060 by M0CUV at 12:24"));
	}


	@Test
	public void justFitsInWithoutComment() throws Exception {
		final String post = tweetPost(ClusterRecord.dbRecord(1,
				"GB4IMD-a-ridiculously-long-name-that-almost-pushes-the-post-length-over-the-140-character-limit-12345678901",
				"M0CUV", new Timestamp(time), "14060", ""));
		//                        12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
		assertThat(post, equalTo("GB4IMD-a-ridiculously-long-name-that-almost-pushes-the-post-length-over-the-140-character-limit-12345678901 heard on 14060 by M0CUV at 12:24"));
	}

	@Test
	public void justStartsToOverflowWithoutComment() throws Exception {
		final String post = tweetPost(ClusterRecord.dbRecord(1,
				"GB4IMD-a-ridiculously-long-name-that-almost-pushes-the-post-length-over-the-140-character-limit-123456789012",
				"M0CUV", new Timestamp(time), "14060", ""));
		//                        12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
		assertThat(post, equalTo("GB4IMD-a-ridiculously-long-name-that-almost-pushes-the-post-length-over-the-140-character-limit-123456789012 heard on 14060 by M0CUV at 12:2"));
	}

	@Test
	public void overflowWithoutComment() throws Exception {
		final String post = tweetPost(ClusterRecord.dbRecord(1,
				"GB4IMD-a-ridiculously-long-name-that-would-push-the-post-length-over-the-140-character-limit-but-gets-truncated",
				"M0CUV", new Timestamp(time), "14060", ""));
		//                        12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
		assertThat(post, equalTo("GB4IMD-a-ridiculously-long-name-that-would-push-the-post-length-over-the-140-character-limit-but-gets-truncated heard on 14060 by M0CUV at 1"));
	}

	@Test
	public void justFitsInWithComment() throws Exception {
		final String post = tweetPost(ClusterRecord.dbRecord(1,
				"GB4IMD-a-ridiculously-long-name-that-almost-pushes-the-post-length-over-the-140-character-limit-123456",
				"M0CUV", new Timestamp(time), "14060", "x"));
		//                        12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
		assertThat(post, equalTo("GB4IMD-a-ridiculously-long-name-that-almost-pushes-the-post-length-over-the-140-character-limit-123456 heard on 14060 by M0CUV at 12:24 \"x\""));
	}

	@Test
	public void overflowWithComment() throws Exception {
		final String post = tweetPost(ClusterRecord.dbRecord(1,
				"GB4IMD-a-ridiculously-long-name-that-almost-pushes-the-post-length-over-the-140-character-limit-1234567",
				"M0CUV", new Timestamp(time), "14060", "x"));
		//                        12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
		assertThat(post, equalTo("GB4IMD-a-ridiculously-long-name-that-almost-pushes-the-post-length-over-the-140-character-limit-1234567 heard on 14060 by M0CUV at 12:24"));
	}

	@Test
	public void justFitsInWithLongerComment() throws Exception {
		final String post = tweetPost(ClusterRecord.dbRecord(1,
				"GB4IMD-a-ridiculously-long-name-that-almost-pushes-the-post-length-over-the-140-character-limit",
				"M0CUV", new Timestamp(time), "14060", "x1234567"));
		//                        12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
		assertThat(post, equalTo("GB4IMD-a-ridiculously-long-name-that-almost-pushes-the-post-length-over-the-140-character-limit heard on 14060 by M0CUV at 12:24 \"x1234567\""));
	}

	@Test
	public void truncatedComment() throws Exception {
		final String post = tweetPost(ClusterRecord.dbRecord(1,
				"GB4IMD-a-ridiculously-long-name-that-almost-pushes-the-post-length-over-the-140-character-limit",
				"M0CUV", new Timestamp(time), "14060", "x1234567890"));
		//                        12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
		assertThat(post, equalTo("GB4IMD-a-ridiculously-long-name-that-almost-pushes-the-post-length-over-the-140-character-limit heard on 14060 by M0CUV at 12:24 \"x1234567\""));
	}

	private String tweetPost(final ClusterRecord record) {
		final String post = Twitter4JTweeter.convertToTweet(record);
		LOGGER.info("Post [{}]", post);
		assertThat(post.length(), lessThanOrEqualTo(140));
		return post;
	}

}
