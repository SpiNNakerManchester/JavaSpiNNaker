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
package uk.ac.manchester.spinnaker.messages.boot;

import static uk.ac.manchester.spinnaker.messages.boot.BootOpCode.FLOOD_FILL_CONTROL;

/**
 * The message indicating the end of a flood fill for booting.
 *
 * @author Donal Fellows
 */
final class EndOfBootMessages extends BootMessage {
	EndOfBootMessages() {
		super(FLOOD_FILL_CONTROL, 1, 0, 0);
	}
}
