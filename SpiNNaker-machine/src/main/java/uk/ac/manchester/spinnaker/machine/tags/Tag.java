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
package uk.ac.manchester.spinnaker.machine.tags;

import static java.lang.Integer.compare;

import java.net.InetAddress;
import java.util.Objects;

import javax.validation.constraints.NotNull;

/** Common properties of SpiNNaker IP tags and reverse IP tags. */
public abstract class Tag implements Comparable<Tag> {
	/** The board address associated with this tagID. */
	@NotNull
	private final InetAddress boardAddress;

	/** The tagID ID associated with this tagID. */
	private final int tagID;

	/** The port number associated with this tagID. */
	@UDPPort
	private Integer port;

	/**
	 * Create a tag.
	 *
	 * @param boardAddress
	 *            The address of the board where the tag is.
	 * @param tagID
	 *            The ID of the tag (0-7?)
	 * @param port
	 *            The UDP port of the tag.
	 */
	Tag(InetAddress boardAddress, int tagID, Integer port) {
		this.boardAddress = Objects.requireNonNull(boardAddress);
		this.tagID = tagID;
		this.port = port;
	}

	/** @return The board address of the tag. */
	public InetAddress getBoardAddress() {
		return boardAddress;
	}

	/**
	 * @return The ID of the tag. Note that this is the only property used for
	 *         determining order.
	 */
	public int getTag() {
		return tagID;
	}

	/**
	 * @return The port of the tagID, or {@code null} if there isn't one yet.
	 */
	public Integer getPort() {
		return port;
	}

	/**
	 * Set the port.
	 *
	 * @param port
	 *            the port to set
	 * @throws IllegalStateException
	 *             If the port has already been set.
	 */
	public void setPort(int port) {
		if (this.port != null && this.port != port) {
			throw new IllegalStateException("port cannot be changed to " + port
					+ " once set to " + this.port);
		}
		this.port = port;
	}

	// Subclasses *must* create an equality test and hash code
	/** {@inheritDoc} */
	@Override
	public abstract boolean equals(Object other);

	/** {@inheritDoc} */
	@Override
	public abstract int hashCode();

	/**
	 * Partial equality test between two tags. Only compares on fields defined
	 * in the {@link Tag} class. Used to make implementing a full equality test
	 * simpler.
	 *
	 * @param otherTag
	 *            The other tag to compare to.
	 * @return Whether the two tags are partially equal.
	 */
	protected final boolean partialEquals(Tag otherTag) {
		return tagID == otherTag.tagID
				&& boardAddress.equals(otherTag.boardAddress)
				&& Objects.equals(port, otherTag.port);
	}

	/**
	 * Partial hash code of a tag. Used to make implementing a full hash
	 * simpler.
	 *
	 * @return The hash of this class's fields.
	 */
	protected final int partialHashCode() {
		return Objects.hash(tagID, boardAddress, port);
	}

	/**
	 * Compare this tag to another one for ordering. <strong>Note:</strong> this
	 * class has a natural ordering that is inconsistent with equals; ordering
	 * is by the tag ID only (this is fine for tags obtained from the same chip
	 * at the same time, which is the primary use case).
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(Tag o) {
		return compare(tagID, o.tagID);
	}
}
