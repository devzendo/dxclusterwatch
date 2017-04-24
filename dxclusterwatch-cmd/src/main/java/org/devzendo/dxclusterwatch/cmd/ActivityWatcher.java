package org.devzendo.dxclusterwatch.cmd;

public interface ActivityWatcher {
	public interface MarkPublished {
		void markPublished(ClusterRecord record);
	}
	
	// record a cluster record. return true if it's a new one (and will be formed into a tweet and
	// marked as published when it fits), false if already seen (and will be marked as published
	// immediately)
	boolean seen(ClusterRecord record, MarkPublished markPublished);

	// empty string if nothing to say, repeat last string if nothing has changed. 
	String latestTweetableActivity();
	
	// number of currently stored entries, for purging tests
	int numEntries();
	int numCallsigns();
	
	// called by seen(), latestTweetableActivity() but also from purging tests.
	void purge();

}
