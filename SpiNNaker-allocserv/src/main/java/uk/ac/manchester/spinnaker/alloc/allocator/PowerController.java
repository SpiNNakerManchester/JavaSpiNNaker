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

/**
 * A service that knows how to initiate the setting of the power state of a job.
 *
 * @author Donal Fellows
 */
public interface PowerController {
	/**
	 * Destroy a job. The power controller has the responsibility because it
	 * releases any resources held by the job.
	 *
	 * @param jobId
	 *            The ID of the job.
	 * @param reason
	 *            Why is the job being destroyed.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	void destroyJob(int jobId, String reason) throws SQLException;

	/**
	 * Issue a power change for a job.
	 *
	 * @param jobId
	 *            The ID of the allocated job.
	 * @param power
	 *            What state to change the job's boards' power to.
	 * @param targetState
	 *            What state are we aiming to put the job into once the power
	 *            has been switched. Should be {@link JobState#READY} or
	 *            {@link JobState#DESTROYED}.
	 * @return Whether any change has been requested.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	boolean setPower(int jobId, PowerState power, JobState targetState)
			throws SQLException;
}
