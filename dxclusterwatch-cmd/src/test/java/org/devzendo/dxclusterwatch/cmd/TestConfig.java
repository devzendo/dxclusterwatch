package org.devzendo.dxclusterwatch.cmd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

import java.util.Set;

import org.junit.Test;

public class TestConfig {

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

}
