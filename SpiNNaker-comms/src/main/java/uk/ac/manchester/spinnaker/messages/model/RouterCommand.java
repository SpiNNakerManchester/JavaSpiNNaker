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

/**
 * The basic router commands, handled by {@code cmd_rtr()} in
 * {@code scamp-cmd.c}.
 */
public enum RouterCommand {
	/** Initialise. */
	ROUTER_INIT,
	/** Clear (entry=arg2, count). */
	ROUTER_CLEAR,
	/** Load (addr=arg2, count, offset=arg3, app_id). */
	ROUTER_LOAD,
	/**
	 * Set/get Fixed Route register (arg2 = route). if bit 31 of arg1 set then
	 * return FR reg else set it
	 */
	ROUTER_FIXED;

	/** The BMP-encoded value. */
	public final byte value;

	RouterCommand() {
		value = (byte) ordinal();
	}
}
