/*
 * Copyright (c) 2022 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag.REPLY_EXPECTED;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPPort.GATHERER_DATA_SPEED_UP;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/**
 * Represents an abstract SCP Request that has no payload data in its response.
 */
public abstract class SimpleRequest extends SCPRequest<CheckOKResponse> {
	private String op;

	private final Enum<?> cmd;

	/**
	 * Create a new request that goes to the default port and needs a reply.
	 *
	 * @param operation
	 *            The higher-level operation that was being done.
	 * @param core
	 *            The core to send the request to.
	 * @param command
	 *            The command ID.
	 */
	protected SimpleRequest(String operation, HasCoreLocation core,
			SCPCommand command) {
		super(core, command);
		cmd = command;
		op = operation;
	}

	/**
	 * Create a new request that goes to the default port and needs a reply.
	 *
	 * @param operation
	 *            The higher-level operation that was being done.
	 * @param core
	 *            The core to send the request to.
	 * @param command
	 *            The command ID.
	 * @param argument1
	 *            The first argument.
	 */
	protected SimpleRequest(String operation, HasCoreLocation core,
			SCPCommand command, int argument1) {
		super(core, command, argument1);
		cmd = command;
		op = operation;
	}

	/**
	 * Create a new request that goes to the default port and needs a reply.
	 *
	 * @param operation
	 *            The higher-level operation that was being done.
	 * @param core
	 *            The core to send the request to.
	 * @param command
	 *            The command ID.
	 * @param argument1
	 *            The first argument.
	 * @param argument2
	 *            The second argument.
	 */
	protected SimpleRequest(String operation, HasCoreLocation core,
			SCPCommand command, int argument1, int argument2) {
		super(core, command, argument1, argument2);
		cmd = command;
		op = operation;
	}

	/**
	 * Create a new request that goes to the default port and needs a reply.
	 *
	 * @param operation
	 *            The higher-level operation that was being done.
	 * @param core
	 *            The core to send the request to.
	 * @param command
	 *            The command ID.
	 * @param argument1
	 *            The first argument.
	 * @param argument2
	 *            The second argument.
	 * @param argument3
	 *            The third argument.
	 */
	protected SimpleRequest(String operation, HasCoreLocation core,
			SCPCommand command, int argument1, int argument2, int argument3) {
		super(core, command, argument1, argument2, argument3);
		cmd = command;
		op = operation;
	}

	/**
	 * Create a new request that goes to the default port and needs a reply.
	 *
	 * @param operation
	 *            The higher-level operation that was being done.
	 * @param core
	 *            The core to send the request to.
	 * @param command
	 *            The command ID.
	 * @param argument1
	 *            The first argument.
	 * @param argument2
	 *            The second argument.
	 * @param argument3
	 *            The third argument.
	 * @param data
	 *            The additional data. Starts at the <i>position</i> and goes to
	 *            the <i>limit</i>.
	 */
	protected SimpleRequest(String operation, HasCoreLocation core,
			SCPCommand command, int argument1, int argument2, int argument3,
			ByteBuffer data) {
		super(core, command, argument1, argument2, argument3, data);
		cmd = command;
		op = operation;
	}

	/**
	 * Create a new request that goes to the reinjector port of an extra monitor
	 * and needs a reply.
	 *
	 * @param operation
	 *            The higher-level operation that was being done.
	 * @param core
	 *            The extra monitor core to send the request to.
	 * @param command
	 *            The command ID.
	 * @param argument1
	 *            The first argument.
	 * @param argument2
	 *            The second argument.
	 * @param argument3
	 *            The third argument.
	 * @param data
	 *            The additional data. Starts at the <i>position</i> and goes to
	 *            the <i>limit</i>.
	 */
	protected SimpleRequest(String operation, HasCoreLocation core,
			ReinjectorCommand command, int argument1, int argument2,
			int argument3, ByteBuffer data) {
		super(new ReinjectionSDPHeader(core), command, argument1, argument2,
				argument3, data);
		cmd = command;
		op = operation;
	}

	/**
	 * Create a new request that goes to the router table control port of an
	 * extra monitor and needs a reply.
	 *
	 * @param operation
	 *            The higher-level operation that was being done.
	 * @param core
	 *            The extra monitor core to send the request to.
	 * @param command
	 *            The command ID.
	 * @param argument1
	 *            The first argument.
	 * @param argument2
	 *            The second argument.
	 * @param argument3
	 *            The third argument.
	 * @param data
	 *            The additional data. Starts at the <i>position</i> and goes to
	 *            the <i>limit</i>.
	 */
	protected SimpleRequest(String operation, HasCoreLocation core,
			RouterTableCommand command, int argument1, int argument2,
			int argument3, ByteBuffer data) {
		super(new SDPHeader(REPLY_EXPECTED, core, GATHERER_DATA_SPEED_UP),
				command, argument1, argument2, argument3, data);
		cmd = command;
		op = operation;
	}

	/**
	 * Create a new request that goes to the Spin1API control port of an
	 * application core and needs a reply.
	 *
	 * @param operation
	 *            The higher-level operation that was being done.
	 * @param core
	 *            The application core to send the request to.
	 * @param command
	 *            The command ID.
	 * @param argument1
	 *            The first argument.
	 * @param argument2
	 *            The second argument.
	 * @param argument3
	 *            The third argument.
	 * @param data
	 *            The additional data. Starts at the <i>position</i> and goes to
	 *            the <i>limit</i>.
	 */
	protected SimpleRequest(String operation, HasCoreLocation core,
			RunningCommand command, int argument1, int argument2,
			int argument3, ByteBuffer data) {
		super(new RunningSDPHeader(core, true), command, argument1, argument2,
				argument3, data);
		cmd = command;
		op = operation;
	}

	@Override
	public final CheckOKResponse getSCPResponse(ByteBuffer buffer)
			throws Exception {
		return new CheckOKResponse(op, cmd, buffer);
	}
}
