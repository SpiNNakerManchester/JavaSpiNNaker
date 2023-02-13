/*
 * Copyright (c) 2018-2023 The University of Manchester
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
 * A description of a particular inter-board link.
 *
 * @author Christian-B
 */
@Immutable
public final class FPGALinkData extends AbstractDataLink {
	/** The link ID from the FPGA perspective. */
	public final int fpgaLinkId;

	/** The ID of the FPGA port being used. */
	public final FpgaId fpgaId;

	/**
	 * Build an instance.
	 *
	 * @param fpgaLinkId
	 *            The link ID from the FPGA perspective.
	 * @param fpgaId
	 *            The ID if the FPGA port being used.
	 * @param location
	 *            The location/Chip being linked to
	 * @param linkId
	 *            The ID/Direction coming out of the Chip
	 * @param boardAddress
	 *            IP address of the Datalink on the board.
	 */
	public FPGALinkData(int fpgaLinkId, FpgaId fpgaId, HasChipLocation location,
			Direction linkId, InetAddress boardAddress) {
		super(location, linkId, boardAddress);
		this.fpgaLinkId = fpgaLinkId;
		this.fpgaId = fpgaId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(location, boardAddress, direction, fpgaLinkId,
				fpgaId);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof FPGALinkData) {
			var other = (FPGALinkData) obj;
			return sameAs(other) && (fpgaLinkId == other.fpgaLinkId)
					&& (fpgaId == other.fpgaId);
		}
		return false;
	}

	@Override
	public String toString() {
		return "FPGALinkData {" + "X:" + getX() + " Y:" + getY()
				+ ", boardAddress=" + boardAddress + ", linkId=" + direction
				+ ", fpgaLinkId=" + fpgaLinkId + ", fpgaId=" + fpgaId + "}";
	}
}
