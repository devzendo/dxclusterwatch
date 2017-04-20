package org.devzendo.dxclusterwatch.cmd;

public interface ActivityWatcher {
	void seen(ClusterRecord record);

	// empty string if nothing to say, repeat last string if nothing has changed. 
	String latestTweetableActivity();
	
	// number of currently stored entries, for purging tests
	int numEntries();
	int numCallsigns();
	
	// called by seen(), latestTweetableActivity() but also from purging tests.
	void purge();

}
