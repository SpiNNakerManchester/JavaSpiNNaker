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
package uk.ac.manchester.spinnaker.alloc.allocator;

import java.sql.Connection;

import uk.ac.manchester.spinnaker.machine.ChipLocation;

public class Job {
	/** Job ID */
	int id;

	/** If not {@code null}, the allocated width of the job's rectangle. */
	Integer width;

	/** If not {@code null}, the allocated height of the job's rectangle. */
	Integer height;

	JobState state;

	/** If not {@code null}, the ID of the root board of the job. */
	Integer root;

	/** The creator of the job. */
	String owner;

	/** Timestamp of last event keeping job alive. (ms from epoch) */
	long keepaliveTime;

	/** Host address that issued last keepalive event, if any. */
	String keepaliveHost;

	Job(Connection conn) {
		// TODO Auto-generated constructor stub
	}

	public void access(String keepaliveAddress) {
		// TODO Auto-generated method stub

	}

	public SubMachine getMachine() {
		// TODO Auto-generated method stub
		return null;
	}

	public void destroy(String reason) {
		// TODO Auto-generated method stub

	}

	public void waitForChange() {
		// TODO Auto-generated method stub

	}

	public int getId() {
		// TODO Auto-generated method stub
		return id;
	}

	public JobState getState() {
		// TODO Auto-generated method stub
		return state;
	}

	public Float getStartTime() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getReason() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getKeepaliveHost() {
		// TODO Auto-generated method stub
		return null;
	}

	public BoardLocation whereIs(int x, int y) {
		// TODO Auto-generated method stub
		return null;
	}

	public ChipLocation getRootChip() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getOwner() {
		// TODO Auto-generated method stub
		return owner;
	}

}
