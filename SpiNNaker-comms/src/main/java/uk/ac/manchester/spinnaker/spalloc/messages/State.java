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
