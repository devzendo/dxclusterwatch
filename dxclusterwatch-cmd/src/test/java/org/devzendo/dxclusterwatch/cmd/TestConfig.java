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
		final Set<String> set = Config.getCallsigns("m0cuv");
		assertThat(set, hasSize(1));
		assertThat(set, contains("M0CUV"));
	}

	
	@Test
	public void emptyString() {
		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("Empty property 'xx'"));

		Config.mustBeString("xx", "");
	}

	@Test
	public void spaceString() {
		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("Empty property 'xx'"));

        Config.mustBeString("xx", " ");
	}

	@Test
	public void nullString() {
		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("Empty property 'xx'"));

        Config.mustBeString("xx", null);
	}

	@Test
	public void singleString() {
		assertThat(Config.mustBeString("xx", "yy"), equalTo("yy"));
	}

	
	@Test
	public void multipleCallsigns() {
		final Set<String> set = Config.getCallsigns(" m0cuv , xxt832,ff1tr  ");
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
		final FileWriter fileWriter = new FileWriter(file);
		fileWriter.append("foo");
		fileWriter.close();
		
		assertThat(file.exists(), equalTo(true));
		assertThat(file.isFile(), equalTo(true));

		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("nonexistent is a file but should be a directory"));

		Config.mustBePath("siteRepoPath", file.getAbsolutePath());
	}

	@Test
	public void existentExecutablePath() throws Exception {
		assertThat(root.exists(), equalTo(true));
		Config.mustBeExecutablePath("hgExecutablePath", root.getAbsolutePath());
	}

	@Test
	public void emptyExecutablePath() throws Exception {
		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("Empty property 'hgExecutablePath'"));

		Config.mustBeExecutablePath("hgExecutablePath", "");
	}

	@Test
	public void nullExecutablePath() throws Exception {
		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("Empty property 'hgExecutablePath'"));

		Config.mustBeExecutablePath("hgExecutablePath", null);
	}

	@Test
	public void nonExistentExecutablePath() throws Exception {
		final File nonExistentPath = new File(root, "nonexistent");
		assertThat(nonExistentPath.exists(), equalTo(false));

		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("nonexistent executable does not exist"));

		Config.mustBeExecutablePath("hgExecutablePath", nonExistentPath.getPath());
	}

	@Test
	public void pathPointsToNonExecutable() throws Exception {
		final File file = new File(root, "somefile");
		final FileWriter fileWriter = new FileWriter(file);
		fileWriter.append("foo");
		fileWriter.close();
		
		assertThat(file.exists(), equalTo(true));
		assertThat(file.isFile(), equalTo(true));
		assertThat(file.canExecute(), equalTo(false));

		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("somefile is not executable"));

		Config.mustBeExecutablePath("hgExecutablePath", file.getAbsolutePath());
	}

	@Test
	public void pathPointsToExecutable() throws Exception {
		final File file = new File(root, "somefile");
		final FileWriter fileWriter = new FileWriter(file);
		fileWriter.append("foo");
		fileWriter.close();
		file.setExecutable(true);
		
		assertThat(file.exists(), equalTo(true));
		assertThat(file.isFile(), equalTo(true));
		assertThat(file.canExecute(), equalTo(true));

		Config.mustBeExecutablePath("hgExecutablePath", file.getAbsolutePath());
	}
	
	@Test
	public void nullInteger() throws Exception {
		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("Empty property 'pollMinutes'"));

		Config.mustBeInteger("pollMinutes", null);
	}

	@Test
	public void emptyInteger() throws Exception {
		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("Empty property 'pollMinutes'"));

		Config.mustBeInteger("pollMinutes", "");
	}

	@Test
	public void notAnInteger() throws Exception {
		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("ffs is not an integer"));

		Config.mustBeInteger("pollMinutes", "ffs");
	}
}
