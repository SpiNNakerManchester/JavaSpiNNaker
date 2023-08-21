/*
 * Copyright (c) 2014 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.nmpi.model.machine;

import static java.lang.Integer.parseInt;
import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsFirst;
import static java.util.Objects.nonNull;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

/**
 * Represents a SpiNNaker machine on which jobs can be executed.
 */
public class SpinnakerMachine
		implements Serializable, Comparable<SpinnakerMachine> {

	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = -2247744763327978524L;

	/**
	 * The number of parts that make up a machine description as a string.
	 */
	private static final int N_PARTS = 6;

	/**
	 * Part of the string that is the name of the machine.
	 */
	private static final int MACHINE_NAME_PART = 0;

	/**
	 * Part of the string that is the version of the machine.
	 */
	private static final int VERSION_PART = 1;

	/**
	 * Part of the string that is the width of the machine.
	 */
	private static final int WIDTH_PART = 2;

	/**
	 * Part of the string that is the height of the machine.
	 */
	private static final int HEIGHT_PART = 3;

	/**
	 * Part of the string that is the number of boards in the machine.
	 */
	private static final int N_BOARDS_PART = 4;

	/**
	 * Part of the string that is the BMP details of the machine.
	 */
	private static final int BMP_DETAILS_PART = 5;

	/**
	 * The name of the machine.
	 */
	private String machineName = null;

	/**
	 * The version of the machine.
	 */
	private String version = null;

	/**
	 * The width of the machine.
	 */
	private int width = 0;

	/**
	 * The height of the machine.
	 */
	private int height = 0;

	/**
	 * The number of boards in the machine.
	 */
	private int nBoards = 0;

	/**
	 * The BMP details of the machine.
	 */
	private String bmpDetails = null;

	/**
	 * Creates an empty machine.
	 */
	public SpinnakerMachine() {
		// Does Nothing
	}

	/**
	 * Creates a new Spinnaker Machine by parsing the name of a machine.
	 *
	 * @param value
	 *            The name of the machine to parse.
	 * @return The parsed machine descriptor.
	 * @throws IllegalArgumentException
	 *             if the description has the wrong overall format
	 * @throws NumberFormatException
	 *             if one of the parts that should be numeric isn't
	 */
	public static SpinnakerMachine parse(final String value) {
		if (!value.startsWith("(") || !value.endsWith(")")) {
			throw new IllegalArgumentException("Cannot convert string \""
					+ value + "\" - missing start and end brackets");
		}

		final var parts = value.substring(1, value.length() - 1).split(":");
		if (parts.length != N_PARTS) {
			throw new IllegalArgumentException(
					"Wrong number of :-separated arguments - " + parts.length
							+ " found but 6 required");
		}

		return new SpinnakerMachine(
				parts[MACHINE_NAME_PART].trim(), parts[VERSION_PART].trim(),
				parseInt(parts[WIDTH_PART].trim()),
				parseInt(parts[HEIGHT_PART].trim()),
				parseInt(parts[N_BOARDS_PART].trim()),
				parts[BMP_DETAILS_PART].trim());
	}

	/**
	 * Get a string version of the machine.
	 */
	@Override
	public String toString() {
		final var output = new StringBuilder();
		// Note: List.of won't work here because things can be null and
		// List.of doesn't allow null things
		final var potentials = new Object[] {
			machineName, version, bmpDetails, width, height, bmpDetails
		};
		for (final var potential : potentials) {
			if (nonNull(potential)) {
				if (output.length() > 0) {
					output.append(':');
				}
				output.append(potential);
			}
		}
		return output.toString();
	}

	/**
	 * Creates a new SpiNNaker Machine description.
	 *
	 * @param machineName
	 *            The name of the machine
	 * @param version
	 *            The version of the machine
	 * @param width
	 *            The width of the machine, in chips
	 * @param height
	 *            The width of the machine, in chips
	 * @param numBoards
	 *            The number of boards in the machine
	 * @param bmpDetails
	 *            How to contact the machine's Board Management Processor
	 */
	public SpinnakerMachine(final String machineName, final String version,
			final int width, final int height, final int numBoards,
			final String bmpDetails) {
		this.machineName = machineName;
		this.version = version;
		this.width = width;
		this.height = height;
		this.nBoards = numBoards;
		this.bmpDetails = bmpDetails;
	}

	/**
	 * Gets the name of the machine.
	 *
	 * @return The name of the machine
	 */
	public String getMachineName() {
		return machineName;
	}

	/**
	 * Sets the name of the machine.
	 *
	 * @param machineName
	 *            The name of the machine
	 */
	public void setMachineName(final String machineName) {
		this.machineName = machineName;
	}

	/**
	 * Gets the version of the machine.
	 *
	 * @return The version of the machine
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Sets the version of the machine.
	 *
	 * @param version
	 *            The version of the machine
	 */
	public void setVersion(final String version) {
		this.version = version;
	}

	/**
	 * Gets the width of the machine in chips.
	 *
	 * @return The width of the machine
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * Sets the width of the machine in chips.
	 *
	 * @param width
	 *            The width of the machine
	 */
	public void setWidth(final int width) {
		this.width = width;
	}

	/**
	 * Gets the height of the machine in chips.
	 *
	 * @return The height of the machine
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * Sets the height of the machine in chips.
	 *
	 * @param height
	 *            The height of the machine
	 */
	public void setHeight(final int height) {
		this.height = height;
	}

	/** @return width &times; height */
	public int getArea() {
		return width * height;
	}

	/**
	 * Gets the number of boards in the machine.
	 *
	 * @return The number of boards in the machine
	 */
	public int getnBoards() {
		return nBoards;
	}

	/**
	 * Sets the number of boards in the machine.
	 *
	 * @param nBoards
	 *            The number of boards in the machine
	 */
	public void setnBoards(final int nBoards) {
		this.nBoards = nBoards;
	}

	/**
	 * Gets the BMP details of the machine.
	 *
	 * @return The BMP details of the machine
	 */
	public String getBmpDetails() {
		return bmpDetails;
	}

	/**
	 * Sets the BMP details of the machine.
	 *
	 * @param bmpDetails
	 *            The BMP details of the machine
	 */
	public void setBmpDetails(final String bmpDetails) {
		this.bmpDetails = bmpDetails;
	}

	/**
	 * Check for equality with another machine.
	 */
	@Override
	public boolean equals(final Object o) {
		if (o instanceof SpinnakerMachine) {
			// TODO Is this the right way to determine equality?
			final var m = (SpinnakerMachine) o;
			return Objects.equals(machineName, m.machineName)
					&& Objects.equals(version, m.version);
		} else {
			return false;
		}
	}

	/** Null-safe string comparator. */
	private static final Comparator<String> STRCMP =
			nullsFirst(String::compareTo);

	/** Machine comparator. */
	private static final Comparator<SpinnakerMachine> M_COMPARE =
			comparing(SpinnakerMachine::getMachineName, STRCMP)
					// TODO Is this the right way to compare versions? It works
					.thenComparing(SpinnakerMachine::getVersion, STRCMP);

	/**
	 * Compare to another machine; order by name then by version.
	 */
	@Override
	public int compareTo(final SpinnakerMachine m) {
		return M_COMPARE.compare(this, m);
	}

	/**
	 * Generate a hash code based on name and version.
	 */
	@Override
	public int hashCode() {
		// Must be consistent with equality tests
		int hc = 0;
		if (nonNull(machineName)) {
			hc ^= machineName.hashCode();
		}
		if (nonNull(version)) {
			hc ^= version.hashCode();
		}
		return hc;
	}
}
