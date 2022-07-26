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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.List;

/**
 * Bean to represent values on a Chip that are typically the same on all Chips.
 *
 * @author Christian-B
 */
public class ChipResources {
	/**
	 * Symbolic value to specify no specific value has been set.
	 *
	 * This allows the value 0 to be declared as specifically set.
	 */
	public static final int NOT_SET = -1;

	private int cores;

	private int monitors;

	private int sdram;

	private List<Integer> tags;

	private int routerEntries;

	private Boolean virtual;

	/**
	 * Default constructor which sets all values to not set.
	 */
	public ChipResources() {
		cores = NOT_SET;
		monitors = NOT_SET;
		sdram = NOT_SET;
		routerEntries = NOT_SET;
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
		if (isNull(tags)) {
			tags = defaults.tags;
		}
		if (routerEntries == NOT_SET) {
			routerEntries = defaults.routerEntries;
		}
		if (isNull(virtual)) {
			virtual = defaults.getVirtual();
		}
	}

	/**
	 * @return the number of cores.
	 */
	public int getCores() {
		return cores;
	}

	/**
	 * @param cores
	 *            the cores to set
	 */
	public void setCores(int cores) {
		this.cores = cores;
	}

	/**
	 * @return the monitors
	 */
	public int getMonitors() {
		return monitors;
	}

	/**
	 * @param monitors
	 *            the monitors to set
	 */
	public void setMonitors(int monitors) {
		this.monitors = monitors;
	}

	/**
	 * @return the sdram
	 */
	public int getSdram() {
		return sdram;
	}

	/**
	 * @param sdram
	 *            the sdram to set
	 */
	public void setSdram(int sdram) {
		this.sdram = sdram;
	}

	/**
	 * @return the tags
	 */
	public List<Integer> getTags() {
		return tags;
	}

	/**
	 * @param tags
	 *            the tags to set
	 */
	public void setTags(List<Integer> tags) {
		this.tags = tags;
	}

	/**
	 * @return the router_entries
	 */
	public int getRouterEntries() {
		return routerEntries;
	}

	/**
	 * @param routerEntries
	 *            the router_entries to set
	 */
	public void setRouterEntries(int routerEntries) {
		this.routerEntries = routerEntries;
	}

	/**
	 * @return the virtual
	 */
	public Boolean getVirtual() {
		return virtual;
	}

	/**
	 * @param virtual
	 *            the virtual to set
	 */
	public void setVirtual(Boolean virtual) {
		this.virtual = virtual;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("[");
		if (cores != NOT_SET) {
			builder.append("cores: ").append(cores).append(", ");
		}
		if (monitors != NOT_SET) {
			builder.append("monitors: ").append(monitors).append(", ");
		}
		if (sdram != NOT_SET) {
			builder.append("sdram: ").append(sdram).append(", ");
		}
		if (nonNull(tags)) {
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
}
