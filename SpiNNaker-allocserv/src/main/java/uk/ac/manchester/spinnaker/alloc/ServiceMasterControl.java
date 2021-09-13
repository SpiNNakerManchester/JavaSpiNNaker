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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.springframework.beans.factory.annotation.Autowired;
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

	private boolean dummyBMP = false;

	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	@Autowired
	public ServiceMasterControl(SpallocProperties properties) {
		this.paused = properties.isPause();
		this.dummyBMP = properties.getTransceiver().isDummy();
	}

	/**
	 * @return Whether periodic tasks should not run.
	 */
	@ManagedAttribute(description = "Whether periodic tasks should not run.")
	public synchronized boolean isPaused() {
		return paused;
	}

	public synchronized void setPaused(boolean paused) {
		boolean old = this.paused;
		this.paused = paused;
		pcs.firePropertyChange("paused", old, paused);
	}

	/**
	 * Add a listener to the {@link #isPaused() paused} property.
	 *
	 * @param listener
	 *            The listener to add.
	 */
	public void addPausedListener(PropertyChangeListener listener) {
		pcs.addPropertyChangeListener("paused", listener);
	}

	/**
	 * Remove a listener from the {@link #isPaused() paused} property.
	 *
	 * @param listener
	 *            The listener to remove.
	 */
	public void removePausedListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener("paused", listener);
	}

	/**
	 * @return Whether to use dummy transceivers for talking to the BMPs.
	 */
	@ManagedAttribute(
			description = "Whether the service actually talks to BMPs; "
					+ "when using a dummy, all actual hardware is ignored.")
	public synchronized boolean isUseDummyBMP() {
		return dummyBMP;
	}

	public synchronized void setUseDummyBMP(boolean dummyBMP) {
		boolean old = this.dummyBMP;
		this.dummyBMP = dummyBMP;
		pcs.firePropertyChange("useDummyBMP", old, dummyBMP);
	}

	/**
	 * Add a listener to the {@link #isUseDummyBMP() useDummyBMP} property.
	 *
	 * @param listener
	 *            The listener to add.
	 */
	public void addUseDummyBMPListener(PropertyChangeListener listener) {
		pcs.addPropertyChangeListener("useDummyBMP", listener);
	}

	/**
	 * Remove a listener from the {@link #isUseDummyBMP() useDummyBMP} property.
	 *
	 * @param listener
	 *            The listener to remove.
	 */
	public void removeUseDummyBMPListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener("useDummyBMP", listener);
	}
}
