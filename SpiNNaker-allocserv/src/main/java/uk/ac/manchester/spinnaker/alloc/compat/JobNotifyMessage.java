/*
 * Copyright (c) 2021 The University of Manchester
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
