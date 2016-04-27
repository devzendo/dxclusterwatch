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

import org.devzendo.commoncode.concurrency.ThreadUtils;
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
		assertThat(PropertiesConfig.getCallsigns(""), empty());
	}

	@Test
	public void spaceCallsigns() {
		assertThat(PropertiesConfig.getCallsigns(" "), empty());
	}

	@Test
	public void nullCallsigns() {
		assertThat(PropertiesConfig.getCallsigns(null), empty());
	}

	@Test
	public void singleCallsigns() {
		final Set<String> set = PropertiesConfig.getCallsigns("m0cuv");
		assertThat(set, hasSize(1));
		assertThat(set, contains("M0CUV"));
	}

	
	@Test
	public void emptyString() {
		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("Empty property 'xx'"));

		PropertiesConfig.mustBeString("xx", "");
	}

	@Test
	public void spaceString() {
		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("Empty property 'xx'"));

        PropertiesConfig.mustBeString("xx", " ");
	}

	@Test
	public void nullString() {
		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("Empty property 'xx'"));

        PropertiesConfig.mustBeString("xx", null);
	}

	@Test
	public void singleString() {
		assertThat(PropertiesConfig.mustBeString("xx", "yy"), equalTo("yy"));
	}

	
	@Test
	public void multipleCallsigns() {
		final Set<String> set = PropertiesConfig.getCallsigns(" m0cuv , xxt832,ff1tr  ");
		assertThat(set, hasSize(3));
		assertThat(set, containsInAnyOrder("M0CUV", "XXT832", "FF1TR"));
	}

	@Test
	public void existentPath() throws Exception {
		assertThat(root.exists(), equalTo(true));
		PropertiesConfig.mustBePath("siteRepoPath", root.getAbsolutePath());
	}

	@Test
	public void emptyPath() throws Exception {
		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("Empty property 'siteRepoPath'"));

		PropertiesConfig.mustBePath("siteRepoPath", "");
	}

	@Test
	public void nullPath() throws Exception {
		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("Empty property 'siteRepoPath'"));

		PropertiesConfig.mustBePath("siteRepoPath", null);
	}

	@Test
	public void nonExistentPath() throws Exception {
		final File nonExistentPath = new File(root, "nonexistent");
		assertThat(nonExistentPath.exists(), equalTo(false));

		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("nonexistent directory does not exist"));

		PropertiesConfig.mustBePath("siteRepoPath", nonExistentPath.getPath());
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

		PropertiesConfig.mustBePath("siteRepoPath", file.getAbsolutePath());
	}

	@Test
	public void existentExecutablePath() throws Exception {
		assertThat(root.exists(), equalTo(true));
		PropertiesConfig.mustBeExecutablePath("hgExecutablePath", root.getAbsolutePath());
	}

	@Test
	public void emptyExecutablePath() throws Exception {
		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("Empty property 'hgExecutablePath'"));

		PropertiesConfig.mustBeExecutablePath("hgExecutablePath", "");
	}

	@Test
	public void nullExecutablePath() throws Exception {
		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("Empty property 'hgExecutablePath'"));

		PropertiesConfig.mustBeExecutablePath("hgExecutablePath", null);
	}

	@Test
	public void nonExistentExecutablePath() throws Exception {
		final File nonExistentPath = new File(root, "nonexistent");
		assertThat(nonExistentPath.exists(), equalTo(false));

		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("nonexistent executable does not exist"));

		PropertiesConfig.mustBeExecutablePath("hgExecutablePath", nonExistentPath.getPath());
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

		PropertiesConfig.mustBeExecutablePath("hgExecutablePath", file.getAbsolutePath());
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

		PropertiesConfig.mustBeExecutablePath("hgExecutablePath", file.getAbsolutePath());
	}
	
	@Test
	public void nullInteger() throws Exception {
		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("Empty property 'pollMinutes'"));

		PropertiesConfig.mustBeInteger("pollMinutes", null);
	}

	@Test
	public void emptyInteger() throws Exception {
		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("Empty property 'pollMinutes'"));

		PropertiesConfig.mustBeInteger("pollMinutes", "");
	}

	@Test
	public void notAnInteger() throws Exception {
		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("ffs is not an integer"));

		PropertiesConfig.mustBeInteger("pollMinutes", "ffs");
	}


	@Test
	public void nullBoolean() throws Exception {
		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("Empty property 'enable'"));

		PropertiesConfig.mustBeBoolean("enable", null);
	}

	@Test
	public void emptyBoolean() throws Exception {
		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("Empty property 'enable'"));

		PropertiesConfig.mustBeBoolean("enable", "");
	}

	@Test
	public void notABoolean() throws Exception {
		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("ffs is not a boolean"));

		PropertiesConfig.mustBeBoolean("enable", "ffs");
	}

	@Test
	public void trueBoolean() throws Exception {
		assertThat(PropertiesConfig.mustBeBoolean("enable", "TRuE"), equalTo(true));
		assertThat(PropertiesConfig.mustBeBoolean("enable", "true"), equalTo(true));
		assertThat(PropertiesConfig.mustBeBoolean("enable", "TRUE"), equalTo(true));
		assertThat(PropertiesConfig.mustBeBoolean("enable", "yes"), equalTo(true));
		assertThat(PropertiesConfig.mustBeBoolean("enable", "Yes"), equalTo(true));
		assertThat(PropertiesConfig.mustBeBoolean("enable", "YES"), equalTo(true));
	}

	@Test
	public void falseBoolean() throws Exception {
		assertThat(PropertiesConfig.mustBeBoolean("enable", "FaLsE"), equalTo(false));
		assertThat(PropertiesConfig.mustBeBoolean("enable", "false"), equalTo(false));
		assertThat(PropertiesConfig.mustBeBoolean("enable", "FALSE"), equalTo(false));
		assertThat(PropertiesConfig.mustBeBoolean("enable", "no"), equalTo(false));
		assertThat(PropertiesConfig.mustBeBoolean("enable", "No"), equalTo(false));
		assertThat(PropertiesConfig.mustBeBoolean("enable", "NO"), equalTo(false));
	}
	
	@Test
	public void loadConfig() throws IOException {
		final boolean feedReadingEnabled = true;
		final File tempFile = ConfigUnittest.createSampleConfig(root, feedReadingEnabled);
		final Config config = new PropertiesConfig(tempFile);
		assertThat(config.getCallsigns(), containsInAnyOrder("M0CUV", "2E0SQL"));
		assertThat(config.getPollMinutes(), equalTo(1));
		assertThat(config.getTweetSeconds(), equalTo(30));
		assertThat(config.getSiteRepoPath(), equalTo(new File(root, "site.repo")));
		assertThat(config.getHgExecutablePath(), equalTo(new File("/opt/local/bin/hg")));
		assertThat(config.getConsumerKey(), equalTo("abc"));
		assertThat(config.getConsumerSecret(), equalTo("def"));
		assertThat(config.getAccessToken(), equalTo("123abc"));
		assertThat(config.getAccessSecret(), equalTo("def987"));
		assertThat(config.getMaxListingEntries(), equalTo(20));
		assertThat(config.isFeedReadingEnabled(), equalTo(feedReadingEnabled));
		assertThat(config.isPageUpdatingEnabled(), equalTo(true));
		assertThat(config.isTweetingEnable(), equalTo(false));;
	}

	@Test
	public void changesToConfigAreAutomaticallyReloaded() throws IOException {
		final boolean initialFeedReadingEnabled = true;
		final File tempFile = ConfigUnittest.createSampleConfig(root, initialFeedReadingEnabled);
		final Config config = new PropertiesConfig(tempFile);
		assertThat(config.isFeedReadingEnabled(), equalTo(initialFeedReadingEnabled));

		// need to leave a few seconds for file modification time change to be discernible
		ThreadUtils.waitNoInterruption(2000);
		
		final boolean newFeedReadingEnabled = false;
		ConfigUnittest.createSampleConfig(root, newFeedReadingEnabled );
		
		assertThat(config.isFeedReadingEnabled(), equalTo(newFeedReadingEnabled));
	}
}
