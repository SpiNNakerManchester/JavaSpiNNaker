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
 * Request to get notifications about a job.
 */
public class NotifyJobCommand extends Command<Integer> {
	/**
	 * Create a request to be notified of changes in job state.
	 *
	 * @param jobId
	 *            The job to request about.
	 */
	public NotifyJobCommand(int jobId) {
		super("notify_job");
		addArg(jobId);
	}

	/**
	 * Create a request to be notified of changes in all jobs' state.
	 */
	public NotifyJobCommand() {
		super("notify_job");
	}
}
