package org.devzendo.dxclusterwatch.cmd;

import java.sql.Timestamp;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/*
    {
        "band": "20",
        "call": "IS0GGA",
        "comment": "tnx for dx.73 from sardinia.",
        "dx_cont": "EU",
        "dx_cqz": "16",
        "dx_ituz": "0",
        "dx_lat": "55.8",
        "dx_long": "37.6",
        "dx_name": "EUROPEAN RUSSIA",
        "dx_prefix": "UA",
        "dxcall": "UA5D",
        "freq": "14217",
        "mytime": "18/04/16 at 07:01",
        "nr": "14689356",
        "spotter_cont": "EU",
        "spotter_cqz": "15",
        "spotter_ituz": "0",
        "spotter_lat": "39.2",
        "spotter_long": "9.1",
        "spotter_name": "SARDINIA",
        "spotter_prefix": "IS0",
        "time": "2016-04-18 07:01:00"
    },
 */

public class ClusterRecord {
	private String band;
	private String call;           // persisted
	private String comment;        // persisted
	private String dx_cont;
	private String dx_cqz;
	private String dx_ituz;
	private String dx_lat;
	private String dx_long;
	private String dx_name;
	private String dx_prefix;
	private String dxcall;         // persisted
	private String freq;           // persisted
	private String mytime;
	private String nr;             // persisted
	private String spotter_cont;
	private String spotter_cqz;
	private String spotter_ituz;
	private String spotter_lat;
	private String spotter_long;
	private String spotter_name;
	private String spotter_prefix;
	private String time;           // persisted
	
	public String getMytime() {
		return mytime;
	}
	public void setMytime(final String mytime) {
		this.mytime = mytime;
	}
	
	public String getDx_cont() {
		return dx_cont;
	}
	public void setDx_cont(final String dx_cont) {
		this.dx_cont = dx_cont;
	}
	public String getDx_cqz() {
		return dx_cqz;
	}
	public void setDx_cqz(final String dx_cqz) {
		this.dx_cqz = dx_cqz;
	}
	public String getDx_ituz() {
		return dx_ituz;
	}
	public void setDx_ituz(final String dx_ituz) {
		this.dx_ituz = dx_ituz;
	}
	public String getDx_lat() {
		return dx_lat;
	}
	public void setDx_lat(final String dx_lat) {
		this.dx_lat = dx_lat;
	}
	public String getDx_long() {
		return dx_long;
	}
	public void setDx_long(final String dx_long) {
		this.dx_long = dx_long;
	}
	public String getDx_name() {
		return dx_name;
	}
	public void setDx_name(final String dx_name) {
		this.dx_name = dx_name;
	}
	public String getDx_prefix() {
		return dx_prefix;
	}
	public void setDx_prefix(final String dx_prefix) {
		this.dx_prefix = dx_prefix;
	}
	public String getSpotter_cont() {
		return spotter_cont;
	}
	public void setSpotter_cont(final String spotter_cont) {
		this.spotter_cont = spotter_cont;
	}
	public String getSpotter_cqz() {
		return spotter_cqz;
	}
	public void setSpotter_cqz(final String spotter_cqz) {
		this.spotter_cqz = spotter_cqz;
	}
	public String getSpotter_ituz() {
		return spotter_ituz;
	}
	public void setSpotter_ituz(final String spotter_ituz) {
		this.spotter_ituz = spotter_ituz;
	}
	public String getSpotter_lat() {
		return spotter_lat;
	}
	public void setSpotter_lat(final String spotter_lat) {
		this.spotter_lat = spotter_lat;
	}
	public String getSpotter_long() {
		return spotter_long;
	}
	public void setSpotter_long(final String spotter_long) {
		this.spotter_long = spotter_long;
	}
	public String getSpotter_name() {
		return spotter_name;
	}
	public void setSpotter_name(final String spotter_name) {
		this.spotter_name = spotter_name;
	}
	public String getSpotter_prefix() {
		return spotter_prefix;
	}
	public void setSpotter_prefix(final String spotter_prefix) {
		this.spotter_prefix = spotter_prefix;
	}
	public String getNr() {
		return nr;
	}
	public void setNr(final String nr) {
		this.nr = nr;
	}
	public String getBand() {
		return band;
	}
	public void setBand(final String band) {
		this.band = band;
	}
	public String getCall() {
		return call;
	}
	public void setCall(final String call) {
		this.call = call;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(final String comment) {
		this.comment = comment;
	}
	public String getDxcall() {
		return dxcall;
	}
	public void setDxcall(final String dxcall) {
		this.dxcall = dxcall;
	}
	public String getFreq() {
		return freq;
	}
	public void setFreq(final String freq) {
		this.freq = freq;
	}
	public String getTime() {
		return time;
	}
	public void setTime(final String time) {
		this.time = time;
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.NO_CLASS_NAME_STYLE);
	}
	
	public static ClusterRecord dbRecord(final int nr, final String dxcall, final String call, final Timestamp when, final String freq, final String comment) {
		final ClusterRecord r = new ClusterRecord();
		r.setNr("" + nr);
		r.setDxcall(StringUtils.defaultString(dxcall));
		r.setCall(StringUtils.defaultString(call));
		r.setTime(StringUtils.defaultString(when.toString()));
		r.setFreq(StringUtils.defaultString(freq));
		r.setComment(StringUtils.defaultString(comment));
		return r;
	}
	
	public String toDbString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append("nr=");
		sb.append(getNr());
		sb.append(",dxcall=");
		sb.append(getDxcall());
		sb.append(",call=");
		sb.append(getCall());
		sb.append(",when=");
		sb.append(getTime());
		sb.append(",freq=");
		sb.append(getFreq());
		sb.append(",comment=");
		sb.append(getComment());
		sb.append("]");
		return sb.toString();
	}
}
