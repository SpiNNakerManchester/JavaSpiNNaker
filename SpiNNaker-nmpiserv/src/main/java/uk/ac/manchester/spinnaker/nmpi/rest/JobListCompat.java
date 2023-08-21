/*
 * Copyright (c) 2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.nmpi.rest;

import java.util.List;

import uk.ac.manchester.spinnaker.nmpi.model.QueueJobCompat;

/**
 * A list of Jobs.
 */
public class JobListCompat {
	private List<QueueJobCompat> objects;

	/**
	 * @return the jobs
	 */
	public List<QueueJobCompat> getObjects() {
		return objects;
	}

	/**
	 * @param objects the jobs to set
	 */
	public void setObjects(List<QueueJobCompat> objects) {
		this.objects = objects;
	}
}
