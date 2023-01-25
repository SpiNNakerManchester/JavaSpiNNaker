/*
 * Copyright (c) 2019 The University of Manchester
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

import static uk.ac.manchester.spinnaker.messages.scp.RouterTableCommand.LOAD_SYSTEM_ROUTES;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * An SDP Request to load the previously-configured system multicast router
 * table.
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
