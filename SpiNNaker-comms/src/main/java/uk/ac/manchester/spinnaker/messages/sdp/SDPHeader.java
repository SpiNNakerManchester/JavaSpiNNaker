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
package uk.ac.manchester.spinnaker.messages.sdp;

import static java.lang.Byte.toUnsignedInt;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.MAX_NUM_CORES;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.validateChipLocation;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.SerializableMessage;

/**
 * Represents the header of an SDP message.
 */
public class SDPHeader implements SerializableMessage {
	private static final int MAX_PORT = 7;

	// Cpu:Port packing within port/cpu byte:
	// [ P|P|P | C|C|C|C|C ]
	private static final int CPU_ADDR_BITS = 5;

	private static final int CPU_MASK = (1 << CPU_ADDR_BITS) - 1;

	private static final int PORT_BITS = 8 - CPU_ADDR_BITS;

	private static final int PORT_MASK = (1 << PORT_BITS) - 1;

	private HasCoreLocation destination;

	private int destinationPort;

	private HasCoreLocation source;

	private int sourcePort;

	private Flag flags;

	private int tag;

	/**
	 * Create a header with all fields set to default. Note that messages
	 * containing this header <i>cannot</i> be sent until a source and
	 * destination have been set!
	 */
	public SDPHeader() {
	}

	/**
	 * Create a simple header with just the flags and destination set. Note that
	 * messages containing this header <i>cannot</i> be sent until a source has
	 * also been set!
	 *
	 * @param flags
	 *            The header flags.
	 * @param destination
	 *            Where the message is bound for.
	 * @param destinationPort
	 *            the <i>SDP port</i> that the message routes through. Note that
	 *            this is <b>not</b> a UDP port! Those are associated with a
	 *            connection, not a message.
	 * @throws IllegalArgumentException
	 *             if a bad SDP port is given
	 */
	public SDPHeader(Flag flags, HasCoreLocation destination,
			int destinationPort) {
		this.flags = flags;
		this.destination = destination;
		if (destinationPort < 0 || destinationPort > MAX_PORT) {
			throw new IllegalArgumentException("port out of range");
		}
		this.destinationPort = destinationPort;
	}

	/**
	 * Create a simple header with just the flags and destination set. Note that
	 * messages containing this header <i>cannot</i> be sent until a source has
	 * also been set!
	 *
	 * @param flags
	 *            The header flags.
	 * @param destination
	 *            Where the message is bound for.
	 * @param destinationPort
	 *            the <i>SDP port</i> that the message routes through. Note that
	 *            this is <b>not</b> a UDP port! Those are associated with a
	 *            connection, not a message.
	 */
	public SDPHeader(Flag flags, HasCoreLocation destination,
			SDPPort destinationPort) {
		this.flags = flags;
		this.destination = destination;
		this.destinationPort = destinationPort.value;
	}

	/**
	 * Read the header from an input buffer.
	 *
	 * @param buffer
	 *            The buffer to read from.
	 */
	public SDPHeader(ByteBuffer buffer) {
		// Caller MUST have stripped the leading padding
		assert buffer.position() == 2 : "leading padding must be skipped";
		flags = Flag.get(buffer.get());
		tag = Byte.toUnsignedInt(buffer.get());
		int dpc = toUnsignedInt(buffer.get());
		int spc = toUnsignedInt(buffer.get());
		int dcy = toUnsignedInt(buffer.get());
		int dcx = toUnsignedInt(buffer.get());
		int scy = toUnsignedInt(buffer.get());
		int scx = toUnsignedInt(buffer.get());
		destinationPort = (dpc >> CPU_ADDR_BITS) & PORT_MASK;
		sourcePort = (spc >> CPU_ADDR_BITS) & PORT_MASK;
		destination = allocCoreLocation(dcx, dcy, dpc & CPU_MASK);
		source = allocCoreLocation(scx, scy, spc & CPU_MASK);
	}

