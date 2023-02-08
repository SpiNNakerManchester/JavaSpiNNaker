/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.model;

import static uk.ac.manchester.spinnaker.utils.CollectionUtils.copy;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;

import uk.ac.manchester.spinnaker.machine.board.ValidTriadHeight;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadWidth;
import uk.ac.manchester.spinnaker.utils.MappableIterable;

/**
 * Descriptive detail for a machine. Used for HTML generation.
 */
public class MachineDescription {
	private int id;

	@NotBlank
	private String name;

	@ValidTriadWidth
	private int width;

	@ValidTriadHeight
	private int height;

	@PositiveOrZero
	private int numInUse;

	private List<@Valid JobInfo> jobs = List.of();

	private List<@NotBlank String> tags = List.of();

	private List<@Valid BoardCoords> dead = List.of();

	private List<@Valid BoardCoords> live;

	private Optional<Long> quota = Optional.empty();

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

	/** @return the width of the machine in triads */
	public int getWidth() {
		return width;
	}

	/** @param width the width of the machine in triads */
	public void setWidth(int width) {
		this.width = width;
	}

	/** @return the height of the machine in triads */
	public int getHeight() {
		return height;
	}

	/** @param height the height of the machine in triads */
	public void setHeight(int height) {
		this.height = height;
	}

	/** @return the number of boards in use */
	public int getNumInUse() {
		return numInUse;
	}

	/** @param numInUse the number of boards in use */
	public void setNumInUse(int numInUse) {
		this.numInUse = numInUse;
	}

	/** @return the in-service boards */
	public List<BoardCoords> getLive() {
		return live;
	}

	/** @param live the in-service boards */
	public void setLive(List<BoardCoords> live) {
		this.live = copy(live);
	}

	/** @param live the in-service boards */
	public void setLive(MappableIterable<BoardCoords> live) {
		this.live = copy(live.toList());
	}

	/** @return the out-of-service boards */
	public List<BoardCoords> getDead() {
		return dead;
	}

	/** @param dead the out-of-service boards */
	public void setDead(List<BoardCoords> dead) {
		this.dead = copy(dead);
	}

	/** @param dead the out-of-service boards */
	public void setDead(MappableIterable<BoardCoords> dead) {
		this.dead = copy(dead.toList());
	}

	/** @return the machine's jobs */
	public List<JobInfo> getJobs() {
		return jobs;
	}

	/** @param jobs the machine's jobs */
	public void setJobs(List<JobInfo> jobs) {
		this.jobs = copy(jobs);
	}

	/** @param jobs the machine's jobs */
	public void setJobs(MappableIterable<JobInfo> jobs) {
		this.jobs = copy(jobs.toList());
	}

	/** @return the machine's tags */
	public List<String> getTags() {
		return tags;
	}

	/** @param tags the machine's tags */
	public void setTags(List<String> tags) {
		this.tags = copy(tags);
	}

	/** @param tags the machine's tags */
	public void setTags(MappableIterable<String> tags) {
		this.tags = copy(tags.toList());
	}

	/**
	 * @return the quota (if that information is to be exposed to the current
	 *         user and is meaningful)
	 */
	public Optional<Long> getQuota() {
		return quota;
	}

	/** @param quota the current user's quota */
	public void setQuota(Long quota) {
		this.quota = Optional.ofNullable(quota);
	}

	/** Information about a single job. */
	public static class JobInfo {
		private int id;

		private URI url;

		private Optional<String> owner = Optional.empty();

		private List<@Valid BoardCoords> boards = List.of();

		/** @return the job ID */
		public int getId() {
			return id;
		}

		/** @param id the job ID */
		public void setId(int id) {
			this.id = id;
		}

		/** @return the URL for more detail  */
		public Optional<URI> getUrl() {
			return Optional.ofNullable(url);
		}

		/** @param url the URL for more detail  */
		public void setUrl(URI url) {
			this.url = url;
		}

		/** @return the board coordinates of all boards allocated to the job */
		public List<BoardCoords> getBoards() {
			return boards;
		}

		/**
		 * @param boards
		 *            the board coordinates of all boards allocated to the job
		 */
		public void setBoards(List<BoardCoords> boards) {
			this.boards = copy(boards);
		}

		/**
		 * @param boards
		 *            the board coordinates of all boards allocated to the job
		 */
		public void setBoards(MappableIterable<BoardCoords> boards) {
			this.boards = copy(boards.toList());
		}

		/** @return the owner (if that information is to be exposed) */
		public Optional<String> getOwner() {
			return owner;
		}

		/** @param owner the owner of the job */
		public void setOwner(String owner) {
			this.owner = Optional.ofNullable(owner);
		}
	}
}
