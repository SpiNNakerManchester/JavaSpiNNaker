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

import org.springframework.stereotype.Component;

/**
 * Manages user quotas.
 *
 * @author Donal Fellows
 */
@Component
public class QuotaManager {
	// TODO implement a real policy; the current one is... not enough

	/**
	 * Can the user create another job at this point? If not, they're currently
	 * out of resources.
	 *
	 * @param machineId
	 *            On what machine do they want to create the job? Quotas are
	 *            theoretically per-machine.
	 * @param user
	 *            Who wants to create the job.
	 * @return True if they can make a job. False if they can't.
	 * @throws SQLException
	 *             If database access fails.
	 */
	public boolean hasQuotaRemaining(int machineId, String user)
			throws SQLException {
		return true;
	}

	/**
	 * Has the execution of a job exceeded its owner's resource allocation at
	 * this point?
	 *
	 * @param machineId
	 *            On what machine is the job running? Quotas are theoretically
	 *            per-machine.
	 * @param jobId
	 *            What job is consuming resources?
	 * @return True if the job can continue to run. False if it can't.
	 * @throws SQLException
	 *             If database access fails.
	 */
	public boolean hasQuotaRemaining(int machineId, int jobId)
			throws SQLException {
		return true;
	}
}
