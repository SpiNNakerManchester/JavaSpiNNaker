/*
 * Copyright (c) 2019 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.scp.RouterTableCommand.LOAD_APPLICATION_ROUTES;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * A request to load the previously-saved application multicast router
 * table. There is no response payload.
 * <p>
 * Handled by {@code data_in_speed_up_load_in_application_routes()} in
 * {@code extra_monitor_support.c}.
 */
public final class LoadApplicationRoutes extends RouterTableRequest {
	/**
	 * @param core
	 *            The coordinates of the monitor core.
	 */
	public LoadApplicationRoutes(HasCoreLocation core) {
		super(core, LOAD_APPLICATION_ROUTES);
	}

	@Override
	String describe() {
		return "Load application multicast routes";
	}
}
