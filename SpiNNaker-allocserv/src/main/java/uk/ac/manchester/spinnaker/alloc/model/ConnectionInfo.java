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
package uk.ac.manchester.spinnaker.alloc.model;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import static java.util.Objects.nonNull;

import java.util.Objects;
import uk.ac.manchester.spinnaker.machine.ChipLocation;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/**
 * Describes a connection by its chip and hostname.
 */
@JsonPropertyOrder({
	"chip", "hostname"
})
@JsonFormat(shape = ARRAY)
/*
 * Duplicated from uk.ac.manchester.spinnaker.spalloc.messages.Connection
 * because that name was too confusing with SQL about!
 */
public final class ConnectionInfo {
	private ChipLocation chip;

	private String hostname;

	/**
	 * Create with defaults.
	 */
	public ConnectionInfo() {
		chip = null;
		hostname = "";
	}

	/**
	 * Create.
	 *
	 * @param chip
	 *            the chip
	 * @param hostname
	 *            the host
	 */
	public ConnectionInfo(ChipLocation chip, String hostname) {
		this.chip = chip;
		this.hostname = hostname;
	}

	/**
	 * Create.
	 *
	 * @param chip
	 *            the chip
	 * @param hostname
	 *            the host
	 */
	public ConnectionInfo(HasChipLocation chip, String hostname) {
		this.chip = chip.asChipLocation();
		this.hostname = hostname;
	}

	public ChipLocation getChip() {
		return chip;
	}

	/**
	 * Set the chip for the connection.
	 *
	 * @param chip
	 *            The chip to set.
	 */
	public void setChip(ChipLocation chip) {
		this.chip = chip;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ConnectionInfo) {
			ConnectionInfo c = (ConnectionInfo) other;
			return Objects.equals(chip, c.chip)
					&& Objects.equals(hostname, c.hostname);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hashcode = 0;
		if (nonNull(hostname)) {
			hashcode += 5 * hostname.hashCode();
		}
		if (nonNull(chip)) {
			hashcode += 7 * chip.hashCode();
		}
		return hashcode;
	}

	@Override
	public String toString() {
		return "Connection(" + chip + "@" + hostname + ")";
	}
}
