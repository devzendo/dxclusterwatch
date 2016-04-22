package org.devzendo.dxclusterwatch.cmd;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class Twitter4JTweeter implements Tweeter {
	private static final Logger LOGGER = LoggerFactory.getLogger(Twitter4JTweeter.class);
	private final String consumerKey;
	private final String consumerSecret;
	private final String accessToken;
	private final String accessSecret;
	private final Twitter twitter;

	
	public Twitter4JTweeter(final Config config) {
		consumerKey = config.getConsumerKey();
		consumerSecret = config.getConsumerSecret();
		accessToken = config.getAccessToken();
		accessSecret = config.getAccessSecret();
		
		final ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
		configurationBuilder.setOAuthAccessToken(accessToken);
		configurationBuilder.setOAuthAccessTokenSecret(accessSecret);
		configurationBuilder.setOAuthConsumerKey(consumerKey);
		configurationBuilder.setOAuthConsumerSecret(consumerSecret);
		final Configuration configuration = configurationBuilder.build();
		final TwitterFactory twitterFactory = new TwitterFactory(configuration);
		twitter = twitterFactory.getInstance();
	}
	
	@Override
	public void tweet(final ClusterRecord recordToTweet) {
		final String post = convertToTweet(recordToTweet);
		LOGGER.debug("Tweeting '{}'", post);
		try {
			twitter.updateStatus(post);
		} catch (final TwitterException e) {
			LOGGER.warn("Could not tweet '{}': {}", post, e);
			throw new RuntimeException(e);
		}
	}
	
	public static final String convertToTweet(final ClusterRecord record) {
		final StringBuilder sb = new StringBuilder();
		sb.append(record.getDxcall());
		sb.append(" heard on ");
		sb.append(record.getFreq());
		sb.append(" by ");
		sb.append(record.getCall());
		sb.append(" at ");
		sb.append(toTime(record.getTime()));
		final String comment = StringUtils.defaultString(record.getComment()).trim();
		if (!comment.isEmpty()) {
			final int len = sb.toString().length();
			final int remaining = 140 - len - 3;
			if (remaining > 0) {
				sb.append(" \"");
				sb.append(comment.substring(0, Math.min(remaining, comment.length())));
				sb.append("\"");
			}
		}
		return sb.toString();
	}

	public static String toTime(final String time) {
		final Timestamp ts = Timestamp.valueOf(time);
		final Calendar cal = Calendar.getInstance();
		cal.setTime(ts);
		return String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
	}

}
