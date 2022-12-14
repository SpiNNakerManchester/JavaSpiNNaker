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
 * Request the state of a job.
 *
 * @see <a href=
 *      "https://spalloc-server.readthedocs.io/en/stable/protocol/#commands.get_job_state"
 *      >Spalloc Server documentation</a>
 * @see JobState The basic result type associated with the request
 */
public final class GetJobStateCommand extends Command<Integer> {
	/**
	 * Create a request to get the state of a job.
	 *
	 * @param jobId
	 *            The job to get the state of.
	 */
	public GetJobStateCommand(int jobId) {
		super("get_job_state");
		addArg(jobId);
	}
}
