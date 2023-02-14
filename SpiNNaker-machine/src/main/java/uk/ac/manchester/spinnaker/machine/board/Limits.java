/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.machine.board;

/**
 * Various limits. These are constrained by the requirement that each of the
 * dimensions of the coordinates for a chip fit in an unsigned byte.
 */
interface Limits {
	/**
	 * Max triad X coordinate. Any larger and the chips become unaddressible.
	 */
	int MAX_TRIAD_X = 20;

	/**
	 * Max triad Y coordinate. Any larger and the chips become unaddressible.
	 */
	int MAX_TRIAD_Y = 20;

	/** Max triad Z coordinate. */
	int MAX_TRIAD_Z = 2;

	/**
	 * Maximum cabinet number. Any larger than this will result in the
	 * underlying chips becoming unaddressible.
	 */
	int MAX_CABINET = 31;

	/**
	 * Maximum frame number. Any larger than this will result in the underlying
	 * chips becoming unaddressible.
	 */
	int MAX_FRAME = 31;
}
