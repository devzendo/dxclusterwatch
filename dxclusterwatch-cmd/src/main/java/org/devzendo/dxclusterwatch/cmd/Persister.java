package org.devzendo.dxclusterwatch.cmd;

public interface Persister {

	void persistRecords(ClusterRecord[] records);

}
