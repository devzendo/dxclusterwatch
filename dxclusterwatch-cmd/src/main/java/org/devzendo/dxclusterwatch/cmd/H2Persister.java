package org.devzendo.dxclusterwatch.cmd;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H2Persister implements Persister {

	private static final Logger LOGGER = LoggerFactory.getLogger(H2Persister.class);

	public H2Persister(final File storeDir) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean persistRecords(ClusterRecord[] records) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ClusterRecord getNextRecordToTweet() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void markTweeted(ClusterRecord tweetedRecord) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}
}
