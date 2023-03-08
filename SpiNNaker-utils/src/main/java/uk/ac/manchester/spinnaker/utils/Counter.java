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
package uk.ac.manchester.spinnaker.utils;

/**
 * Thin wrapper around an {@code int} for counting.
 * <p>
 * This allows the object to be final and therefore passed into inner classes.
 * <p>
 * This is <i>not</i> thread safe.
 *
 * @author Christian-B
 */
public final class Counter {
	private int count;

	/**
	 * Create a counter starting at zero.
	 */
	public Counter() {
		count = 0;
	}

	/**
	 * Add one to the count.
	 */
	public void increment() {
		count++;
	}

	/**
	 * Add any amount to the counter.
	 * <p>
	 * Could also be used to add a negative number.
	 *
	 * @param other
	 *            int values by which to change the counter.
	 */
	public void add(int other) {
		count += other;
	}

	/**
	 * Retrieve the current value.
	 *
	 * @return The current counter value.
	 */
	public int get() {
		return count;
	}
}
