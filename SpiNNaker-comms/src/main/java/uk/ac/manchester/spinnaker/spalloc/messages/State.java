/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.spalloc.messages;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.NUMBER;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * The possible states of a spalloc job. In particular, the {@link #READY} state
 * indicates that the job is running/ready to run.
 *
 * @author Donal Fellows
 */
@JsonFormat(shape = NUMBER)
public enum State {
	/** SpallocJob is unknown. */
	UNKNOWN,
	/** SpallocJob is in the queue, awaiting allocation. */
	QUEUED,
	/** SpallocJob is having its boards powered up. */
	POWER,
	/** SpallocJob is running (or at least ready to run). */
	READY,
	/** SpallocJob has terminated, see the {@code reason} property for why. */
	DESTROYED;
}
