package org.devzendo.dxclusterwatch.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;

import org.apache.log4j.BasicConfigurator;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.devzendo.commoncode.resource.ResourceLoader;
import org.devzendo.dxclusterwatch.cmd.ClusterRecord;
import org.devzendo.dxclusterwatch.cmd.Config;
import org.devzendo.dxclusterwatch.cmd.FakeDXCluster;
import org.devzendo.dxclusterwatch.impl.DXClusterSitePoller;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class TestDXClusterSitePoller {
	private FakeDXCluster fakeDXCluster;

	private static Logger LOGGER = LoggerFactory.getLogger(TestDXClusterSitePoller.class);

	@BeforeClass
	public static void setupLogging() {
		BasicConfigurator.resetConfiguration();
		BasicConfigurator.configure();
	}

    @Before
    public void getPreRequisites() throws IOException {
        fakeDXCluster = FakeDXCluster.createServer(8000);
    }
    
    @After
    public void shutdownWebServer() {
        if (fakeDXCluster != null) {
        	fakeDXCluster.stop();
        }
    }

	@Mock
	private Config config;
	
	@Test
	public void pollFakeServerGetsRecords() {
		final HashSet<String> callsigns = new HashSet<String>();
		callsigns.add("UA5D");
		
		final URI testServerURL = fakeDXCluster.getURI();
		
		when(config.getCallsigns()).thenReturn(callsigns);
		when(config.getServerURI()).thenReturn(testServerURL);
		
		final DXClusterSitePoller sp = new DXClusterSitePoller(new File("src/test/resources"), config);
		final ClusterRecord[] records = sp.poll();
		LOGGER.info("Read {} records", records.length);
		for (final ClusterRecord clusterRecord : records) {
			LOGGER.info(clusterRecord.toDbString());
		}
		assertThat(records.length, equalTo(1));
		
		verify(config).getCallsigns();
		verify(config).getServerURI();
	}

	@Test
	public void loadingOfFileWorks() throws JsonParseException, JsonMappingException, IOException {		
		final ClusterRecord[] records = getRecordsFromSampleFile();
		
		assertThat(records.length, equalTo(35));
	}

	@Test
	public void filterNoCallsigns() throws JsonParseException, JsonMappingException, IOException {		
		final ClusterRecord[] records = getRecordsFromSampleFile();

		final ClusterRecord[] filtered = DXClusterSitePoller.filterCallsigns(Collections.<String>emptySet(), records);
		assertThat(filtered.length, equalTo(0));
	}

	@Test
	public void filterCallsigns() throws JsonParseException, JsonMappingException, IOException {		
		final ClusterRecord[] records = getRecordsFromSampleFile();
		final HashSet<String> callsigns = new HashSet<String>();
		callsigns.add("UA5D");
		callsigns.add("HG225");
		// HG225A is present in feed, imagine I'd configured that but they were on air as HG225A/P
		// so I want to see this.

		final ClusterRecord[] filtered = DXClusterSitePoller.filterCallsigns(callsigns, records);
		
		System.out.println("Read " + filtered.length + " records");
		final HashSet<String> dxcalls = new HashSet<String>();
		for (final ClusterRecord clusterRecord : filtered) {
			System.out.println(clusterRecord);
			dxcalls.add(clusterRecord.getDxcall());
		}
		assertThat(filtered.length, equalTo(4));
		assertThat(dxcalls, Matchers.containsInAnyOrder("UA5D", "HG225E", "HG225MSE", "HG225A"));
	}

	private ClusterRecord[] getRecordsFromSampleFile() throws IOException, JsonParseException, JsonMappingException {
		return new ObjectMapper().readValue(ResourceLoader.getResourceInputStream("pretty_printed_dxcluster.json"), ClusterRecord[].class);
	}

}

