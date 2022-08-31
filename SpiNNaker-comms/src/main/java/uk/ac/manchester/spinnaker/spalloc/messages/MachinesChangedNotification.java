/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.spalloc.messages;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.isNull;

import java.util.List;

/**
 * A response that describes what machines have changed state.
 */
public final class MachinesChangedNotification implements Notification {
	private List<String> machinesChanged = List.of();

	/** @return What machines have changed. */
	public List<String> getMachinesChanged() {
		return machinesChanged;
	}

	/** @param machinesChanged What machines have changed. */
	public void setMachinesChanged(List<String> machinesChanged) {
		this.machinesChanged = isNull(machinesChanged) ? List.of()
				: unmodifiableList(machinesChanged);
	}

	@Override
	public String toString() {
		return "Machine Changed " + machinesChanged;
	}
}
