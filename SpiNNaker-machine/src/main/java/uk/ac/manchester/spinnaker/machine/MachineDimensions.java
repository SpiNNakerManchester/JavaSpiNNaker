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
package uk.ac.manchester.spinnaker.machine;

import com.google.errorprone.annotations.Immutable;

/** Represents the size of a machine in chips. */
@Immutable
public final class MachineDimensions {
	/** The width of the machine in chips. */
	@ValidMachineWidth
	public final int width;

	/** The height of the machine in chips. */
	@ValidMachineHeight
	public final int height;

	/**
	 * Create a new instance.
	 *
	 * @param width
	 *            The width of the machine, in chips.
	 * @param height
	 *            The height of the machine, in chips.
	 */
	public MachineDimensions(int width, int height) {
		this.width = width;
		this.height = height;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof MachineDimensions) {
			var dim = (MachineDimensions) o;
			return (width == dim.width) && (height == dim.height);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return width << 16 | height;
	}

	@Override
	public String toString() {
		return "Width:" + width + " Height:" + height;
	}
}
