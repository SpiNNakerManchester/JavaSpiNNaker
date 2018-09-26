package uk.ac.manchester.spinnaker.spalloc.messages;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import java.util.List;

/**
 * A response that describes what machines have changed state.
 */
public class MachinesChangedNotification implements Notification {
	private List<String> machinesChanged = emptyList();

	public List<String> getMachinesChanged() {
		return machinesChanged;
	}

	public void setMachinesChanged(List<String> machinesChanged) {
		this.machinesChanged = machinesChanged == null ? emptyList()
				: unmodifiableList(machinesChanged);
	}

    @Override
	public String toString() {
        return "Machine Changed " + machinesChanged;
    }
}
