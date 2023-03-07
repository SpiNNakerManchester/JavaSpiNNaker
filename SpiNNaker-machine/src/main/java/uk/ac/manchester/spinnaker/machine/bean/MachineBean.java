/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.machine.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

import javax.validation.Valid;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.MachineDimensions;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * Used to build a {@link Machine} from JSON.
 *
 * @author Christian-B
 */
@UsedInJavadocOnly(Machine.class)
public class MachineBean {
	@Valid
	private final MachineDimensions dimensions;

	@Valid
	private final ChipLocation root;

	@Valid
	private final ChipResources ethernetResources;

	@Valid
	private final ChipResources standardResources;

	private final List<@Valid ChipBean> chips;

	/**
	 * Main Constructor that sets all values.
	 *
	 * @param height
	 *            The height of the Machine in Chips
	 * @param width
	 *            The width of the Machine in Chips
	 * @param root
	 *            The Root Chip. (Typically 0,0)
	 * @param ethernetResources
	 *            The resource values shared by all chips that have an
	 *            ip_address, expect when overwritten by the Chip itself.
	 * @param standardResources
	 *            The resource values shared by all chips that do not have an
	 *            ip_address, expect when overwritten by the Chip itself.
	 * @param chips
	 *            Beans for each Chips on the machine.
	 */
	public MachineBean(
			@JsonProperty(value = "height", required = true) int height,
			@JsonProperty(value = "width", required = true) int width,
			@JsonProperty(value = "root", required = true) ChipLocation root,
			@JsonProperty(value = "ethernetResources", required = true)
			ChipResources ethernetResources,
			@JsonProperty(value = "standardResources", required = true)
			ChipResources standardResources,
			@JsonProperty(value = "chips", required = true)
			List<ChipBean> chips) {
		dimensions = new MachineDimensions(width, height);
		this.root = root;
		this.chips = chips;
		this.ethernetResources = ethernetResources;
		this.standardResources = standardResources;
	}

	/** @return The dimensions of the machine. */
	@JsonIgnore
	public MachineDimensions getMachineDimensions() {
		return dimensions;
	}

	/** @return The root chip of the machine. */
	public ChipLocation getRoot() {
		return root;
	}

	/** @return the default resources of ethernet-enabled chips */
	public ChipResources getEthernetResources() {
		return ethernetResources;
	}

	/** @return the default resources of chips that are not ethernet-enabled */
	public ChipResources getStandardResources() {
		return standardResources;
	}

	/** @return the chips */
	public List<ChipBean> getChips() {
		return chips;
	}

	@Override
	public String toString() {
		return dimensions + " root: " + root + "# Chips: " + chips.size();
	}

	/**
	 * Longer String representation over several lines.
	 *
	 * @return A description of the machine and its details.
	 */
	public String describe() {
		var builder = new StringBuilder();
		builder.append(dimensions);
		builder.append("\nroot: ").append(root);
		builder.append("\nethernet_resources: ").append(ethernetResources);
		builder.append("\nstandard_resources: ").append(standardResources);
		for (var bean : chips) {
			builder.append("\n").append(bean);
		}
		return builder.toString();
	}

}
