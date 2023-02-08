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
package uk.ac.manchester.spinnaker.machine.datalinks;

import java.net.InetAddress;
import java.util.Objects;

import com.google.errorprone.annotations.Immutable;

import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/**
 * A description of a data link that uses the SpiNNaker-link protocol.
 *
 * @author Christian-B
 */
@Immutable
public final class SpinnakerLinkData extends AbstractDataLink {

	/** The link ID from the SpiNNaker perspective. */
	public final int spinnakerLinkId;

	/**
	 * Build an instance.
	 *
	 * @param spinnakerLinkId
	 *            The link ID from the SpiNNaker perspective.
	 * @param location
	 *            The location/Chip being linked to
	 * @param linkId
	 *            The ID/Direction coming out of the Chip
	 * @param boardAddress
	 *            IP address of the Datalink on the board.
	 */
	public SpinnakerLinkData(int spinnakerLinkId, HasChipLocation location,
			Direction linkId, InetAddress boardAddress) {
		super(location, linkId, boardAddress);
		this.spinnakerLinkId = spinnakerLinkId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(location, boardAddress, direction, spinnakerLinkId);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SpinnakerLinkData) {
			var other = (SpinnakerLinkData) obj;
			return sameAs(other) && (spinnakerLinkId == other.spinnakerLinkId);
		}
		return false;
	}

	@Override
	public String toString() {
		return "SpinnakerLinkData {" + "X:" + getX() + " Y:" + getY()
				+ ", boardAddress=" + boardAddress + ", linkId=" + direction
				+ ", SpinnakerLinkId=" + spinnakerLinkId + "}";
	}
}
