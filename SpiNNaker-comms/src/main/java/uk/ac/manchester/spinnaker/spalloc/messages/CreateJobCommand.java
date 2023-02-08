/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
public class CreateJobCommand extends Command<Integer> {
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
