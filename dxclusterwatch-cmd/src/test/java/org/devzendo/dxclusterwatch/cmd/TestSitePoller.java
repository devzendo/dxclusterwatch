package org.devzendo.dxclusterwatch.cmd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.devzendo.commoncode.resource.ResourceLoader;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TestSitePoller {
	private final String mainServerUrl = "https://www.dxcluster.co.uk/index.php?/api/all";
	//private final String localServerUrl = "http://localhost:8000/original_dxcluster.html";

	@BeforeClass
	public static void setupLogging() {
		LoggingUnittest.initialise();
	}

	@Test
	@Ignore
	public void test() {
		final HashSet<String> callsigns = new HashSet<String>();
		callsigns.add("UA5D");
		
		final DXClusterSitePoller sp = new DXClusterSitePoller(new File("src/test/resources"), mainServerUrl, callsigns);
		final ClusterRecord[] records = sp.poll();
		System.out.println("Read " + records.length + " records");
		for (ClusterRecord clusterRecord : records) {
			System.out.println(clusterRecord);
		}
		assertThat(records.length, equalTo(1));
	}

	@Test
	public void loadingOfFileWorks() throws JsonParseException, JsonMappingException, IOException {		
		ClusterRecord[] records = getRecordsFromSampleFile();
		
		assertThat(records.length, equalTo(35));
	}

	@Test
	public void filterNoCallsigns() throws JsonParseException, JsonMappingException, IOException {		
		final ClusterRecord[] records = getRecordsFromSampleFile();

		final ClusterRecord[] filtered = DXClusterSitePoller.filterCallsigns(Collections.emptySet(), records);
		assertThat(filtered.length, equalTo(0));
	}

	@Test
	public void filterCallsigns() throws JsonParseException, JsonMappingException, IOException {		
		final ClusterRecord[] records = getRecordsFromSampleFile();
		final HashSet<String> callsigns = new HashSet<String>();
		callsigns.add("UA5D");

		ClusterRecord[] filtered = DXClusterSitePoller.filterCallsigns(callsigns, records);
		
		System.out.println("Read " + filtered.length + " records");
		for (ClusterRecord clusterRecord : filtered) {
			System.out.println(clusterRecord);
		}
		assertThat(filtered.length, equalTo(1));
	}

	private ClusterRecord[] getRecordsFromSampleFile() throws IOException, JsonParseException, JsonMappingException {
		return new ObjectMapper().readValue(ResourceLoader.getResourceInputStream("pretty_printed_dxcluster.json"), ClusterRecord[].class);
	}

}

