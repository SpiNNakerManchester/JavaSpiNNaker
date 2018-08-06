package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.model.IPTagCommand.GET;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.COMMAND_FIELD;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.CORE_MASK;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.PORT_SHIFT;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.THREE_BITS_MASK;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_IPTAG;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.IPTagTimeOutWaitTime;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/** An SCP Request to get an IP tag. */
public class IPTagGet extends SCPRequest<IPTagGet.Response> {
	private static int argument1(int tagID) {
		return (GET.value << COMMAND_FIELD) | (tagID & THREE_BITS_MASK);
	}

	/**
	 * @param chip
	 *            The chip to get the tag from.
	 * @param tag
	 *            The tag to get the details of.
	 */
	public IPTagGet(HasChipLocation chip, int tag) {
		super(chip.getScampCore(), CMD_IPTAG, argument1(tag), 1, null);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** An SCP response to a request for an IP tags. */
	public static final class Response extends CheckOKResponse {
		/**
		 * The count of the number of packets that have been sent with the tag.
		 */
		public final int count;
		/** The flags of the tag. */
		public final short flags;
		/** The IP address of the tag. */
		public final InetAddress ipAddress;
		/** The MAC address of the tag, as an array of 6 bytes. */
		public final byte[] macAddress;
		/** The port of the tag. */
		public final int port;
		/** The receive port of the tag. */
		public final short rxPort;
		/**
		 * The location of the core on the chip which the tag is defined on and
		 * where the core that handles the tag's messages resides.
		 */
		public final HasCoreLocation spinCore;
		/** The spin-port of the IP tag. */
		public final int spinPort;
		/** The timeout of the tag. */
		public final IPTagTimeOutWaitTime timeout;

		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException, UnknownHostException {
			super("Get IP Tag Info", CMD_IPTAG, buffer);

			byte[] ipBytes = new byte[IPV4_BYTES];
			buffer.get(ipBytes);
			ipAddress = InetAddress.getByAddress(ipBytes);

			macAddress = new byte[MAC_BYTES];
			buffer.get(macAddress);

			port = buffer.getShort();
			timeout = IPTagTimeOutWaitTime.get(buffer.getShort());
			flags = buffer.getShort();
			count = buffer.getInt();
			rxPort = buffer.getShort();
			int y = Byte.toUnsignedInt(buffer.get());
			int x = Byte.toUnsignedInt(buffer.get());
			int pp = Byte.toUnsignedInt(buffer.get());
			spinCore = new CoreLocation(x, y, pp & CORE_MASK);
			spinPort = (pp >>> PORT_SHIFT) & THREE_BITS_MASK;
		}

		private static final int IPV4_BYTES = 4;
		private static final int MAC_BYTES = 6;
		private static final int USE_BIT = 15;
		private static final int TEMP_BIT = 14;
		private static final int ARP_BIT = 13;
		private static final int REV_BIT = 9;
		private static final int STRIP_BIT = 8;

		private boolean bitset(int bit) {
			return (flags & (1 << bit)) != 0;
		}

		/** @return True if the tag is marked as being in use. */
		public boolean isInUse() {
			return bitset(USE_BIT);
		}

		/** @return True if the tag is temporary. */
		public boolean isTemporary() {
			return bitset(TEMP_BIT);
		}

		/**
		 * @return True if the tag is in the ARP state (where the MAC address is
		 *         being looked up; this is a transient state so unlikely).
		 */
		public boolean isARP() {
			return bitset(ARP_BIT);
		}

		/** @return True if the tag is a reverse tag. */
		public boolean isReverse() {
			return bitset(REV_BIT);
		}

		/** @return True if the tag is to strip the SDP header. */
		public boolean isStrippingSDP() {
			return bitset(STRIP_BIT);
		}
	}
}
