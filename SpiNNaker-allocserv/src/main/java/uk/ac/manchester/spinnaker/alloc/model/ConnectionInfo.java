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

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;
import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;

import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.errorprone.annotations.Immutable;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.spalloc.messages.Connection;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;
import uk.ac.manchester.spinnaker.utils.validation.IPAddress;

/**
 * Describes a connection by its chip and hostname.
 *
 * @see Connection
 */
@JsonPropertyOrder({
	"chip", "hostname"
})
@JsonFormat(shape = ARRAY)
@JsonAutoDetect(setterVisibility = NON_PRIVATE)
/*
 * Duplicated from uk.ac.manchester.spinnaker.spalloc.messages.Connection
 * because that name was too confusing with SQL about!
 */
@Immutable
@UsedInJavadocOnly(Connection.class)
public final class ConnectionInfo {
	@Valid
	private final ChipLocation chip;

	@IPAddress
	private final String hostname;

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

	/** @return The chip for the connection. */
	public ChipLocation getChip() {
		return chip;
	}

	/** @return Where to connect to to talk to the chip. */
	public String getHostname() {
		return hostname;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ConnectionInfo) {
			var c = (ConnectionInfo) other;
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
