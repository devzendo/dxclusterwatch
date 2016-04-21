package org.devzendo.dxclusterwatch.cmd;

public interface PageBuilder {
	void rebuildPage(int retrievedRecords, int newRecords);
	void publishPage();
}
