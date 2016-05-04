package org.devzendo.dxclusterwatch.cmd;

import static org.junit.Assert.*;

import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.atLeast;
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
import org.devzendo.commoncode.concurrency.ThreadUtils;
import org.devzendo.commoncode.resource.ResourceLoader;
import org.devzendo.dxclusterwatch.cmd.ClusterRecord;
import org.devzendo.dxclusterwatch.cmd.Config;
import org.devzendo.dxclusterwatch.impl.TestDXClusterSitePoller;
import org.devzendo.dxclusterwatch.test.FakeDXCluster;
import org.devzendo.dxclusterwatch.test.LoggingUnittest;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.verification.AtLeast;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.verification.VerificationMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class TestController {

	private static Logger LOGGER = LoggerFactory.getLogger(TestController.class);

	@BeforeClass
	public static void setupLogging() {
		LoggingUnittest.initialise();
	}

	
	@After
	public void stopController() {
		if (controller != null) {
			controller.stop();
		}
		if (controllerThread.isAlive()) {
			controllerThread.interrupt();
		}
	}
	
	private Thread controllerThread; // controller start() is blocking, so need to start it elsewhere.
	private volatile Controller controller;

	@Mock
	private Config config;
	@Mock
	private Persister persister;
	@Mock
	private PageBuilder pageBuilder;
	@Mock
	private Tweeter tweeter;
	@Mock
	private SitePoller sitePoller;

	@Test
	public void startsAndStops() {
		when(config.getPollMinutes()).thenReturn(1);
		when(sitePoller.poll()).thenReturn(new ClusterRecord[0]);
		when(config.getTweetSeconds()).thenReturn(1);
		when(persister.getNextRecordToTweet()).thenReturn(null);
		
		controllerThread = new Thread(new Runnable() {

			@Override
			public void run() {
				controller = new Controller(config, persister, pageBuilder, tweeter, sitePoller);
				controller.start();
			}});
		controllerThread.setDaemon(true);
		controllerThread.start();
		
		ThreadUtils.waitNoInterruption(2500);
		controller.stop();
		
		verify(config, atLeast(1)).getPollMinutes();
	}
}
