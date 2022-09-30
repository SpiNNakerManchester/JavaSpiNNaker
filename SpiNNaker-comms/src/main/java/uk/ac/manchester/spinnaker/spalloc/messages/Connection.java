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

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;
import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;
import static java.util.Objects.isNull;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/**
 * Describes a connection by its chip and hostname.
 */
@JsonPropertyOrder({
	"chip", "hostname"
})
@JsonFormat(shape = ARRAY)
@JsonAutoDetect(setterVisibility = NON_PRIVATE)
public final class Connection {
	private ChipLocation chip;

	private String hostname;

	/**
	 * Create with defaults.
	 */
	public Connection() {
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
	public Connection(HasChipLocation chip, String hostname) {
		this.chip = isNull(chip) ? null : chip.asChipLocation();
		this.hostname = hostname;
	}

	/** @return The chip for the connection. */
	public ChipLocation getChip() {
		return chip;
	}

	void setChip(ChipLocation chip) {
		this.chip = chip;
	}

	/** @return Where to connect to to talk to the chip. */
	public String getHostname() {
		return hostname;
	}

	void setHostname(String hostname) {
		this.hostname = hostname;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Connection) {
			var c = (Connection) other;
			return Objects.equals(chip, c.chip)
					&& Objects.equals(hostname, c.hostname);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(hostname, chip);
	}

	@Override
	public String toString() {
		return "Connection(" + chip + "@" + hostname + ")";
	}
}
