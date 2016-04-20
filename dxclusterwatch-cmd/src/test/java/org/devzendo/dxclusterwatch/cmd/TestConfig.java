package org.devzendo.dxclusterwatch.cmd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class TestConfig {

	@Rule
	public final TemporaryFolder tempDir = new TemporaryFolder();
	private File root;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@BeforeClass
	public static void setupLogging() {
		LoggingUnittest.initialise();
	}

	@Before
	public void setupTempfile() throws IOException {
		tempDir.create();
		root = tempDir.getRoot();
	}

	@Test
	public void emptyCallsigns() {
		assertThat(Config.getCallsigns(""), empty());
	}

	@Test
	public void spaceCallsigns() {
		assertThat(Config.getCallsigns(" "), empty());
	}

	@Test
	public void nullCallsigns() {
		assertThat(Config.getCallsigns(null), empty());
	}

	@Test
	public void singleCallsigns() {
		Set<String> set = Config.getCallsigns("m0cuv");
		assertThat(set, hasSize(1));
		assertThat(set, contains("M0CUV"));
	}

	@Test
	public void multipleCallsigns() {
		Set<String> set = Config.getCallsigns(" m0cuv , xxt832,ff1tr  ");
		assertThat(set, hasSize(3));
		assertThat(set, containsInAnyOrder("M0CUV", "XXT832", "FF1TR"));
	}

	@Test
	public void existentPath() throws Exception {
		assertThat(root.exists(), equalTo(true));
		Config.mustBePath("siteRepoPath", root.getAbsolutePath());
	}

	@Test
	public void emptyPath() throws Exception {
		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("Empty property 'siteRepoPath'"));

		Config.mustBePath("siteRepoPath", "");
	}

	@Test
	public void nullPath() throws Exception {
		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("Empty property 'siteRepoPath'"));

		Config.mustBePath("siteRepoPath", null);
	}

	@Test
	public void nonExistentPath() throws Exception {
		final File nonExistentPath = new File(root, "nonexistent");
		assertThat(nonExistentPath.exists(), equalTo(false));

		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("nonexistent directory does not exist"));

		Config.mustBePath("siteRepoPath", nonExistentPath.getPath());
	}

	@Test
	public void pathPointsToFile() throws Exception {
		final File file = new File(root, "nonexistent");
		FileWriter fileWriter = new FileWriter(file);
		fileWriter.append("foo");
		fileWriter.close();
		
		assertThat(file.exists(), equalTo(true));
		assertThat(file.isFile(), equalTo(true));

		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("nonexistent is a file but should be a directory"));

		Config.mustBePath("siteRepoPath", file.getAbsolutePath());
	}

}
