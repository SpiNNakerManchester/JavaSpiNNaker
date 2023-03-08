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
package uk.ac.manchester.spinnaker.alloc.model;

import static uk.ac.manchester.spinnaker.utils.CollectionUtils.copy;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

/**
 * Entry in a table of machines. The table is like this:
 *
 * <table border="1">
 * <caption style="display:none">Machine List</caption>
 * <tr>
 * <th>Name
 * <th>Num boards
 * <th>In-use
 * <th>Jobs
 * <th>Tags
 * <tr>
 * <td>{@code SpiNNaker1M}
 * <td>1200
 * <td>28
 * <td>28
 * <td>{@code default}, {@code machine-room}
 * <tr>
 * <td>{@code Spin24b-001}
 * <td>24
 * <td>0
 * <td>0
 * <td>{@code default}, {@code IT408}
 * <tr>
 * <td>{@code power-monitor}
 * <td>1
 * <td>0
 * <td>0
 * <td>{@code power-monitor}, {@code machine-room}
 * <tr>
 * <td colspan=5>...
 * </table>
 */
public class MachineListEntryRecord {
	private int id;

	@NotBlank
	private String name;

	private URI detailsUrl;

	@Positive
	private int numBoards;

	@PositiveOrZero
	private int numInUse;

	@PositiveOrZero
	private int numJobs;

	private List<@NotBlank String> tags = List.of();

	/** @return the machine ID */
	public int getId() {
		return id;
	}

	/** @param id the machine ID */
	public void setId(int id) {
		this.id = id;
	}

	/** @return the machine name */
	public String getName() {
		return name;
	}

	/** @param name the machine name */
	public void setName(String name) {
		this.name = name;
	}

	/** @return where to go for more details */
	public Optional<URI> getDetailsUrl() {
		return Optional.ofNullable(detailsUrl);
	}

	/** @param detailsUrl where to go for more details */
	public void setDetailsUrl(URI detailsUrl) {
		this.detailsUrl = detailsUrl;
	}

	/** @return the number of boards in the machine */
	public int getNumBoards() {
		return numBoards;
	}

	/** @param numBoards the number of boards in the machine */
	public void setNumBoards(int numBoards) {
		this.numBoards = numBoards;
	}

	/** @return the number of boards in use */
	public int getNumInUse() {
		return numInUse;
	}

	/** @param numInUse the number of boards in use */
	public void setNumInUse(int numInUse) {
		this.numInUse = numInUse;
	}

	/** @return the number of jobs running on the machine */
	public int getNumJobs() {
		return numJobs;
	}

	/** @param numJobs the number of jobs running on the machine */
	public void setNumJobs(int numJobs) {
		this.numJobs = numJobs;
	}

	/** @return the machine's tags */
	public List<String> getTags() {
		return tags;
	}

	/** @param tags the machine's tags */
	public void setTags(List<String> tags) {
		this.tags = copy(tags);
	}
}
