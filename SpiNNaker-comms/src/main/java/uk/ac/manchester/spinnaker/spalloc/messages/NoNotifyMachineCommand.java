/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.spalloc.messages;

/**
 * Request to not receive notifications about a machine.
 *
 * @see <a href=
 *      "https://spalloc-server.readthedocs.io/en/stable/protocol/#commands.no_notify_machine"
 *      >Spalloc Server documentation</a>
 */
public final class NoNotifyMachineCommand extends Command<String> {
	//
	/**
	 * Create a request to not be notified of changes in machine state.
	 *
	 * @param machineName
	 *            The machine to request about.
	 */
	public NoNotifyMachineCommand(String machineName) {
		super("no_notify_machine");
		addArg(machineName);
	}

	/**
	 * Create a request to not be notified of changes in all machines' state.
	 */
	public NoNotifyMachineCommand() {
		super("no_notify_machine");
	}
}
