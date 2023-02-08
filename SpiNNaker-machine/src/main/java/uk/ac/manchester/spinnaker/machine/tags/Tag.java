/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.machine.tags;

import static java.lang.Integer.compare;

import java.net.InetAddress;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import uk.ac.manchester.spinnaker.utils.validation.UDPPort;

/** Common properties of SpiNNaker IP tags and reverse IP tags. */
public abstract class Tag implements Comparable<Tag> {
	/** The board address associated with this tagID. */
	@NotNull
	private final InetAddress boardAddress;

	/** The tagID ID associated with this tagID. */
	@TagID
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
