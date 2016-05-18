package org.devzendo.dxclusterwatch.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.devzendo.dxclusterwatch.cmd.ClusterRecord;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

public class TestJsonListSerialisation {
	final ClusterRecord dbRecord1 = ClusterRecord.dbRecord(1, "GB4IMD", "M0CUV", ago(5), "14060", "Hi Matt");
	final ClusterRecord dbRecord2 = ClusterRecord.dbRecord(2, "GB3IMD", "M0CUV", ago(25), "7035", "UP 20");
	final ClusterRecord dbRecord3 = ClusterRecord.dbRecord(3, "GB3MRS", "G0VAR", ago(35), "10118", "VY 73 OM");
	final ClusterRecord dbRecord4 = ClusterRecord.dbRecord(4, "IY0GM", "F6IIS", ago(40), "3580", "Ciao Guglielmo");

	final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	public void serialiseList() throws Exception {
		final String json = serialiseRecords();
		System.out.println(json);
		final ClusterRecord[] records = deserialiseRecords(json);
		for (final ClusterRecord clusterRecord : records) {
			System.out.println(clusterRecord);
		}
		assertThat(records.length, equalTo(4));
		assertThat(records[0].getFreq(), equalTo("14060"));
		assertThat(records[1].getFreq(), equalTo("7035"));
		assertThat(records[2].getFreq(), equalTo("10118"));
		assertThat(records[3].getFreq(), equalTo("3580"));
	}

	private ClusterRecord[] deserialiseRecords(final String json) throws JsonParseException, JsonMappingException, IOException {
		return objectMapper.readValue(json, ClusterRecord[].class);
	}

	private String serialiseRecords() throws IOException, JsonGenerationException, JsonMappingException {
		final List<ClusterRecord> clusterRecordList = Arrays.asList(dbRecord1, dbRecord2, dbRecord3, dbRecord4);
		return objectMapper.writeValueAsString(clusterRecordList);
	}
	
	private Timestamp ago(final long minutesAgo) {
		final long millisecondsAgo = System.currentTimeMillis() - (minutesAgo * 60000);
		return new Timestamp(millisecondsAgo);
	}
}
