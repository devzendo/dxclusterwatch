package org.devzendo.dxclusterwatch.cmd;

import java.sql.Timestamp;
import java.util.List;

public interface Persister {
	int persistRecords(ClusterRecord[] records);

	List<ClusterRecord> getRecords();

	ClusterRecord getNextRecordToTweet();
	List<ClusterRecord> getUntweetedRecords();

	void markTweeted(ClusterRecord tweetedRecord);

	void close();

	List<ClusterRecord> getRecordsBetween(Timestamp start, Timestamp end);
	
	Timestamp getEarliestTimeRecord();

}