	private HasCoreLocation allocCoreLocation(int x, int y, int p) {
		if (p >= 0 && p < MAX_NUM_CORES) {
			return new CoreLocation(x, y, p);
		}
		validateChipLocation(x, y);
		return new HasCoreLocation() {
			@Override
			public int getX() {
				return x;
			}

			@Override
			public int getY() {
				return y;
			}

			@Override
			public int getP() {
				return p;
			}

			@Override
			public int hashCode() {
				throw new UnsupportedOperationException(
						"this object may not be used as a key");
			}

			@Override
			public boolean equals(Object other) {
				if (!(other instanceof HasCoreLocation)) {
					return false;
				}
				HasCoreLocation c = (HasCoreLocation) other;
				return x == c.getX() && y == c.getY() && p == c.getP();
			}
		};
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		int dpc = ((destinationPort & PORT_MASK) << CPU_ADDR_BITS)
				| (destination.getP() & CPU_MASK);
		int spc = ((sourcePort & PORT_MASK) << CPU_ADDR_BITS)
				| (source.getP() & CPU_MASK);

		buffer.put(flags.value);
		buffer.put((byte) tag);
		buffer.put((byte) dpc);
		buffer.put((byte) spc);
		buffer.put((byte) destination.getY());
		buffer.put((byte) destination.getX());
		buffer.put((byte) source.getY());
		buffer.put((byte) source.getX());
	}

	public HasCoreLocation getDestination() {
		return destination;
	}

	public void setDestination(HasCoreLocation destination) {
		this.destination = destination;
	}

	public int getDestinationPort() {
		return destinationPort;
	}

	/**
	 * Set the target SDP port. Note that this is not a UDP port.
	 *
	 * @param port
	 *            The port to set it to.
	 */
	public void setDestinationPort(SDPPort port) {
		this.destinationPort = port.value;
	}

	/**
	 * Set the target SDP port. Note that this is not a UDP port.
	 *
	 * @param port
	 *            The port to set it to.
	 * @throws IllegalArgumentException
	 *             If the port number is bad.
	 */
	public void setDestinationPort(int port) {
		if (port < 0 || port > MAX_PORT) {
			throw new IllegalArgumentException("port out of range");
		}
		this.destinationPort = port;
	}

	public HasCoreLocation getSource() {
		return source;
	}

	public void setSource(HasCoreLocation source) {
		this.source = source;
	}

	public int getSourcePort() {
		return sourcePort;
	}

	/**
	 * Set the originating SDP port. Note that this is not a UDP port.
	 *
	 * @param port
	 *            The port to set it to.
	 */
	public void setSourcePort(SDPPort port) {
		this.sourcePort = port.value;
	}

	/**
	 * Set the originating SDP port. Note that this is not a UDP port.
	 *
	 * @param port
	 *            The port to set it to.
	 * @throws IllegalArgumentException
	 *             If the port number is bad.
	 */
	public void setSourcePort(int port) {
		if (port < 0 || port > MAX_PORT) {
			throw new IllegalArgumentException("port out of range");
		}
		this.sourcePort = port;
	}

	public Flag getFlags() {
		return flags;
	}

	public void setFlags(Flag flags) {
		this.flags = flags;
	}

	public int getTag() {
		return tag;
	}

	public void setTag(byte tag) {
		this.tag = Byte.toUnsignedInt(tag);
	}

	/** The meanings of individual flag bits in {@link SDPHeader.Flag}. */
	private static class SCAMP {
		/**
		 * A secret agent's value!
		 * <p>
		 * Most plausible explanation of this (from a set of one) is that Dave
		 * Lester thinks that Jamie Knight put it in as a reference to wearing
		 * dinner jackets for a meeting at Pot Shrigley.
		 */
		private static final int BOND = 007;

		/** Reply expected. */
		private static final int SDPF_REPLY = 0x80;

		/** Checksum before routing. */
		@SuppressWarnings("unused")
		private static final int SDPF_SUM = 0x40;

		/** Don't route via P2P. */
		private static final int SDPF_NR = 0x20;
	}

	/** Possible values of the {@code flags} field. */
	public enum Flag {
		/** Indicates that a reply is not expected. */
		REPLY_NOT_EXPECTED(SCAMP.BOND),
		/**
		 * Indicates that a reply is not expected and the packet should not be
		 * P2P routed.
		 */
		REPLY_NOT_EXPECTED_NO_P2P(SCAMP.BOND | SCAMP.SDPF_NR),
		/** Indicates that a reply is expected. */
		REPLY_EXPECTED(SCAMP.BOND | SCAMP.SDPF_REPLY),
		/**
		 * Indicates that a reply is expected and the packet should not be P2P
		 * routed.
		 */
		REPLY_EXPECTED_NO_P2P(SCAMP.BOND | SCAMP.SDPF_REPLY | SCAMP.SDPF_NR);

		/** The SDP-encoded form of the flag. */
		public final byte value;

		private static final Map<Byte, Flag> MAP = new HashMap<>();

		Flag(int value) {
			this.value = (byte) value;
		}

		static {
			for (Flag flag : values()) {
				MAP.put(flag.value, flag);
			}
		}

		public static Flag get(byte value) {
			return MAP.get(value);
		}
	}
}
