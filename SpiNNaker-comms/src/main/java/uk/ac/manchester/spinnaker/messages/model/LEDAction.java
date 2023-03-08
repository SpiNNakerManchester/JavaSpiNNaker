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
package uk.ac.manchester.spinnaker.messages.model;

/** The SCP LED actions. */
public enum LEDAction {
	/** Do nothing. */
	NO_CHANGE(0),
	/** Toggle the LED status. */
	TOGGLE(1),
	/** Turn the LED off. */
	OFF(2),
	/** Turn the LED on. */
	ON(3);

	/** The SCAMP-encoded value. */
	public final byte value;

	LEDAction(int value) {
		this.value = (byte) value;
	}
}
