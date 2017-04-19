package org.devzendo.dxclusterwatch.cmd;

public interface ActivityWatcher {
	void seen(ClusterRecord record);

	// empty string if nothing to say, repeat last string if nothing has changed. 
	String latestTweetableActivity();
}
