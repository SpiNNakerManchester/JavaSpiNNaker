/*
 * Copyright (c) 2018-2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.transceiver;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.scp.SCPResult;

/**
 * Encapsulates exceptions from processes which communicate with some core/chip.
 */
public class ProcessException extends SpinnmanException {
	private static final long serialVersionUID = 5198868033333540659L;

	private static final String S = "     "; // five spaces

	private static final String MSG_TEMPLATE =
			"when sending to %d:%d:%d, received exception: %s\n" + S
					+ "with message: %s";

	/** Where does the code believe this exception originated? */
	public final HasCoreLocation core;

	/**
	 * The response that cause this exception to be thrown, if known. Never
	 * {@link SCPResult#RC_OK RC_OK}; that doesn't cause exceptions! May be
	 * {@code null} if the cause was not identified as an error from SpiNNaker.
	 */
	public final SCPResult responseCode;

	private ProcessException(HasCoreLocation core, Throwable cause,
			SCPResult responseCode) {
		super(format(MSG_TEMPLATE, core.getX(), core.getY(), core.getP(),
				cause.getClass().getName(), cause.getMessage()), cause);
		this.core = core.asCoreLocation();
		this.responseCode = responseCode;
	}

	private ProcessException(HasCoreLocation core,
			UnexpectedResponseCodeException cause) {
		this(core, cause, cause.response);
	}

	/**
	 * Create an exception.
	 *
	 * @param core
	 *            What core were we talking to.
	 * @param cause
	 *            What exception caused problems.
	 * @return A process exception, or a subclass of it.
	 */
	static ProcessException makeInstance(HasCoreLocation core,
			Throwable cause) {
		if (requireNonNull(cause) instanceof UnexpectedResponseCodeException) {
			UnexpectedResponseCodeException urc =
					(UnexpectedResponseCodeException) cause;
			if (isNull(urc.response)) {
				return new ProcessException(core, cause, null);
			}
			switch (urc.response) {
			case RC_LEN:
				return new BadPacketLength(core, urc);
			case RC_SUM:
				return new BadChecksum(core, urc);
			case RC_CMD:
				return new BadCommand(core, urc);
			case RC_ARG:
				return new InvalidArguments(core, urc);
			case RC_PORT:
				return new BadSCPPort(core, urc);
			case RC_TIMEOUT:
				return new TimedOut(core, urc);
			case RC_ROUTE:
				return new NoP2PRoute(core, urc);
			case RC_CPU:
				return new BadCPUNumber(core, urc);
			case RC_DEAD:
				return new DeadDestination(core, urc);
			case RC_BUF:
				return new NoBufferAvailable(core, urc);
			case RC_P2P_NOREPLY:
				return new P2PNoReply(core, urc);
			case RC_P2P_REJECT:
				return new P2PReject(core, urc);
			case RC_P2P_BUSY:
				return new P2PBusy(core, urc);
			case RC_P2P_TIMEOUT:
				return new P2PTimedOut(core, urc);
			case RC_PKT_TX:
				return new PacketTransmissionFailed(core, urc);
			default:
				// Fall through
			}
		}
		return new ProcessException(core, cause, null);
	}

	/**
	 * Marks an exception for errors in the message by the caller. Suggested
	 * recovery strategy: don't send bad messages.
	 */
	public abstract static class CallerProcessException
			extends ProcessException {
		private static final long serialVersionUID = 1L;

		private CallerProcessException(HasCoreLocation core,
				UnexpectedResponseCodeException cause) {
			super(core, cause);
		}
	}

	/**
	 * Marks an exception for a transient condition. Suggested recovery
	 * strategy: try again in a few moments.
	 */
	public abstract static class TransientProcessException
			extends ProcessException {
		private static final long serialVersionUID = 1L;

		private TransientProcessException(HasCoreLocation core,
				UnexpectedResponseCodeException cause) {
			super(core, cause);
		}
	}

