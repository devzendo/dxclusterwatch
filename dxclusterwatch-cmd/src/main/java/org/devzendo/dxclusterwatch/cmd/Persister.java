package org.devzendo.dxclusterwatch.cmd;

import java.util.List;

public interface Persister {

	int persistRecords(ClusterRecord[] records);

	List<ClusterRecord> getRecords();

	ClusterRecord getNextRecordToTweet();

	void markTweeted(ClusterRecord tweetedRecord);

	void close();

}
