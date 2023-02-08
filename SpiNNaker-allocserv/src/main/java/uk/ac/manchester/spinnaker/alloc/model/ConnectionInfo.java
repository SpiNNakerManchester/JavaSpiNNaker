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
