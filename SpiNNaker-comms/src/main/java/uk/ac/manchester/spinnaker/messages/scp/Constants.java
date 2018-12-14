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
package uk.ac.manchester.spinnaker.messages.scp;

/**
 * Constants used elsewhere in this package only.
 *
 * @author Donal Fellows
 */
abstract class Constants {
	private Constants() {
	}

	/**
	 * Indicates that all cores should receive a signal.
	 */
	static final int ALL_CORE_SIGNAL_MASK = 0xFFFF;

	/**
	 * Mask for selecting application IDs for signals.
	 */
	static final int APP_MASK = 0xFF;
}
