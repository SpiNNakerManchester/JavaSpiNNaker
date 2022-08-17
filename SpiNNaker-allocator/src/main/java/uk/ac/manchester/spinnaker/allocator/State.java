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
package uk.ac.manchester.spinnaker.allocator;

/** States that a job might be in. */
public enum State {
	/**
	 * The job ID requested was not recognised, or is in the initial state.
	 */
	UNKNOWN,
	/** The job is waiting in a queue for a suitable machine. */
	QUEUED,
	/**
	 * The boards allocated to the job are currently being powered on or
	 * powered off.
	 */
	POWER,
	/**
	 * The job has been allocated boards and the boards are not currently
	 * powering on or powering off.
	 */
	READY,
	/** The job has been destroyed. */
	DESTROYED
}
