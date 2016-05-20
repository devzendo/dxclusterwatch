package org.devzendo.dxclusterwatch.impl;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.codehaus.jackson.map.ObjectMapper;
import org.devzendo.dxclusterwatch.cmd.ClusterRecord;
import org.devzendo.dxclusterwatch.cmd.Persister;
import org.devzendo.dxclusterwatch.util.Signals;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.misc.SignalHandler;

public class ReplayDXClusterServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(ReplayDXClusterServer.class);
	private static final int PORT = 9080;
	
	private final Persister persister;

	private final AtomicBoolean running = new AtomicBoolean(true);
	private final SignalHandler oldIntHandler;
	private final Connection connection;
	private final List<ClusterRecord> currentPage = new ArrayList<>();
	private final Thread mainThread;

	public ReplayDXClusterServer(final File replayDbDir) throws IOException {
		persister = new H2Persister(replayDbDir, 0);
		mainThread = Thread.currentThread();

		this.oldIntHandler = Signals.withHandler(new Runnable() {
			@Override
			public void run() {
	            LOGGER.info("Interrupt received, stopping");
	            running.set(false);
	            mainThread.interrupt();
			}}, Signals.SignalName.INT);

		final FakeServer container = new FakeServer(this);
		connection = new SocketConnection(container);
		LOGGER.info("Listening on port {}", PORT);
		final SocketAddress address = new InetSocketAddress(PORT);
		
		connection.connect(address);
	}

	public List<ClusterRecord> getData() {
		synchronized(currentPage) {
			return Collections.unmodifiableList(currentPage);
		}
	}

	private void start() {
		LOGGER.info("Starting....");
		final Timestamp earliestTimeRecord = persister.getEarliestTimeRecord();
		LOGGER.info("Earliest recording is at time {}", earliestTimeRecord);
		int minutesFromEarliestRecording = 0;
		while (earliestTimeRecord != null && running.get()) {
			
			final long currentMinuteMsSinceEpoch = earliestTimeRecord.getTime() + (minutesFromEarliestRecording * 60 * 1000);
			final int windowInMinutes = 30;
			final long windowMinutesBeforeCurrentMinute = currentMinuteMsSinceEpoch - (windowInMinutes * 60 * 1000);
			final Timestamp from = new Timestamp(windowMinutesBeforeCurrentMinute);
			final Timestamp to = new Timestamp(currentMinuteMsSinceEpoch);
			
			final List<ClusterRecord> data = persister.getRecordsBetween(from, to); // [ earliest .. most recent ]
			LOGGER.info("Page [{} .. {}] contains {} record(s)", from, to, data.size());
			synchronized(currentPage) {
				currentPage.clear();
				currentPage.addAll(data);
			}
			try {
				Thread.sleep(1000);
			} catch (final InterruptedException e) {
				LOGGER.debug("Sleep interrupted");
			}
			minutesFromEarliestRecording+=5;
		}
		
		LOGGER.info("Finished");

		try {
			LOGGER.debug("Closing connection");
			connection.close();
		} catch (final IOException e) {
			LOGGER.warn("Closing FakeDXCluster caught IOException: " + e.getMessage(), e);
		}

		try {
			LOGGER.debug("Closing database");
			persister.close();
		} catch (final Exception e) {
			LOGGER.warn("Could not close db: " + e.getMessage());
		}
		LOGGER.debug("Reinstating old signal handler");
		Signals.handle(oldIntHandler, Signals.SignalName.INT);
	}

	public static class FakeServer implements Container {

		private final ReplayDXClusterServer clusterServer;
		private final ObjectMapper objectMapper;

		public FakeServer(final ReplayDXClusterServer replayDXClusterServer) {
			this.clusterServer = replayDXClusterServer;
			objectMapper = new ObjectMapper();
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
				final List<ClusterRecord> list = clusterServer.getData();
				LOGGER.info("Serving {} records", list.size());
				final String json = objectMapper.writeValueAsString(list);
				LOGGER.debug("Target: " + target + " json: '" + json + "'");
				if (json == null) {
					response.setCode(404);
				} else {
					body.print(json);
				}
				body.close();
			} catch (final IOException e) {
				LOGGER.warn("FakeDXCluster caught IOException: " + e.getMessage(), e);
			}
		}
	}
	
	public static void main(final String[] args) {
		setupLogging();
		try {
			File replayDbDir = null;
			for (int i = 0; i < args.length; i++ ) {
				final String arg = args[i];
				if (arg.equals("-d")) {
					if (i == args.length - 1) {
						throw new IllegalArgumentException("-d needs an argument");
					}
					replayDbDir = new File(args[++i]);
					if (!replayDbDir.exists() || !replayDbDir.isDirectory()) {
						throw new IllegalArgumentException("-d directory " + replayDbDir + " doesn't exist, or isn't a directory");
					}
					
				}
			}
			if (replayDbDir == null) {
				throw new IllegalAccessException("Must specify a directory with -d");
			}
			new ReplayDXClusterServer(replayDbDir).start();
			LOGGER.info("Terminating");
		} catch (final Exception e) {
			LOGGER.warn(e.getMessage(), e);
		}
	}

	private static void setupLogging() {
		BasicConfigurator.resetConfiguration();
		final org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();
		rootLogger.addAppender(new ConsoleAppender(new PatternLayout("%d{ISO8601} %5p %c{1}:%L - %m%n")));
		rootLogger.setLevel(Level.INFO);
	}
}
