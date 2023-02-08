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
package uk.ac.manchester.spinnaker.messages.scp;

/** Standard bit shifts. */
abstract class Bits {
	private Bits() {
	}

	/** The top bit of the word. */
	static final int TOP_BIT = 31;

	/** Bits 31&ndash;24. */
	static final int BYTE3 = 24;

	/** Bits 23&ndash;16. */
	static final int BYTE2 = 16;

	/** Bits 15&ndash;8. */
	static final int BYTE1 = 8;

	/** Bits 7&ndash;0. */
	static final int BYTE0 = 0;

	/** Bits 31&ndash;16. */
	static final int HALF1 = 16;

	/** Bits 15&ndash;0. */
	static final int HALF0 = 0;
}
