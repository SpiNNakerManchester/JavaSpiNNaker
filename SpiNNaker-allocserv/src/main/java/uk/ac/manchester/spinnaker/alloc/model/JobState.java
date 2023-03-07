/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.model;

/** All the possible states that a job may be in. */
public enum JobState {
	/** The job ID requested was not recognised, or is in the initial state. */
	UNKNOWN,
	/** The job is waiting in a queue for a suitable machine. */
	QUEUED,
	/**
	 * The boards allocated to the job are currently being powered on or powered
	 * off.
	 */
	POWER,
	/**
	 * The job has been allocated boards and the boards are not currently
	 * powering on or powering off.
	 */
	READY,
	/** The job has been destroyed. */
	DESTROYED
}
