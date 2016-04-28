package org.devzendo.dxclusterwatch.cmd;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;

import org.devzendo.commoncode.resource.ResourceLoader;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FakeDXCluster implements Container {

	private static Logger LOGGER = LoggerFactory.getLogger(FakeDXCluster.class);

	private final int port;
	private Connection connection;	

	public static FakeDXCluster createServer(final int port) throws IOException {
		final FakeDXCluster container = new FakeDXCluster(port);
		final Connection connection = new SocketConnection(container);
		final SocketAddress address = new InetSocketAddress(port);
		
		connection.connect(address);
		container.setConnection(connection);
		return container;
	}
	
	public FakeDXCluster(final int port) {
		this.port = port;
	}

	private void setConnection(final Connection connection) {
		this.connection = connection;
	}

	@Override
	public void handle(final Request request, final Response response) {
		try {
			final long time = System.currentTimeMillis();

			response.set("Content-Type", "text/html"); // it's application/json really, but simulate the response the real DXCluster sends.
			response.set("Server", "FakeDXCluster/1.0 (Simple 4.0)");
			response.setDate("Date", time);
			response.setDate("Last-Modified", time);

			final PrintStream body = response.getPrintStream();
			final String target = request.getTarget();
			final String output = ResourceLoader.readResource("original_dxcluster.html");
			LOGGER.debug("Target: " + target + " output: '" + output + "'");
			if (output == null) {
				response.setCode(404);
			} else {
				body.print(output);
			}
			body.close();
		} catch (final IOException e) {
			LOGGER.warn("FakeDXCluster caught IOException: " + e.getMessage(), e);
		}
	}

	/**
	 * Stop the server, close listening sockets, etc.
	 */
	public void stop() {
		try {
			connection.close();
		} catch (final IOException e) {
			LOGGER.warn("Closing FakeDXCluster caught IOException: " + e.getMessage(), e);
		}
	}

	public URI getURI() {
		return URI.create("http://localhost:" + port);
	}
}
