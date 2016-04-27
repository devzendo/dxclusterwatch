package org.devzendo.dxclusterwatch.cmd;

import java.io.File;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.core.header.InBoundHeaders;

public class DXClusterSitePoller implements SitePoller {
	private static Logger LOGGER = LoggerFactory.getLogger(DXClusterSitePoller.class);
	private final WebResource webResource;
	private final Config config;

	public DXClusterSitePoller(final File prefsDir, final String serverUrl, final Set<String> callsigns, final Config config) {
		this.config = config;
		try {
			KeyStore.getInstance("JKS");
			System.setProperty("javax.net.ssl.trustStore", new File(prefsDir, "cacerts").getAbsolutePath());

			final ClientConfig clientConfig = new DefaultClientConfig();
			clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
			clientConfig.getClasses().add(JacksonJsonProvider.class);
			
			final Client client = Client.create(clientConfig);
			//client.addFilter(new LoggingFilter(System.out));
			
			// DXCluster.co.uk always sends a content-type of text/html, which Jersey doesn't interpret as application/json, so bodge the response
			// to be application/json to force JSON deserialisation.
			client.addFilter(new ClientFilter() {

				@Override
				public ClientResponse handle(final ClientRequest cr) throws ClientHandlerException {
					final ClientResponse response = getNext().handle(cr);
					final InBoundHeaders headers = new InBoundHeaders();
					final MultivaluedMap<String, String> origHeaders = response.getHeaders();
					for (final String header: origHeaders.keySet()) {
						if (header.equalsIgnoreCase(HttpHeaders.CONTENT_TYPE)) {
							//System.out.println("Setting the application/json content type");
							headers.putSingle(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						} else {
							final List<String> origHeaderValue = origHeaders.get(header);
							//System.out.println("Setting header " + header + " to " + origHeaderValue);
							headers.put(header, origHeaderValue);
						}
					}
					return new ClientResponse(response.getStatus(), headers, response.getEntityInputStream(), client.getMessageBodyWorkers());
				}});
			
			webResource = client.resource(serverUrl);
		} catch (final KeyStoreException e1) {
			final String msg = "Can't set up key store: " + e1.getMessage();
			LOGGER.error(msg);
			throw new RuntimeException(msg, e1);
		}
	}

	@Override
	public ClusterRecord[] poll() {
		LOGGER.debug("Polling DXCluster...");
		final long start = System.currentTimeMillis();
		final ClientResponse clientResponse = webResource.type(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		if (clientResponse.getStatus() == 200) {
			LOGGER.debug("Response: " + clientResponse);
			final ClusterRecord[] r = clientResponse.getEntity(ClusterRecord[].class);
			final Set<String> callsigns = config.getCallsigns();
			final ClusterRecord[] filtered = filterCallsigns(callsigns, r);
			final long stop = System.currentTimeMillis();
			LOGGER.debug("Retrieved {} records in {} ms, {} filtered records", r.length, (stop - start), filtered.length);
			return filtered;
		} else {
			LOGGER.warn("Failed to poll DXCluster: " + clientResponse);
			return new ClusterRecord[0];
		}
	}

	static ClusterRecord[] filterCallsigns(final Set<String> callsigns, final ClusterRecord[] records) {
		LOGGER.debug("Filtering {} records by {} callsigns", records.length, callsigns.size());
		if (callsigns == null || callsigns.isEmpty()) {
			return new ClusterRecord[0];
		}
		
		final ArrayList<ClusterRecord> outList = new ArrayList<>();
		for (final ClusterRecord cr : records) {
			final String upperDXCall = cr.getDxcall().toUpperCase();
			boolean found = false;
			for (final String callsign : callsigns) {
				//LOGGER.debug("Does received callsign {} contain requested {}", upperDXCall, callsign);
				if (upperDXCall.contains(callsign)) {
					found = true;
				}
			}
			if (found) {
				outList.add(cr);
			}
		}
			
		return outList.toArray(new ClusterRecord[0]);
	}
}
