package org.devzendo.dxclusterwatch.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BitbucketPagesPageBuilder implements PageBuilder {

	private static final Logger LOGGER = LoggerFactory.getLogger(BitbucketPagesPageBuilder.class);

	private final Config config;
	private final Persister persister;

	public BitbucketPagesPageBuilder(final Config config, final Persister persister) {
		this.config = config;
		this.persister = persister;
	}

	@Override
	public void rebuildPage() {
		// TODO Auto-generated method stub

	}

}