	/**
	 * Marks an exception for a permanent condition. Suggested recovery
	 * strategy: don't try! (Recovery may involve rebooting some hardware or
	 * even physically reattaching hardware; it's not going to just get better
	 * by itself.)
	 */
	public abstract static class PermanentProcessException
			extends ProcessException {
		private static final long serialVersionUID = 1L;

		private PermanentProcessException(HasCoreLocation core,
				UnexpectedResponseCodeException cause) {
			super(core, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a {@link SCPResult#RC_LEN}
	 * message, indicating that the packet length was wrong.
	 */
	public static final class BadPacketLength extends CallerProcessException {
		private static final long serialVersionUID = 4329836896716525422L;

		private BadPacketLength(HasCoreLocation core,
				UnexpectedResponseCodeException cause) {
			super(core, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a {@link SCPResult#RC_SUM}
	 * message, indicating that the checksum was wrong.
	 */
	public static final class BadChecksum extends CallerProcessException {
		private static final long serialVersionUID = -5660270018252119601L;

		private BadChecksum(HasCoreLocation core,
				UnexpectedResponseCodeException cause) {
			super(core, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a {@link SCPResult#RC_CMD}
	 * message, indicating that the command was not supported by the
	 * destination.
	 */
	public static final class BadCommand extends CallerProcessException {
		private static final long serialVersionUID = 2446636059917726286L;

		private BadCommand(HasCoreLocation core,
				UnexpectedResponseCodeException cause) {
			super(core, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a {@link SCPResult#RC_ARG}
	 * message, indicating that the arguments to the command are wrong.
	 */
	public static final class InvalidArguments extends CallerProcessException {
		private static final long serialVersionUID = 3907517289211998444L;

		private InvalidArguments(HasCoreLocation core,
				UnexpectedResponseCodeException cause) {
			super(core, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a {@link SCPResult#RC_PORT}
	 * message, indicating that the SCP port was out of range.
	 */
	public static final class BadSCPPort extends CallerProcessException {
		private static final long serialVersionUID = -5171910962257032626L;

		private BadSCPPort(HasCoreLocation core,
				UnexpectedResponseCodeException cause) {
			super(core, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a
	 * {@link SCPResult#RC_TIMEOUT} message, indicating that communications
	 * timed out.
	 */
	public static final class TimedOut extends TransientProcessException {
		private static final long serialVersionUID = -298985937364034661L;

		private TimedOut(HasCoreLocation core,
				UnexpectedResponseCodeException cause) {
			super(core, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a {@link SCPResult#RC_ROUTE}
	 * message, indicating that messages cannot be directed to that destination
	 * for some reason.
	 */
	public static final class NoP2PRoute extends PermanentProcessException {
		private static final long serialVersionUID = -6132417061161625508L;

		private NoP2PRoute(HasCoreLocation core,
				UnexpectedResponseCodeException cause) {
			super(core, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a {@link SCPResult#RC_CPU}
	 * message, indicating that the destination core number was out of range.
	 */
	public static final class BadCPUNumber extends CallerProcessException {
		private static final long serialVersionUID = 6532417803149087690L;

		private BadCPUNumber(HasCoreLocation core,
				UnexpectedResponseCodeException cause) {
			super(core, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a {@link SCPResult#RC_DEAD}
	 * message, indicating that the destination core was not responding to
	 * messages from SCAMP.
	 */
	public static final class DeadDestination
			extends PermanentProcessException {
		private static final long serialVersionUID = -3842030808096451015L;

		private DeadDestination(HasCoreLocation core,
				UnexpectedResponseCodeException cause) {
			super(core, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a {@link SCPResult#RC_BUF}
	 * message, indicating that SCAMP had exhausted its supply of buffers.
	 */
	public static final class NoBufferAvailable
			extends TransientProcessException {
		private static final long serialVersionUID = 3647501054775981197L;

		private NoBufferAvailable(HasCoreLocation core,
				UnexpectedResponseCodeException cause) {
			super(core, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a
	 * {@link SCPResult#RC_P2P_NOREPLY} message, indicating that the inter-SCAMP
	 * messaging failed because the channel open failed.
	 */
	public static final class P2PNoReply extends TransientProcessException {
		private static final long serialVersionUID = 2196366740196153289L;

		private P2PNoReply(HasCoreLocation core,
				UnexpectedResponseCodeException cause) {
			super(core, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a
	 * {@link SCPResult#RC_P2P_REJECT} message, indicating that the receiver in
	 * the inter-SCAMP messaging rejected the message.
	 */
	public static final class P2PReject extends TransientProcessException {
		private static final long serialVersionUID = -2903670314989693747L;

		private P2PReject(HasCoreLocation core,
				UnexpectedResponseCodeException cause) {
			super(core, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a
	 * {@link SCPResult#RC_P2P_BUSY} message, indicating that the receiver in
	 * the inter-SCAMP messaging was busy.
	 */
	public static final class P2PBusy extends TransientProcessException {
		private static final long serialVersionUID = 4445680981367158468L;

		private P2PBusy(HasCoreLocation core,
				UnexpectedResponseCodeException cause) {
			super(core, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a
	 * {@link SCPResult#RC_P2P_TIMEOUT} message, indicating that the receiver in
	 * the inter-SCAMP messaging did not respond.
	 */
	public static final class P2PTimedOut extends TransientProcessException {
		private static final long serialVersionUID = -7686611958418374003L;

		private P2PTimedOut(HasCoreLocation core,
				UnexpectedResponseCodeException cause) {
			super(core, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a {@link SCPResult#RC_PKT_TX}
	 * message, indicating that the packet transmission failed.
	 */
	public static final class PacketTransmissionFailed
			extends TransientProcessException {
		private static final long serialVersionUID = 5119831821960433468L;

		private PacketTransmissionFailed(HasCoreLocation core,
				UnexpectedResponseCodeException cause) {
			super(core, cause);
		}
	}
}
