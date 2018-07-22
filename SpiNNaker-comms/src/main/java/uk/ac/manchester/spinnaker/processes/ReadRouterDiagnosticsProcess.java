package uk.ac.manchester.spinnaker.processes;

import java.io.IOException;

import javax.xml.ws.Holder;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;
import uk.ac.manchester.spinnaker.selectors.ConnectionSelector;

/** A process for reading the diagnostic data block from a SpiNNaker router. */
public class ReadRouterDiagnosticsProcess extends MultiConnectionProcess {
	private static final int REGISTER = 4;
	private static final int NUM_REGISTERS = 16;
	private static final int ROUTER_CONTROL_REGISTER = 0xe1000000;
	private static final int ROUTER_ERROR_STATUS = 0xe1000014;
	private static final int ROUTER_REGISTERS = 0xe1000300;

	public ReadRouterDiagnosticsProcess(ConnectionSelector connectionSelector) {
		super(connectionSelector);
	}

	public RouterDiagnostics getRouterDiagnostics(HasChipLocation chip)
			throws IOException, Exception {
		Holder<Integer> cr = new Holder<>();
		Holder<Integer> es = new Holder<>();
		int reg[] = new int[NUM_REGISTERS];

		sendRequest(new ReadMemory(chip, ROUTER_CONTROL_REGISTER, REGISTER),
				response -> cr.value = response.data.getInt());
		sendRequest(new ReadMemory(chip, ROUTER_ERROR_STATUS, REGISTER),
				response -> es.value = response.data.getInt());
		sendRequest(
				new ReadMemory(chip, ROUTER_REGISTERS,
						NUM_REGISTERS * REGISTER),
				response -> response.data.asIntBuffer().get(reg));

		finish();
		checkForError();
		return new RouterDiagnostics(cr.value, es.value, reg);
	}
}
