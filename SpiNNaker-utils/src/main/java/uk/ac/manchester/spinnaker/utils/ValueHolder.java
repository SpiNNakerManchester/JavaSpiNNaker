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
package uk.ac.manchester.spinnaker.utils;

import java.util.function.Function;

/**
 * A simple class that can optionally hold a single value. <i>This class is
 * modifiable.</i>
 *
 * @author Donal Fellows
 * @param <T>
 *            The type of value to hold.
 */
public class ValueHolder<T> {
	private T value;

	/**
	 * Create an instance. The initial value held is {@code null}.
	 */
	public ValueHolder() {
	}

	/**
	 * Create an instance.
	 *
	 * @param value
	 *            The initial value to hold.
	 */
	public ValueHolder(T value) {
		this.value = value;
	}

	/**
	 * Get whether a (non-{@code null}) value is absent.
	 *
	 * @return Whether a value is absent and this holder is empty.
	 */
	public boolean isEmpty() {
		return value == null;
	}

	/**
	 * Get the value held.
	 *
	 * @return The value held.
	 */
	public T getValue() {
		return value;
	}

	/**
	 * Set the value to hold.
	 *
	 * @param value
	 *            The new value to hold.
	 */
	public void setValue(T value) {
		this.value = value;
	}

	/**
	 * Applies a function to update the value of this instance, returning the
	 * old value.
	 *
	 * @param function
	 *            The operation to apply.
	 * @return The <em>old</em> value.
	 */
	public T update(Function<T, T> function) {
		try {
			return value;
		} finally {
			value = function.apply(value);
		}
	}
}
