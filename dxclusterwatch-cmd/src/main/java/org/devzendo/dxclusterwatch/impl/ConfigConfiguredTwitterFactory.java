package org.devzendo.dxclusterwatch.impl;

import org.devzendo.dxclusterwatch.cmd.Config;
import org.devzendo.dxclusterwatch.cmd.ConfiguredTwitterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class ConfigConfiguredTwitterFactory implements ConfiguredTwitterFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigConfiguredTwitterFactory.class);
	private final Config config;

	// Twitter instance control - new one if config changes
	private String configStrings = "";
	private Twitter twitterInstance = null;

	public ConfigConfiguredTwitterFactory(final Config config) {
		this.config = config;
	}

	@Override
	public Twitter createTwitter() {
		final String consumerKey = config.getConsumerKey();
		final String consumerSecret = config.getConsumerSecret();
		final String accessToken = config.getAccessToken();
		final String accessSecret = config.getAccessSecret();

		final String newConfigStrings = consumerKey + consumerSecret + accessToken + accessSecret;
		if (!newConfigStrings.equals(configStrings) || twitterInstance == null) {
			LOGGER.info("Constructing a (re)configured Twitter instance");
			final ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
			configurationBuilder.setOAuthAccessToken(accessToken);
			configurationBuilder.setOAuthAccessTokenSecret(accessSecret);
			configurationBuilder.setOAuthConsumerKey(consumerKey);
			configurationBuilder.setOAuthConsumerSecret(consumerSecret);
			final Configuration configuration = configurationBuilder.build();
			final TwitterFactory twitterFactory = new TwitterFactory(configuration);
			twitterInstance = twitterFactory.getInstance();
			configStrings = newConfigStrings;
		}

		return twitterInstance;
	}
}
