package org.devzendo.dxclusterwatch.cmd;

public interface Persister {

	boolean persistRecords(ClusterRecord[] records);

	ClusterRecord getNextRecordToTweet();

	void markTweeted(ClusterRecord tweetedRecord);

	void close();
}
