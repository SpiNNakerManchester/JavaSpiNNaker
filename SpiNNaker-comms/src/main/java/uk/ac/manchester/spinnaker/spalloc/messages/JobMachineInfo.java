package uk.ac.manchester.spinnaker.spalloc.messages;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import java.util.List;

/**
 * A description of a machine associated with a job, in terms of width, height,
 * connections and its name.
 */
public class JobMachineInfo {
	private int width;
	private int height;
	private List<Connection> connections;
	private String machineName;

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public List<Connection> getConnections() {
		return connections;
	}

	public void setConnections(List<Connection> connections) {
		this.connections = connections == null ? emptyList()
				: unmodifiableList(connections);
	}

	public String getMachineName() {
		return machineName;
	}

	public void setMachineName(String machineName) {
		this.machineName = machineName;
	}
}
