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

/**
 * Descriptive detail for a machine. Used for HTML generation.
 */
public class MachineDescription {
	private int id;

	private String name;

	private int width;

	private int height;

	private int numInUse;

	private List<JobInfo> jobs = new ArrayList<>();

	private List<String> tags = new ArrayList<>();

	public MachineDescription() {
	}

	/**
	 * @return the machine ID
	 */
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return the machine name
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the width of the machine in triads
	 */
	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	/**
	 * @return the height of the machine in triads
	 */
	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	/**
	 * @return the number of boards in use
	 */
	public int getNumInUse() {
		return numInUse;
	}

	public void setNumInUse(int numInUse) {
		this.numInUse = numInUse;
	}

	/**
	 * @return the machine's jobs
	 */
	public List<JobInfo> getJobs() {
		return unmodifiableList(jobs);
	}

	public void setJobs(List<JobInfo> jobs) {
		this.jobs = jobs;
	}

	/**
	 * @return the machine's tags
	 */
	public List<String> getTags() {
		return unmodifiableList(tags);
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	/** Information about a single job. */
	public static class JobInfo {
		private int id;

		private URI url;

		private Optional<String> owner = Optional.empty();

		private List<BoardCoords> boards = new ArrayList<>();

		/**
		 * @return the job ID
		 */
		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		/**
		 * @return the URL for more detail
		 */
		public Optional<URI> getUrl() {
			return Optional.ofNullable(url);
		}

		public void setUrl(URI url) {
			this.url = url;
		}

		/**
		 * @return the board coordinates of all boards allocated to the job
		 */
		public List<BoardCoords> getBoards() {
			return unmodifiableList(boards);
		}

		public void setBoards(List<BoardCoords> boards) {
			this.boards = boards;
		}

		/**
		 * @return the owner
		 */
		public Optional<String> getOwner() {
			return owner;
		}

		public void setOwner(String owner) {
			this.owner = Optional.ofNullable(owner);
		}
	}
}
