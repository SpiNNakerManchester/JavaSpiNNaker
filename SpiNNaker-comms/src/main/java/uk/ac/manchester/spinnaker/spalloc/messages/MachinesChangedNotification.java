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
package uk.ac.manchester.spinnaker.spalloc.messages;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.copy;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * A response that describes what machines have changed state.
 */
@JsonAutoDetect(setterVisibility = NON_PRIVATE)
public class MachinesChangedNotification implements Notification {
	private List<String> machinesChanged = List.of();

	/** @return What machines have changed. */
	public List<String> getMachinesChanged() {
		return machinesChanged;
	}

	void setMachinesChanged(List<String> machinesChanged) {
		this.machinesChanged = copy(machinesChanged);
	}

	@Override
	public String toString() {
		return "Machine Changed " + machinesChanged;
	}
}
