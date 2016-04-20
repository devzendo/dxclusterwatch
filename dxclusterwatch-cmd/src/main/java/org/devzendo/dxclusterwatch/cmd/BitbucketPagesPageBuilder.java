package org.devzendo.dxclusterwatch.cmd;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.devzendo.commoncode.resource.ResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BitbucketPagesPageBuilder implements PageBuilder {

	private static final Logger LOGGER = LoggerFactory.getLogger(BitbucketPagesPageBuilder.class);

	private final Persister persister;
	private final File indexFile;

	public BitbucketPagesPageBuilder(final Config config, final Persister persister) {
		this.persister = persister;

		indexFile = new File(config.getSiteRepoPath(), "index.html");
	}

	@Override
	public void rebuildPage() {
		LOGGER.debug("Rebuilding page");
		try {
			final FileWriter fw = new FileWriter(indexFile);
			try {
				writeHeader(fw);
				final List<ClusterRecord> records = persister.getRecords();
				if (records.isEmpty()) {
					writeEmpty(fw);
				} else {
					writeRecords(fw, records);
				}
				writeFooter(fw);
			} finally {
				fw.close();
			}
		} catch (final IOException ioe) {
			LOGGER.warn("Could not write to {}: {}", indexFile, ioe.getMessage());
		}
	}

	private void writeHeader(final FileWriter fw) throws IOException {
		appendResource(fw, "header.html");

		final SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
		dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));

		// Time in GMT
		fw.write("Page updated " + dateFormatGmt.format(new Date()));
		fw.write("<hr>");
	}

	private void writeRecords(final FileWriter fw, final List<ClusterRecord> records) throws IOException {
		fw.write("<div class=\"contents\">");
		fw.write("<table cellspacing=\"0\" class=\"spots\">");
		fw.write("<tr class=\"title\">");
		fw.write("<td>Date</td>");
		fw.write("<td>Freq</td>");
		fw.write("<td>IMD Station Callsign</td>");
		fw.write("<td>Called by</td>");
		fw.write("<td>Comment</td>");
		fw.write("</tr>");

		int num = 0;
		for (final ClusterRecord record : records) {
			fw.write("<tr class=\"tr" + num + "\">");
			num ^= 1;
			fw.write("<td>" + record.getTime() + "</td>");
			fw.write("<td>" + record.getFreq() + "</td>");
			fw.write("<td>" + record.getDxcall() + "</td>");
			fw.write("<td>" + record.getCall() + "</td>");
			fw.write("<td>" + record.getComment() + "</td>");
			fw.write("</tr>");
		}
		
		fw.write("</table></div>");
	}

	private void writeEmpty(final FileWriter fw) throws IOException {
		fw.write("No stations heard yet - check back later!");
	}

	private void writeFooter(final FileWriter fw) throws IOException {
		appendResource(fw, "footer.html");
	}

	private void appendResource(final FileWriter fw, final String resourcePath) throws IOException {
		fw.write(ResourceLoader.readResource(resourcePath));
	}
}
