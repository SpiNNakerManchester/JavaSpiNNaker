/*
 * Copyright (c) 2018-2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc;

/**
 * Miscellaneous constants.
 *
 * @author Donal Fellows
 */
public interface Constants {
	/** The number of boards in a triad. */
	int TRIAD_DEPTH = 3;

	/** The width and height of a triad, in chips. */
	int TRIAD_CHIP_SIZE = 12;
}
