package org.vadere.simulator.projects.dataprocessing_mtp;

public class PedestrianIdOutputFile extends OutputFile<PedestrianIdDataKey> {

	public PedestrianIdOutputFile() {
		this.setKeyHeader(PedestrianIdDataKey.getHeader());
	}

}