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
	private static final String LINE_SEP = System.getProperty("line.separator");

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


	@Test
	public void nullBoolean() throws Exception {
		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("Empty property 'enable'"));

		Config.mustBeBoolean("enable", null);
	}

	@Test
	public void emptyBoolean() throws Exception {
		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("Empty property 'enable'"));

		Config.mustBeBoolean("enable", "");
	}

	@Test
	public void notABoolean() throws Exception {
		thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("ffs is not a boolean"));

		Config.mustBeBoolean("enable", "ffs");
	}

	@Test
	public void trueBoolean() throws Exception {
		assertThat(Config.mustBeBoolean("enable", "TRuE"), equalTo(true));
		assertThat(Config.mustBeBoolean("enable", "true"), equalTo(true));
		assertThat(Config.mustBeBoolean("enable", "TRUE"), equalTo(true));
		assertThat(Config.mustBeBoolean("enable", "yes"), equalTo(true));
		assertThat(Config.mustBeBoolean("enable", "Yes"), equalTo(true));
		assertThat(Config.mustBeBoolean("enable", "YES"), equalTo(true));
	}

	@Test
	public void falseBoolean() throws Exception {
		assertThat(Config.mustBeBoolean("enable", "FaLsE"), equalTo(false));
		assertThat(Config.mustBeBoolean("enable", "false"), equalTo(false));
		assertThat(Config.mustBeBoolean("enable", "FALSE"), equalTo(false));
		assertThat(Config.mustBeBoolean("enable", "no"), equalTo(false));
		assertThat(Config.mustBeBoolean("enable", "No"), equalTo(false));
		assertThat(Config.mustBeBoolean("enable", "NO"), equalTo(false));
	}
	
	@Test
	public void loadConfig() throws IOException {
		final boolean feedReadingEnabled = true;
		final File tempFile = createSampleConfig(root, feedReadingEnabled);
		final Config config = new Config(tempFile);
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
		assertThat(config.isEnableFeedReading(), equalTo(feedReadingEnabled));
		assertThat(config.isEnablePageUpdating(), equalTo(true));
		assertThat(config.isEnableTweeting(), equalTo(false));;
	}

	private File createSampleConfig(final File dir, final boolean feedReadingEnabled) throws IOException {
		final File properties = new File(dir, "test.properties");
		final File siteRepo = new File(dir, "site.repo");
		siteRepo.mkdir();
		final FileWriter fileWriter = new FileWriter(properties);
		try {
			fileWriter.write("callsigns=m0cuv,2e0sql" + LINE_SEP);
			fileWriter.write("pollMinutes=1" + LINE_SEP);
			fileWriter.write("tweetSeconds=30" + LINE_SEP);
			fileWriter.write("siteRepoPath=" + siteRepo.getAbsolutePath() + LINE_SEP);
			fileWriter.write("hgExecutablePath=/opt/local/bin/hg" + LINE_SEP);
			fileWriter.write("consumerKey=abc" + LINE_SEP);
			fileWriter.write("consumerSecret=def" + LINE_SEP);
			fileWriter.write("accessToken=123abc" + LINE_SEP);
			fileWriter.write("accessSecret=def987" + LINE_SEP);
			fileWriter.write("maxListingEntries=20" + LINE_SEP);
			fileWriter.write("enableFeedReading=" + feedReadingEnabled + LINE_SEP);
			fileWriter.write("enablePageUpdating=yes" + LINE_SEP);
			fileWriter.write("enableTweeting=No" + LINE_SEP);
		} finally {
			fileWriter.close();
		}
		return properties;
	}
}
