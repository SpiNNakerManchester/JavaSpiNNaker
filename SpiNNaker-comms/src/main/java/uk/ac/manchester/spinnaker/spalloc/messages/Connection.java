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
package uk.ac.manchester.spinnaker.spalloc.messages;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;
import static java.util.Objects.isNull;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Immutable;

import jakarta.validation.Valid;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.utils.validation.IPAddress;

/**
 * Describes a connection by its chip and hostname.
 */
@JsonPropertyOrder({
	"chip", "hostname"
})
@JsonFormat(shape = ARRAY)
@JsonDeserialize(builder = Connection.Builder.class)
@Immutable
public final class Connection {
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
	public Connection(HasChipLocation chip, String hostname) {
		this.chip = isNull(chip) ? null : chip.asChipLocation();
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

	@JsonPropertyOrder({
		"chip", "hostname"
	})
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
