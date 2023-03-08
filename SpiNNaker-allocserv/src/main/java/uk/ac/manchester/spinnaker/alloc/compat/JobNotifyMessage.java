/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.compat;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A job notification message.
 *
 * @author Donal Fellows
 * @param jobsChanged
 *            What jobs have had their state change
 */
record JobNotifyMessage(
		@JsonProperty("jobs_changed") List<Integer> jobsChanged) {
}
