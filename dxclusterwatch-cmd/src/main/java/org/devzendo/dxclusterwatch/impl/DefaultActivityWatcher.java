package org.devzendo.dxclusterwatch.impl;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.devzendo.commoncode.time.Sleeper;
import org.devzendo.commoncode.types.RepresentationType;
import org.devzendo.dxclusterwatch.cmd.ActivityWatcher;
import org.devzendo.dxclusterwatch.cmd.ClusterRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultActivityWatcher implements ActivityWatcher {
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultActivityWatcher.class);

	private final Sleeper sleeper;

	public DefaultActivityWatcher(final Sleeper sleeper) {
		this.sleeper = sleeper;
	}

	public static int toInt(final String realStr) {
		final String defRealStr = StringUtils.defaultIfBlank(realStr, "0");
		final int dotIndex = defRealStr.indexOf('.');
		if (dotIndex == -1) {
			return Integer.parseInt(defRealStr);
		} else {
			return Integer.parseInt(defRealStr.substring(0,  dotIndex));
		}
	}

	private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
	private static final int SECONDS = 1000; // in ms
	private static final int MINUTES = 60 * SECONDS; // in ms
	private static final long EXPIRY_MS = 30 * MINUTES;
	{{
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	}}

	public static class Stuff {
		public Timestamp when;
		public int frequencyKHz;
		public boolean tweeted;
		public long expiryTime;
		public Stuff(final Timestamp when, final int freq, final long expiryTime) {
			this.when = when;
			this.frequencyKHz = freq;
			this.expiryTime = expiryTime;
			this.tweeted = false;
		}
		@Override
		public String toString() {
			final Date date = new Date(when.getTime());
			final String formattedTime = sdf.format(date);
			return "" + frequencyKHz + " " + (formattedTime);
		}
	}
	
	private class Callsign extends RepresentationType<String> {
		public Callsign(final String value) {
			super(value);
		}
	}
	
	private final Map<Callsign, List<Stuff>> map = new HashMap<>();
	
	@Override
	public int numEntries() {
		int count = 0;
		final Set<Entry<Callsign, List<Stuff>>> entries = map.entrySet();
		for (final Entry<Callsign, List<Stuff>> entry : entries) {
			count += entry.getValue().size();
		}
		return count;
	}
	
	@Override
	public int numCallsigns() {
		return map.size();
	}
	
	// callsign -> [ time, frequency, tweeted ]  // ordered on date/time, frequency is integerised
	// new callsign/frequency, add to outgoing. same? ignore.
	// already recorded but at a new time? ignore.
	// eg KM1CC (18075 19:56)
	// if multiple frequencies have been heard since last tweet
	// KM1CC (18075 19:56, 7240 20:01) <-- ordered on time (date taken into account too, but not displayed)
	
	// multiple calls heard
	// KM1CC (18075 19:56, 7240 20:01)
	// CW1GM (28490 20:01)
	// entries to be added to the tweet are sorted on earliest time
	// and all eligible are added to the tweet if they will fit.
	// if they fit, they're marked as tweeted so won't go out again
	// Times are removed from the callsign's time list after 30 mins. Callsigns with empty time lists are removed.
	@Override
	public void seen(final ClusterRecord record) {
		final Callsign callsign = new Callsign(record.getDxcall());
		if (map.containsKey(callsign)) {
			final List<Stuff> records = map.get(callsign);
			records.add(toStuff(record));
		} else {
			final List<Stuff> records = new ArrayList<>();
			records.add(toStuff(record));
			map.put(callsign, records);
		}
		purge();
	}

	private Stuff toStuff(final ClusterRecord record) {
		return new Stuff(record.getTimeAsTimestamp(), toInt(record.getFreq()), sleeper.currentTimeMillis() + EXPIRY_MS);
	}

	@Override
	// empty string if nothing to say.
	// get all entries that contain [time, freq, tweeted] that have not been tweeted. order by time.
	//
	// start with tweet=""
	// foreach entry
	//   form text
	//   if tweet+text fits,
	//     tweet += text
	//     mark entry tweeted
	//   else
	//     break
	// tweet
	// purge old times, and if callsign entry empty, purge that.
	public String latestTweetableActivity() {
		purge();
		if (map.isEmpty()) {
			return "";
		}
		final StringBuilder sb = new StringBuilder();
		final List<Callsign> callsignsByEntryTimes = sortMapByEntryTime();
		for (final Callsign callsign : callsignsByEntryTimes) {
			final List<Stuff> stuffs = map.get(callsign);
//			System.out.println("list of stuff for callsign " + callsign + " contains " + stuffs.size() + " entries");
			final List<Stuff> untweetedStuff = filterUntweetedAndSortOnTime(stuffs);
			if (!untweetedStuff.isEmpty()) {
				final StringBuilder text = new StringBuilder();
				text.append(callsign);
				text.append(" (");
//				System.out.println("sb is [" + sb.toString() + "] text is [" + text.toString() + "]");
				final int remaining = 140 - (sb.toString().length() + text.toString().length() + 2 /* 2 for the )\n */);
				final String joined = joinWhileStillFitsAndMarkTweeted(remaining, untweetedStuff);
				text.append(joined);
				text.append(")\n");
				sb.append(text.toString());
			} else {
//				System.out.println("callsign " + callsign + " list of untweeted stuff is empty");
			}
		}
		return sb.toString().trim();
	}

	@Override
	public void purge() {
		final long now = sleeper.currentTimeMillis();
		final Set<Entry<Callsign, List<Stuff>>> entries = map.entrySet();
		final Iterator<Entry<Callsign, List<Stuff>>> entryIterator = entries.iterator();
		while (entryIterator.hasNext()) {
			final Entry<Callsign, List<Stuff>> entry = entryIterator.next();
			final Iterator<Stuff> stuffIterator = entry.getValue().iterator();
			while (stuffIterator.hasNext()) {
				final Stuff stuff = stuffIterator.next();
				if (stuff.tweeted == true && stuff.expiryTime < now) {
					LOGGER.info("Purging {}", stuff);
					stuffIterator.remove();
				}
			}
			if (entry.getValue().isEmpty()) {
				LOGGER.info("Purging callsign {}", entry.getKey());
				entryIterator.remove();
			}
		}
	}

	private List<Callsign> sortMapByEntryTime() {
		final List<Callsign> callsigns = new ArrayList<>(map.keySet());
		Collections.sort(callsigns, new Comparator<Callsign>() {
			@Override
			public int compare(final Callsign o1, final Callsign o2) {
				final List<Stuff> o1stuffs = map.get(o1);
				final List<Stuff> o2stuffs = map.get(o2);
				if (o1stuffs.size() == 0 && o2stuffs.size() != 0) {
					return -1;
				}
				if (o1stuffs.size() != 0 && o2stuffs.size() == 0) {
					return 1;
				}
				if (o1stuffs.size() == 0 && o2stuffs.size() == 0) {
					return 0;
				}
				// both lists have contents
				final Stuff o1earliest = earliestStuff(o1stuffs);
				final Stuff o2earliest = earliestStuff(o2stuffs);
				return o1earliest.when.compareTo(o2earliest.when);
			}

			// precondition: stuffs has at least one entry
			private Stuff earliestStuff(final List<Stuff> stuffs) {
				Stuff earliest = stuffs.get(0);
				for (int i = 1; i < stuffs.size(); i++) {
					if (stuffs.get(i).when.before(earliest.when)) {
						earliest = stuffs.get(i);
					}
				}
				return earliest;
			}});
		return callsigns;
	}

	public static String joinWhileStillFitsAndMarkTweeted(final int remaining, final List<Stuff> possiblyUntweetedStuff) {
//		System.out.println("remaining " + remaining);
//		for (final Stuff s : possiblyUntweetedStuff) {
//			System.out.println("possibly untweeted " + s + " tweeted: " + s.tweeted);
//		}
		final List<Stuff> untweetedStuff = filterUntweetedAndSortOnTime(possiblyUntweetedStuff);
		for (int sliceSize=untweetedStuff.size(); sliceSize > 0; sliceSize--) {
			final List<Stuff> subList = untweetedStuff.subList(0, sliceSize);
			final String joined = StringUtils.join(subList, ", ");
//			System.out.println("joined length " + joined.length() + "=" + joined);
			if (joined.length() <= remaining) {
				// all these entries could fit. mark them
//				System.out.println("it fits!");
				for (final Stuff mark: subList) {
//					System.out.println("marking " + mark + " as tweeted");
					mark.tweeted = true;
				}
				return joined;
			} else {
//				System.out.println("exceeded");
			}
		}
		return "";
	}

	private static List<Stuff> filterUntweetedAndSortOnTime(final List<Stuff> entry) {
		final List<Stuff> untweetedStuff = new ArrayList<>();
		for (final Stuff stuff : entry) {
			if (!stuff.tweeted) {
				untweetedStuff.add(stuff);
			}
		}
		Collections.sort(untweetedStuff, new Comparator<Stuff>() {
			@Override
			public int compare(final Stuff o1, final Stuff o2) {
				return o1.when.compareTo(o2.when);
			}});
		return untweetedStuff;
	}
}
