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

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface SpallocInterface {

	Map<String, Machine> getMachines() throws SQLException;

	Machine getMachine(String name) throws SQLException;

	JobCollection getJobs(boolean deleted, int limit, int start)
			throws SQLException;

	Job getJob(int id) throws SQLException;

	Job createJob(String owner, List<Integer> dimensions, String machineName,
			List<String> tags, Integer maxDeadBoards) throws SQLException;

}
