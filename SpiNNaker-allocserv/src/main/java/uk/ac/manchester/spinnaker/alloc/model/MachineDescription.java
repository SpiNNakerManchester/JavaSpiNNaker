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
package uk.ac.manchester.spinnaker.alloc.model;

import static java.util.Collections.unmodifiableList;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import uk.ac.manchester.spinnaker.utils.MappableIterable;

/**
 * Descriptive detail for a machine. Used for HTML generation.
 */
public class MachineDescription {
	private int id;

	@NotBlank
	private String name;

	@Positive
	private int width;

	@Positive
	private int height;

	@PositiveOrZero
	private int numInUse;

	private List<@Valid JobInfo> jobs = new ArrayList<>();

	private List<@NotBlank String> tags = new ArrayList<>();

	private List<@Valid BoardCoords> dead = new ArrayList<>();

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
		this.live = live;
	}

	/** @param live the in-service boards */
	public void setLive(MappableIterable<BoardCoords> live) {
		this.live = live.toList();
	}

	/** @return the out-of-service boards */
	public List<BoardCoords> getDead() {
		return dead;
	}

	/** @param dead the out-of-service boards */
	public void setDead(List<BoardCoords> dead) {
		this.dead = dead;
	}

	/** @param dead the out-of-service boards */
	public void setDead(MappableIterable<BoardCoords> dead) {
		this.dead = dead.toList();
	}

	/** @return the machine's jobs */
	public List<JobInfo> getJobs() {
		return unmodifiableList(jobs);
	}

	/** @param jobs the machine's jobs */
	public void setJobs(List<JobInfo> jobs) {
		this.jobs = jobs;
	}

	/** @param jobs the machine's jobs */
	public void setJobs(MappableIterable<JobInfo> jobs) {
		this.jobs = jobs.toList();
	}

	/** @return the machine's tags */
	public List<String> getTags() {
		return unmodifiableList(tags);
	}

	/** @param tags the machine's tags */
	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	/** @param tags the machine's tags */
	public void setTags(MappableIterable<String> tags) {
		this.tags = tags.toList();
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

		private List<@Valid BoardCoords> boards = new ArrayList<>();

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
			return unmodifiableList(boards);
		}

		/**
		 * @param boards
		 *            the board coordinates of all boards allocated to the job
		 */
		public void setBoards(List<BoardCoords> boards) {
			this.boards = boards;
		}

		/**
		 * @param boards
		 *            the board coordinates of all boards allocated to the job
		 */
		public void setBoards(MappableIterable<BoardCoords> boards) {
			this.boards = boards.toList();
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
