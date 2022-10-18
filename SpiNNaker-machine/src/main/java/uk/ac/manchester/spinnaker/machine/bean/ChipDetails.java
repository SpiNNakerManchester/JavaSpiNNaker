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

import static java.net.InetAddress.getByName;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.MAX_NUM_CORES;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

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
	 * Creates a Chip Details bean with the required fields leaving optional
	 * ones form setters.
	 *
	 * @param cores
	 *            Total number of cores working cores including monitors.
	 * @param ipAddress
	 *            the ipAddress to set
	 * @param ethernet
	 *            Location of the nearest Ethernet Chip.
	 * @param links
	 *            Description of link information (only present when the links
	 *            are not a complete default set).
	 * @param deadLinks
	 *            the deadLinks to set
	 * @throws UnknownHostException
	 *             If the ipAddress can not be converted to an InetAddress
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
		if (nonNull(ipAddress)) {
			this.ipAddress = getByName(ipAddress);
		} else {
			this.ipAddress = null;
		}
		if (nonNull(deadLinks)) {
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
	 * @return the ethernet
	 */
	public ChipLocation getEthernet() {
		return ethernet;
	}

	/**
	 * @return the ipAddress
	 */
	public InetAddress getIpAddress() {
		return ipAddress;
	}

	/**
	 * @return the deadLinks
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
		if (nonNull(ipAddress)) {
			builder.append(" ipAddress: ").append(ipAddress).append(", ");
		}
		if (nonNull(deadDirections)) {
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
		if (nonNull(links)) {
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
