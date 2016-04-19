package org.devzendo.dxclusterwatch.cmd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import java.io.File;

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
		final DXClusterSitePoller sp = new DXClusterSitePoller(new File("src/test/resources"), mainServerUrl);
		final ClusterRecord[] records = sp.poll();
		System.out.println("Read " + records.length + " records");
		for (ClusterRecord clusterRecord : records) {
			System.out.println(clusterRecord);
		}
		assertThat(records.length, greaterThan(0));
	}

}

