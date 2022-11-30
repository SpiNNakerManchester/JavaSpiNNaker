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

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;
import static java.util.Objects.isNull;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Immutable;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.utils.validation.IPAddress;

/**
 * Describes a connection by its chip and hostname.
 *
 * @param chip
 *            The chip for the connection.
 * @param hostname
 *            Where to connect to to talk to the chip.
 */
@JsonPropertyOrder({ "chip", "hostname" })
@JsonFormat(shape = ARRAY)
@JsonDeserialize(builder = Connection.Builder.class)
@Immutable
public final record Connection(@Valid ChipLocation chip,
		@IPAddress String hostname) {
	/**
	 * Create.
	 *
	 * @param chip
	 *            the chip
	 * @param hostname
	 *            the host
	 */
	public Connection(HasChipLocation chip, String hostname) {
		this(isNull(chip) ? null : chip.asChipLocation(), hostname);
	}

	@Override
	public String toString() {
		return "Connection(" + chip + "@" + hostname + ")";
	}

	@JsonPropertyOrder({ "chip", "hostname" })
	@JsonFormat(shape = ARRAY)
	@JsonPOJOBuilder
	static class Builder {
		private ChipLocation chip;

		private String hostname;

		@CanIgnoreReturnValue
		public Builder withChip(ChipLocation chip) {
			this.chip = chip;
			return this;
		}

		@CanIgnoreReturnValue
		public Builder withHostname(String hostname) {
			this.hostname = hostname;
			return this;
		}

		public Connection build() {
			return new Connection(chip, hostname);
		}
	}
}
