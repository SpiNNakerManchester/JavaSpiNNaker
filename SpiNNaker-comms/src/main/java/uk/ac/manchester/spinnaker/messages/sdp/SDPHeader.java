package uk.ac.manchester.spinnaker.messages.sdp;

import java.nio.ByteBuffer;

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
	private SDPFlag flags;
	private byte tag;

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
	 */
	public SDPHeader(SDPFlag flags, HasCoreLocation destination,
			int destinationPort) {
		this.flags = flags;
		this.destination = destination;
		this.destinationPort = destinationPort;
	}

	/**
	 * Read the header from an input buffer.
	 */
	public SDPHeader(ByteBuffer buffer) {
		flags = SDPFlag.get(buffer.get());
		tag = buffer.get();
		byte dpc = buffer.get();
		byte spc = buffer.get();
		byte dcy = buffer.get();
		byte dcx = buffer.get();
		byte scy = buffer.get();
		byte scx = buffer.get();
		destinationPort = (dpc >> CPU_ADDR_BITS) & PORT_MASK;
		sourcePort = (spc >> CPU_ADDR_BITS) & PORT_MASK;
		destination = new CoreLocation(dcx, dcy, dpc & CPU_MASK);
		source = new CoreLocation(scx, scy, spc & CPU_MASK);
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		int dpc = ((destinationPort & PORT_MASK) << CPU_ADDR_BITS)
				| (destination.getP() & CPU_MASK);
		int spc = ((sourcePort & PORT_MASK) << CPU_ADDR_BITS)
				| (source.getP() & CPU_MASK);

		buffer.put(flags.value);
		buffer.put(tag);
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

	public void setDestinationPort(SDPPort port) {
		this.destinationPort = port.value;
	}

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

	public void setSourcePort(SDPPort port) {
		this.sourcePort = port.value;
	}

	public void setSourcePort(int port) {
		if (port < 0 || port > MAX_PORT) {
			throw new IllegalArgumentException("port out of range");
		}
		this.sourcePort = port;
	}

	public SDPFlag getFlags() {
		return flags;
	}

	public void setFlags(SDPFlag flags) {
		this.flags = flags;
	}

	public byte getTag() {
		return tag;
	}

	public void setTag(byte tag) {
		this.tag = tag;
	}
}
