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

/**
 * Request to destroy a job.
 *
 * @see <a href=
 *      "https://spalloc-server.readthedocs.io/en/stable/protocol/#commands.destroy_job"
 *      >Spalloc Server documentation</a>
 */
public final class DestroyJobCommand extends Command<Integer> {
	/**
	 * Make a request to destroy a job.
	 *
	 * @param jobId
	 *            The ID of the job.
	 */
	public DestroyJobCommand(int jobId) {
		super("destroy_job");
		addArg(jobId);
	}

	/**
	 * Make a request to destroy a job.
	 *
	 * @param jobId
	 *            The ID of the job.
	 * @param reason
	 *            Why the job is to be destroyed.
	 */
	public DestroyJobCommand(int jobId, String reason) {
		super("destroy_job");
		addArg(jobId);
		if (reason != null) {
			addKwArg("reason", reason);
		}
	}
}
