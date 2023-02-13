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
package uk.ac.manchester.spinnaker.messages.scp;

/**
 * Constants used elsewhere in this package only.
 *
 * @author Donal Fellows
 */
abstract class Constants {
	private Constants() {
	}

	/**
	 * Indicates that all cores should receive a signal.
	 */
	static final int ALL_CORE_SIGNAL_MASK = 0xFFFF;

	/**
	 * Mask for selecting application IDs for signals.
	 */
	static final int APP_MASK = 0xFF;
}
