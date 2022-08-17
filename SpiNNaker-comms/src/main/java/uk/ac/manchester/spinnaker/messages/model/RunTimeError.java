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
package uk.ac.manchester.spinnaker.messages.model;

import static java.util.Objects.requireNonNull;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.makeEnumBackingMap;

import java.util.Map;

/**
 * SARK Run time errors.
 */
public enum RunTimeError {
	/** No error. */
	NONE(0),
	/** Branch through zero. */
	RESET(1),
	/** Undefined instruction. */
	UNDEF(2),
	/** Undefined SVC or no handler. */
	SVC(3),
	/** Prefetch abort. */
	PABT(4),
	/** Data abort. */
	DABT(5),
	/** Unhandled IRQ. */
	IRQ(6),
	/** Unhandled FIQ. */
	FIQ(7),
	/** Unconfigured VIC vector. */
	VIC(8),
	/** Generic user abort. */
	ABORT(9),
	/** "malloc" failure. */
	MALLOC(10),
	/** Divide by zero. */
	DIVBY0(11),
	/** Event startup failure. */
	EVENT(12),
	/** Fatal SW error. */
	SWERR(13),
	/** Failed to allocate IO buffer. */
	IOBUF(14),
	/** Bad event enable. */
	ENABLE(15),
	/** Generic null pointer error. */
	NULL(16),
	/** Pkt startup failure. */
	PKT(17),
	/** Timer startup failure. */
	TIMER(18),
	/** API startup failure. */
	API(19),
	/** SW version conflict. */
	SARK_VERSION_INCORRECT(20);

	/** The SCAMP RTE code. */
	public final int value;

	private static final Map<Integer, RunTimeError> MAP =
			makeEnumBackingMap(values(), v -> v.value);

	RunTimeError(int value) {
		this.value = value;
	}

	/**
	 * Parse a SCAMP RTE code.
	 *
	 * @param value
	 *            the code to parse.
	 * @return The enum element.
	 */
	public static RunTimeError get(int value) {
		return requireNonNull(MAP.get(value), "unknown RTE state: " + value);
	}
}
