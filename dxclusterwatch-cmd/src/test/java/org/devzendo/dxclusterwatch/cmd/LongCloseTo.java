package org.devzendo.dxclusterwatch.cmd;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class LongCloseTo extends TypeSafeMatcher<Long> {
	private final Long value;
	private final Long delta;

	/**
	 * Creates a matcher of Long that matches when an examined Long is equal
	 * to the specified <code>operand</code>, within a range of +/-
	 * <code>error</code>.
	 * @param value
	 *            the expected value of matching Long
	 * @param error
	 *            the delta (+/-) within which matches will be allowed
	 */
	public static TypeSafeMatcher<Long> closeTo(final Long value, final Long error) {
		return new LongCloseTo(value, error);
	}

	public LongCloseTo(final Long value, final Long error) {
		this.value = value;
		this.delta = error;
	}

	@Override
	public void describeMismatchSafely(final Long item, final Description mismatchDescription) {
		mismatchDescription.appendValue(item).appendText(" differed by ").appendValue(actualDelta(item))
				.appendText(" more than delta ").appendValue(delta);
	}

	@Override
	public void describeTo(final Description description) {
		description.appendText("a numeric value within ").appendValue(delta).appendText(" of ").appendValue(value);
	}

	private Long actualDelta(final Long item) {
		return Math.abs(item - value) - delta;
	}

	@Override
	protected boolean matchesSafely(final Long item) {
	      return actualDelta(item) <= 0;
	}
}