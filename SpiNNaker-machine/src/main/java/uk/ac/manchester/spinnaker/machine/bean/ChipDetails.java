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

import static java.net.InetAddress.getByName;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.MAX_NUM_CORES;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import uk.ac.manchester.spinnaker.machine.Chip;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * The details of a {@link Chip}.
 *
 * @author Christian-B
 */
@UsedInJavadocOnly(Chip.class)
public class ChipDetails {
	/** Total number of working core on this chip. */
	@PositiveOrZero
	@Max(MAX_NUM_CORES)
	public final int cores;

	/** Location of the nearest Ethernet-enabled chip. */
	@Valid
	public final ChipLocation ethernet;

	private final InetAddress ipAddress;

	private final Set<@NotNull Direction> deadDirections;

	private final List<@Valid LinkBean> links;

	/**
	 * Make an instance.
	 *
	 * @param cores
	 *            Total number of cores working cores including monitors.
	 * @param ipAddress
	 *            the IP address to set
	 * @param ethernet
	 *            Location of the nearest Ethernet-enabled chip.
	 * @param links
	 *            Description of link information (only present when the links
	 *            are not a complete default set).
	 * @param deadLinks
	 *            the dead links of the chip
	 * @throws UnknownHostException
	 *             If the {@code ipAddress} can not be converted to an
	 *             {@link InetAddress}
	 */
	public ChipDetails(
			@JsonProperty(value = "cores", required = true) int cores,
			@JsonProperty(value = "ethernet") ChipLocation ethernet,
			@JsonProperty(value = "ipAddress") String ipAddress,
			@JsonProperty(value = "links") List<LinkBean> links,
			@JsonProperty(value = "deadLinks") List<Integer> deadLinks)
			throws UnknownHostException {
		this.cores = cores;
		this.ethernet = ethernet;
		this.links = links;
		if (ipAddress != null) {
			this.ipAddress = getByName(ipAddress);
		} else {
			this.ipAddress = null;
		}
		if (deadLinks != null) {
			deadDirections = deadLinks.stream().map(Direction::byId)
					.collect(toUnmodifiableSet());
		} else {
			deadDirections = Set.of();
		}
	}

	/**
	 * @return the number of cores.
	 */
	public int getCores() {
		return cores;
	}

	/**
	 * @return the location of the board's Ethernet-enabled chip
	 */
	public ChipLocation getEthernet() {
		return ethernet;
	}

	/**
	 * @return the IP address
	 */
	public InetAddress getIpAddress() {
		return ipAddress;
	}

	/**
	 * @return the dead links of the chip
	 */
	@JsonIgnore
	public Set<Direction> getDeadDirections() {
		return deadDirections;
	}

	@Override
	public String toString() {
		var builder = new StringBuilder("[");
		builder.append("ethernet: ").append(ethernet).append(", ");
		builder.append(" cores: ").append(cores).append(", ");
		if (ipAddress != null) {
			builder.append(" ipAddress: ").append(ipAddress).append(", ");
		}
		if (deadDirections != null) {
			builder.append(" deadLinks:").append(deadDirections).append(", ");
		}
		builder.setLength(builder.length() - 2);
		builder.append("]");
		return builder.toString();
	}

	/**
	 * Gets where a link is really going.
	 *
	 * @param direction
	 *            Which direction the link is going in.
	 * @param source
	 *            Where the link is coming from.
	 * @param machine
	 *            The machine on which the link exists.
	 * @return The location of the destination of the link.
	 */
	public ChipLocation getLinkDestination(Direction direction,
			HasChipLocation source, Machine machine) {
		if (links != null) {
			for (var bean : links) {
				if (bean.sourceDirection == direction) {
					return bean.destination;
				}
			}
		}
		return machine.normalizedLocation(source.getX() + direction.xChange,
				source.getY() + direction.yChange);
	}
}
