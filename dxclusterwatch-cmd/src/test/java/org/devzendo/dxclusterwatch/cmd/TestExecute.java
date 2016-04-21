package org.devzendo.dxclusterwatch.cmd;

import static org.hamcrest.Matchers.containsString;

import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestExecute {
	final File cwd = new File(System.getProperty("user.dir"));

	@Rule
	public ExpectedException thrown = ExpectedException.none();


	@BeforeClass
	public static void setupLogging() {
		BasicConfigurator.resetConfiguration();
		BasicConfigurator.configure();
	}

	@Test
	public void testSuccessfulExecution() {
		new Execute(cwd, "ls", "-l").run();
	}

	@Test
	public void testFailedExecution() {		
		thrown.expect(RuntimeException.class);
        thrown.expectMessage(containsString("Cannot run program \"nonexistent\" (in directory \"" + cwd.getAbsolutePath() + "\"): error=2, No such file or directory"));

		new Execute(cwd, "nonexistent", "program").run();
	}
}
