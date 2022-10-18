/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.machine.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import uk.ac.manchester.spinnaker.machine.tags.TagID;

import java.util.List;

/**
 * Bean to represent values on a Chip that are typically the same on all Chips.
 *
 * @author Christian-B
 */
@JsonDeserialize(builder = ChipResources.Builder.class)
public class ChipResources {
	/**
	 * Symbolic value to specify no specific value has been set.
	 * <p>
	 * This allows the value 0 to be declared as specifically set.
	 */
	public static final int NOT_SET = -1;

	private int cores;

	private int monitors;

	private int sdram;

	private List<@TagID Integer> tags;

	private int routerEntries;

	/**
	 * Default constructor which sets all values to not set.
	 */
	public ChipResources() {
		cores = NOT_SET;
		monitors = NOT_SET;
		sdram = NOT_SET;
		routerEntries = NOT_SET;
	}

	private ChipResources(int cores, int monitors, int sdram,
			List<Integer> tags, int routerEntries) {
		this.cores = cores;
		this.monitors = monitors;
		this.sdram = sdram;
		this.tags = tags;
		this.routerEntries = routerEntries;
	}

	/**
	 * Adds the default values if and only if no value had been specifically
	 * set. If a value is not set in both this and the defaults it will remain
	 * as not set. No Exception is thrown.
	 *
	 * @param defaults
	 *            Another resources whose values should replace those which have
	 *            not been set.
	 */
	@JsonIgnore
	public void addDefaults(ChipResources defaults) {
		if (cores == NOT_SET) {
			cores = defaults.cores;
		}
		if (monitors == NOT_SET) {
			monitors = defaults.monitors;
		}
		if (sdram == NOT_SET) {
			sdram = defaults.sdram;
		}
		if (tags == null) {
			tags = defaults.tags;
		}
		if (routerEntries == NOT_SET) {
			routerEntries = defaults.routerEntries;
		}
	}

	/**
	 * @return the number of cores.
	 */
	public int getCores() {
		return cores;
	}

	/**
	 * @return the monitors
	 */
	public int getMonitors() {
		return monitors;
	}

	/**
	 * @return the sdram
	 */
	public int getSdram() {
		return sdram;
	}

	/**
	 * @return the tags
	 */
	public List<Integer> getTags() {
		if (tags == null) {
			tags = List.of();
		}
		return List.copyOf(tags);
	}

	/**
	 * @return the router_entries
	 */
	public int getRouterEntries() {
		return routerEntries;
	}

	@Override
	public String toString() {
		var builder = new StringBuilder("[");
		if (cores != NOT_SET) {
			builder.append("cores: ").append(cores).append(", ");
		}
		if (monitors != NOT_SET) {
			builder.append("monitors: ").append(monitors).append(", ");
		}
		if (sdram != NOT_SET) {
			builder.append("sdram: ").append(sdram).append(", ");
		}
		if (tags != null) {
			builder.append("tags: ").append(tags).append(", ");
		}
		if (routerEntries != NOT_SET) {
			builder.append("router_entries: ").append(routerEntries)
					.append(", ");
		}
		if (builder.length() < 2) {
			builder.append("EMPTY");
		}
		builder.setLength(builder.length() - 2);
		builder.append("]");
		return builder.toString();
	}

	@JsonPOJOBuilder
	static class Builder {
		private int cores = NOT_SET;

		private int monitors = NOT_SET;

		private int sdram = NOT_SET;

		private List<Integer> tags = List.of();

		private int routerEntries = NOT_SET;

		@CanIgnoreReturnValue
		public Builder withCores(int cores) {
			this.cores = cores;
			return this;
		}

		@CanIgnoreReturnValue
		public Builder withMonitors(int monitors) {
			this.monitors = monitors;
			return this;
		}

		@CanIgnoreReturnValue
		public Builder withSdram(int sdram) {
			this.sdram = sdram;
			return this;
		}

		@CanIgnoreReturnValue
		public Builder withTags(List<Integer> tags) {
			this.tags = List.copyOf(tags);
			return this;
		}

		@CanIgnoreReturnValue
		public Builder withRouterEntries(int routerEntries) {
			this.routerEntries = routerEntries;
			return this;
		}

		public ChipResources build() {
			return new ChipResources(cores, monitors, sdram, tags,
					routerEntries);
		}
	}
}
