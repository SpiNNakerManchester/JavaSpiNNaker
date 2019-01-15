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
package uk.ac.manchester.spinnaker.front_end.download;

/** The various IDs of messages used in the fast download protocol. */
@Deprecated
public enum ProtocolID {
	/** ID of message used to start sending data. */
	START_SENDING_DATA(100),
	/** ID of message used to start sending missing sequence numbers. */
	START_MISSING_SEQS(1000),
	/** ID of message used to send more missing sequence numbers. */
	NEXT_MISSING_SEQS(1001);
	/** The value of the ID. */
	public final int value;

	ProtocolID(int value) {
		this.value = value;
	}
}
