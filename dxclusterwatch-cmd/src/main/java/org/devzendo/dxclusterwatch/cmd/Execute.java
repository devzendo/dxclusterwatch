package org.devzendo.dxclusterwatch.cmd;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Execute {
	private static final Logger LOGGER = LoggerFactory.getLogger(Execute.class);
	private final ProcessBuilder processBuilder;
	private final List<String> output = new ArrayList<String>();
	private StreamReader thread;
	private Process process;
	private InputStream inputStream;
	private CountDownLatch done;
	
	public Execute(final File inDirectory, final String ... cmdline) {
		processBuilder = new ProcessBuilder(cmdline).directory(inDirectory).redirectErrorStream(true);
		LOGGER.debug("Executing '" + StringUtils.join(cmdline, ' ') + "' in directory '" + inDirectory.getAbsolutePath() + "'");
	}

	class StreamReader extends Thread {
        private final InputStream mInputStream;

        /**
         * Construct a reading thread monitoring an InputStream
         * @param is the InputStream to monitor
         */
        StreamReader(final InputStream is) {
            this.mInputStream = is;
        }

        @Override
        public void run() {
            try {
                String l = null;
                final BufferedReader br = new BufferedReader(new InputStreamReader(mInputStream));
                while ((l = br.readLine()) != null) {
                	LOGGER.debug("Output: " + l);
                    output.add(l);
                }
            } catch (final IOException ioe) {
                LOGGER.warn("Failed to read stream: " + ioe.getMessage());
            }
            done.countDown();
        }
    }

	public void run() {
		try {
			process = processBuilder.start();
			inputStream = process.getInputStream();
			done = new CountDownLatch(1);
			thread = new StreamReader(inputStream);

			thread.start();
			final int exitCode = process.waitFor();
			done.await();
			if (exitCode == 0) {
				LOGGER.debug("Process ended with successful exit code 0");
			} else {
				throw new RuntimeException("Process ended with failure exit code " + exitCode);
			}
				
		} catch (final IOException e) {
			LOGGER.warn(e.getMessage());
			throw new RuntimeException(e);
		} catch (final InterruptedException e) {
			LOGGER.warn(e.getMessage());
			throw new RuntimeException(e);
		} finally {
			close();
		}
	}

	public void close() {
        if (thread != null) {
            try {
                thread.join();
            } catch (final InterruptedException e) {
                LOGGER.warn("Interrupted waiting for output reader thread: " + e.getMessage());
            }
        }
        if (process != null) {
            process.destroy();
        }
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (final IOException ioe) {
                LOGGER.warn("Could not output stream: " + ioe.getMessage());
            }
        }
    }
}
