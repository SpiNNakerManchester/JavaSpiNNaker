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
package uk.ac.manchester.spinnaker.data_spec;

/**
 * A reference handle to another region.
 */
public final class Reference {
	/** The reference of the region. */
	private final int ref;

	/**
	 * Create a reference to another region.
	 *
	 * @param reference
	 *            The reference to make.
	 */
	Reference(int reference) {
		ref = reference;
	}

	@Override
	public String toString() {
		return Integer.toString(ref);
	}

	@Override
	public int hashCode() {
		return ref;
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof Reference r) && r.ref == ref;
	}
}
