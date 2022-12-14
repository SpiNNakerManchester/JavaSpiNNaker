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

import java.util.List;
import java.util.Map;

/**
 * Request to create a job.
 *
 * @see <a href=
 *      "https://spalloc-server.readthedocs.io/en/stable/protocol/#commands.create_job"
 *      >Spalloc Server documentation</a>
 */
public final class CreateJobCommand extends Command<Integer> {
	/**
	 * Create a request to create a job. Short-hand form for most basic kind of
	 * request.
	 *
	 * @param numBoards
	 *            The number of boards to request.
	 * @param owner
	 *            The owner of the job to create.
	 */
	public CreateJobCommand(int numBoards, String owner) {
		super("create_job");
		addArg(numBoards);
		addKwArg("owner", owner);
	}

	/**
	 * Create a request to create a job.
	 *
	 * @param args
	 *            The arguments, describing default (empty), the number of
	 *            boards (one arg), the triad size (two args) or the board
	 *            location (three args).
	 * @param kwargs
	 *            Additional arguments required. Must include the key
	 *            {@code owner}. Values can be boxed primitive types or strings.
	 * @throws IllegalArgumentException
	 *             if the {@code owner} key is missing
	 */
	public CreateJobCommand(List<Integer> args, Map<String, Object> kwargs) {
		super("create_job");
		for (int i : args) {
			addArg(i);
		}
		if (!kwargs.containsKey("owner")) {
			throw new IllegalArgumentException(
					"owner must be specified for all jobs");
		}
		for (String key : kwargs.keySet()) {
			addKwArg(key, kwargs.get(key));
		}
	}
}
