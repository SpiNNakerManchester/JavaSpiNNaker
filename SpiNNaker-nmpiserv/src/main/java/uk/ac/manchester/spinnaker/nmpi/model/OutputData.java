/*
 * Copyright (c) 2014 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.nmpi.model;

import java.util.List;

import uk.ac.manchester.spinnaker.nmpi.model.job.nmpi.DataItem;

/**
 * The output data from a Job.
 */
public class OutputData {
	/** The repository. */
	private String repository;

	/** The files of the repository. */
	private List<DataItem> files;

	/**
	 * Creates an empty item of data.
	 */
	public OutputData() {
		// Does Nothing
	}

	/**
	 * Make an instance for a repository.
	 *
	 * @param repository
	 *            The name of the repository.
	 */
	public OutputData(final String repository) {
		this.repository = repository;
	}

	/**
	 * Get the repository.
	 *
	 * @return The repository.
	 */
	public String getRepository() {
		return repository;
	}

	/**
	 * Set the repository.
	 *
	 * @param repository
	 *            The repository to set.
	 */
	public void setRepository(String repository) {
		this.repository = repository;
	}

	/**
	 * Get the files.
	 *
	 * @return The files.
	 */
	public List<DataItem> getFiles() {
		return files;
	}

	/**
	 * Set the files.
	 *
	 * @param files
	 *            The files to set.
	 */
	public void setFiles(List<DataItem> files) {
		this.files = files;
	}
}
