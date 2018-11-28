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
package uk.ac.manchester.spinnaker.utils;

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
}
