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

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.errorprone.annotations.Immutable;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.spalloc.messages.Connection;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;
import uk.ac.manchester.spinnaker.utils.validation.IPAddress;

/**
 * Describes a connection by its chip and hostname.
 *
 * @see Connection
 * @param chip
 *            The chip for the connection.
 * @param hostname
 *            Where to connect to to talk to the chip.
 */
@JsonPropertyOrder({ "chip", "hostname" })
@JsonFormat(shape = ARRAY)
@JsonAutoDetect(setterVisibility = NON_PRIVATE)
/*
 * Duplicated from uk.ac.manchester.spinnaker.spalloc.messages.Connection
 * because that name was too confusing with SQL about!
 */
@Immutable
@UsedInJavadocOnly(Connection.class)
public record ConnectionInfo(@Valid ChipLocation chip,
		@IPAddress String hostname) {
	@Override
	public String toString() {
		return "Connection(" + chip + "@" + hostname + ")";
	}
}
