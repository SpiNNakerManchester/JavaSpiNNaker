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
package uk.ac.manchester.spinnaker.messages.scp;

import com.google.errorprone.annotations.concurrent.GuardedBy;

/** Where to get sequence numbers from. */
public abstract class SequenceNumberSource {
	/** The number of items in the sequence. */
	public static final int SEQUENCE_LENGTH = 65536;

	private SequenceNumberSource() {
	}

	/** Keep a global track of the sequence numbers used. */
	@GuardedBy("SequenceNumberSource.class")
	private static int nextSequence = 0;

	/**
	 * Get the next number from the global sequence, applying appropriate
	 * wrapping rules as the sequence numbers have a fixed number of bits.
	 *
	 * @return the next sequence number; these loop between 0 and 65535
	 *         (unsigned).
	 */
	static synchronized short getNextSequenceNumber() {
		int seq = nextSequence;
		nextSequence = (nextSequence + 1) % SEQUENCE_LENGTH;
		return (short) seq;
	}
}
