package org.devzendo.dxclusterwatch.cmd;

public interface Tweeter {
	void tweet(ClusterRecord recordToTweet);

	void tweetText(String activity);
}
