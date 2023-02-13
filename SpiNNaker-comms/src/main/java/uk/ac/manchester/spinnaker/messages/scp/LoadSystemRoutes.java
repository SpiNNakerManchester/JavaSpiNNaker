/*
 * Copyright (c) 2018-2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.RouterTableCommand.LOAD_SYSTEM_ROUTES;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * A request to load the previously-configured system multicast router
 * table. There is no response payload.
 * <p>
 * Handled by {@code data_in_speed_up_load_in_system_tables()} in
 * {@code extra_monitor_support.c}.
 */
public final class LoadSystemRoutes extends RouterTableRequest {
	/**
	 * @param core
	 *            The coordinates of the monitor core.
	 */
	public LoadSystemRoutes(HasCoreLocation core) {
		super(core, LOAD_SYSTEM_ROUTES);
	}

	@Override
	String describe() {
		return "Load system multicast routes";
	}
}
