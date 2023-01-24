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

import static uk.ac.manchester.spinnaker.messages.scp.RouterTableCommand.LOAD_APPLICATION_ROUTES;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * An SDP Request to load the previously-saved application multicast router
 * table.
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
