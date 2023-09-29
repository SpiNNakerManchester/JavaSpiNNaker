/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.machine;

import com.google.errorprone.annotations.Immutable;

/**
 * Represents the size of a machine in chips.
 *
 * @param width
 *            The width of the machine, in chips.
 * @param height
 *            The height of the machine, in chips.
 */
@Immutable
public record MachineDimensions(//
		@ValidMachineWidth int width, //
		@ValidMachineHeight int height) {
	@Override
	public String toString() {
		return "Width:" + width + " Height:" + height;
	}
}
