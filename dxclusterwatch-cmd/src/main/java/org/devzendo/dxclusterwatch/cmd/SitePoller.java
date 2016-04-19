package org.devzendo.dxclusterwatch.cmd;

public interface SitePoller {
	ClusterRecord[] poll();
}