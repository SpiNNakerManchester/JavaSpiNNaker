package uk.ac.manchester.spinnaker.messages.scp;

import static uk.ac.manchester.spinnaker.messages.model.IPTagCommand.GET;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_IPTAG;
import static uk.ac.manchester.spinnaker.messages.sdp.SDPFlag.REPLY_EXPECTED;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.IPTagTimeOutWaitTime;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;

/** An SCP Request to get an IP tag */
public class IPTagGet extends SCPRequest<IPTagGet.Response> {
	private static int argument1(int tagID) {
		return (GET.value << 16) | (tagID & 0x7);
	}

	/**
	 * @param chip
	 *            The chip to get the tag from.
	 * @param tag
	 *            The tag to get the details of.
	 */
	public IPTagGet(HasChipLocation chip, int tag) {
		super(new SDPHeader(REPLY_EXPECTED, chip.getScampCore(), 0),
				new SCPRequestHeader(CMD_IPTAG), argument1(tag), 1, null);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** An SCP response to a request for an IP tags */
	public static class Response extends CheckOKResponse {
		/**
		 * The count of the number of packets that have been sent with the tag.
		 */
		public final int count;
		/** The flags of the tag */
		public final short flags;
		/** The IP address of the tag */
		public final InetAddress ipAddress;
		/** The MAC address of the tag, as an array of 6 bytes */
		public final byte[] macAddress;
		/** The port of the tag */
		public final int port;
		/** The receive port of the tag */
		public final short rxPort;
		/**
		 * The location of the core on the chip which the tag is defined on and
		 * where the core that handles the tag's messages resides.
		 */
		public final HasCoreLocation spinCore;
		/** The spin-port of the IP tag */
		public final int spinPort;
		/** The timeout of the tag */
		public final IPTagTimeOutWaitTime timeout;

		public Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException, UnknownHostException {
			super("Get IP Tag Info", CMD_IPTAG, buffer);

			byte[] ip_address = new byte[4];
			buffer.get(ip_address);
			ipAddress = InetAddress.getByAddress(ip_address);

			macAddress = new byte[6];
			buffer.get(macAddress);

			port = buffer.getShort();
			timeout = IPTagTimeOutWaitTime.get(buffer.getShort());
			flags = buffer.getShort();
			count = buffer.getInt();
			rxPort = buffer.getShort();
			byte y = buffer.get();
			byte x = buffer.get();
			byte pp = buffer.get();
			spinCore = new CoreLocation(x, y, pp & 0x1F);
			spinPort = (pp >>> 5) & 0x7;
		}

		/** True if the tag is marked as being in use */
		public boolean isInUse() {
			return (flags & 0x8000) > 0;
		}

		/** True if the tag is temporary */
		public boolean isTemporary() {
			return (flags & 0x4000) > 0;
		}

		/**
		 * True if the tag is in the ARP state (where the MAC address is being
		 * looked up; this is a transient state so unlikely).
		 */
		public boolean isARP() {
			return (flags & 0x2000) > 0;
		}

		/** True if the tag is a reverse tag */
		public boolean isReverse() {
			return (flags & 0x0200) > 0;
		}

		/** True if the tag is to strip the SDP header */
		public boolean isStrippingSDP() {
			return (flags & 0x0100) > 0;
		}
	}
}
