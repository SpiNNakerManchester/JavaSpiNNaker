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
import java.util.List;

public class JobCollection {
	/** Do not call this constructor outside of tests. */
	JobCollection() {
	}

	JobCollection(Connection conn) {
		// TODO Auto-generated constructor stub
		// DO NOT SAVE THE CONNECTION!
	}

	public void waitForChange() {
		// TODO Auto-generated method stub

	}

	public List<Integer> ids(int start, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

	void addJob(int jobId, int int2, long int3) {
		// TODO Auto-generated method stub

	}

}
