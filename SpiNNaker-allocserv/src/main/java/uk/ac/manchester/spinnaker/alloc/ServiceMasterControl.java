/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import com.google.errorprone.annotations.concurrent.GuardedBy;

/**
 * Control over general aspects of the service's manageability. Not exposed as
 * part of service because this includes the bits that shut the service off when
 * disabled.
 *
 * @author Donal Fellows
 */
@Service("control")
@ManagedResource("Spalloc:type=ServiceMasterControl,name=control")
public class ServiceMasterControl {
	@GuardedBy("this")
	private boolean paused = false;

	@GuardedBy("this")
	private boolean dummyBMP = false;

	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	/**
	 * Create an instance.
	 *
	 * @param properties
	 *            The service properties.
	 */
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

	/**
	 * @param paused
	 *            Whether periodic tasks should not run.
	 */
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

	/**
	 * @param dummyBMP
	 *            Whether to use dummy transceivers for talking to the BMPs.
	 */
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
