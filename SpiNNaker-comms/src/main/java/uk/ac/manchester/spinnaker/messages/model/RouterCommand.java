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
