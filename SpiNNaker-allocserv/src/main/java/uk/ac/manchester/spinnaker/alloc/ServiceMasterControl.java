/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

/**
 * Control over general aspects of the service's manageability. Not exposed as
 * part of service because this includes the bits that shut the service off when
 * disabled.
 *
 * @author Donal Fellows
 */
@Component("control")
@ManagedResource("Spalloc:type=ServiceMasterControl,name=control")
public class ServiceMasterControl {
	private boolean paused = false;

	/**
	 * @return Whether periodic tasks should not run.
	 */
	@ManagedAttribute(description = "Whether periodic tasks should not run.")
	public synchronized boolean isPaused() {
		return paused;
	}

	public synchronized void setPaused(boolean paused) {
		this.paused = paused;
	}
}
