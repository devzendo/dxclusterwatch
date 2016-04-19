package org.devzendo.dxclusterwatch.cmd;

public interface Persister {

	void persistRecords(ClusterRecord[] records);

	ClusterRecord getNextRecordToTweet();

	void markTweeted(ClusterRecord tweetedRecord);

}
