package uk.ac.manchester.spinnaker.spalloc.messages;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * A response that is the result of listing the machines.
 */
@JsonFormat(shape = ARRAY)
public class ListMachinesResponse {

	private List<Machine> machines;

	public List<Machine> getMachines() {
		return machines;
	}

	public void setMachines(List<Machine> machines) {
		this.machines =
				machines == null ? emptyList() : unmodifiableList(machines);
	}
}
