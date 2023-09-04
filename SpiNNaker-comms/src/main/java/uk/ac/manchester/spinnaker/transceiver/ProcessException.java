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
package uk.ac.manchester.spinnaker.transceiver;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;
import uk.ac.manchester.spinnaker.messages.scp.SCPResult;
import uk.ac.manchester.spinnaker.messages.sdp.SDPLocation;

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
	public final SDPLocation source;

	/**
	 * The response that cause this exception to be thrown, if known. Never
	 * {@link SCPResult#RC_OK RC_OK}; that doesn't cause exceptions! May be
	 * {@code null} if the cause was not identified as an error from SpiNNaker.
	 */
	public final SCPResult responseCode;

	private ProcessException(SDPLocation source, Throwable cause,
			SCPResult responseCode) {
		super(format(MSG_TEMPLATE, source.getX(), source.getY(), source.getP(),
				cause.getClass().getName(), cause.getMessage()), cause);
		this.source = source;
		this.responseCode = responseCode;
	}

	private ProcessException(SDPLocation source,
			UnexpectedResponseCodeException cause) {
		this(source, cause, cause.response);
	}

	/**
	 * Create an exception.
	 *
	 * @param source
	 *            What core were we talking to.
	 * @param cause
	 *            What exception caused problems.
	 * @return A process exception, or a subclass of it.
	 * @throws InterruptedException
	 *             If the cause was an interrupt, it is immediately rethrown.
	 */
	static ProcessException makeInstance(SDPLocation source,
			Throwable cause) throws InterruptedException {
		if (requireNonNull(cause) instanceof UnexpectedResponseCodeException) {
			var urc = (UnexpectedResponseCodeException) cause;
			if (urc.response == null) {
				return new ProcessException(source, cause, null);
			}
			switch (urc.response) {
			case RC_LEN:
				return new BadPacketLength(source, urc);
			case RC_SUM:
				return new BadChecksum(source, urc);
			case RC_CMD:
				return new BadCommand(source, urc);
			case RC_ARG:
				return new InvalidArguments(source, urc);
			case RC_PORT:
				return new BadSCPPort(source, urc);
			case RC_TIMEOUT:
				return new TimedOut(source, urc);
			case RC_ROUTE:
				return new NoP2PRoute(source, urc);
			case RC_CPU:
				return new BadCPUNumber(source, urc);
			case RC_DEAD:
				return new DeadDestination(source, urc);
			case RC_BUF:
				return new NoBufferAvailable(source, urc);
			case RC_P2P_NOREPLY:
				return new P2PNoReply(source, urc);
			case RC_P2P_REJECT:
				return new P2PReject(source, urc);
			case RC_P2P_BUSY:
				return new P2PBusy(source, urc);
			case RC_P2P_TIMEOUT:
				return new P2PTimedOut(source, urc);
			case RC_PKT_TX:
				return new PacketTransmissionFailed(source, urc);
			default:
				// Fall through
			}
		}
		if (cause instanceof InterruptedException) {
			throw (InterruptedException) cause;
		}
		return new ProcessException(source, cause, null);
	}

	/**
	 * Marks an exception for errors in the message by the caller. Suggested
	 * recovery strategy: don't send bad messages.
	 */
	public abstract static class CallerProcessException
			extends ProcessException {
		private static final long serialVersionUID = 1L;

		private CallerProcessException(SDPLocation source,
				UnexpectedResponseCodeException cause) {
			super(source, cause);
		}
	}

	/**
	 * Marks an exception for a transient condition. Suggested recovery
	 * strategy: try again in a few moments.
	 */
	public abstract static class TransientProcessException
			extends ProcessException {
		private static final long serialVersionUID = 1L;

		private TransientProcessException(SDPLocation source,
				UnexpectedResponseCodeException cause) {
			super(source, cause);
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

		private PermanentProcessException(SDPLocation source,
				UnexpectedResponseCodeException cause) {
			super(source, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a {@link SCPResult#RC_LEN}
	 * message, indicating that the packet length was wrong.
	 */
	public static final class BadPacketLength extends CallerProcessException {
		private static final long serialVersionUID = 4329836896716525422L;

		private BadPacketLength(SDPLocation source,
				UnexpectedResponseCodeException cause) {
			super(source, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a {@link SCPResult#RC_SUM}
	 * message, indicating that the checksum was wrong.
	 */
	public static final class BadChecksum extends CallerProcessException {
		private static final long serialVersionUID = -5660270018252119601L;

		private BadChecksum(SDPLocation source,
				UnexpectedResponseCodeException cause) {
			super(source, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a {@link SCPResult#RC_CMD}
	 * message, indicating that the command was not supported by the
	 * destination.
	 */
	public static final class BadCommand extends CallerProcessException {
		private static final long serialVersionUID = 2446636059917726286L;

		private BadCommand(SDPLocation source,
				UnexpectedResponseCodeException cause) {
			super(source, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a {@link SCPResult#RC_ARG}
	 * message, indicating that the arguments to the command are wrong.
	 */
	public static final class InvalidArguments extends CallerProcessException {
		private static final long serialVersionUID = 3907517289211998444L;

		private InvalidArguments(SDPLocation source,
				UnexpectedResponseCodeException cause) {
			super(source, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a {@link SCPResult#RC_PORT}
	 * message, indicating that the SCP port was out of range.
	 */
	public static final class BadSCPPort extends CallerProcessException {
		private static final long serialVersionUID = -5171910962257032626L;

		private BadSCPPort(SDPLocation source,
				UnexpectedResponseCodeException cause) {
			super(source, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a
	 * {@link SCPResult#RC_TIMEOUT} message, indicating that communications
	 * timed out.
	 */
	public static final class TimedOut extends TransientProcessException {
		private static final long serialVersionUID = -298985937364034661L;

		private TimedOut(SDPLocation source,
				UnexpectedResponseCodeException cause) {
			super(source, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a {@link SCPResult#RC_ROUTE}
	 * message, indicating that messages cannot be directed to that destination
	 * for some reason.
	 */
	public static final class NoP2PRoute extends PermanentProcessException {
		private static final long serialVersionUID = -6132417061161625508L;

		private NoP2PRoute(SDPLocation source,
				UnexpectedResponseCodeException cause) {
			super(source, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a {@link SCPResult#RC_CPU}
	 * message, indicating that the destination core number was out of range.
	 */
	public static final class BadCPUNumber extends CallerProcessException {
		private static final long serialVersionUID = 6532417803149087690L;

		private BadCPUNumber(SDPLocation source,
				UnexpectedResponseCodeException cause) {
			super(source, cause);
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

		private DeadDestination(SDPLocation source,
				UnexpectedResponseCodeException cause) {
			super(source, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a {@link SCPResult#RC_BUF}
	 * message, indicating that SCAMP had exhausted its supply of buffers.
	 */
	public static final class NoBufferAvailable
			extends TransientProcessException {
		private static final long serialVersionUID = 3647501054775981197L;

		private NoBufferAvailable(SDPLocation source,
				UnexpectedResponseCodeException cause) {
			super(source, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a
	 * {@link SCPResult#RC_P2P_NOREPLY} message, indicating that the inter-SCAMP
	 * messaging failed because the channel open failed.
	 */
	public static final class P2PNoReply extends TransientProcessException {
		private static final long serialVersionUID = 2196366740196153289L;

		private P2PNoReply(SDPLocation source,
				UnexpectedResponseCodeException cause) {
			super(source, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a
	 * {@link SCPResult#RC_P2P_REJECT} message, indicating that the receiver in
	 * the inter-SCAMP messaging rejected the message.
	 */
	public static final class P2PReject extends TransientProcessException {
		private static final long serialVersionUID = -2903670314989693747L;

		private P2PReject(SDPLocation source,
				UnexpectedResponseCodeException cause) {
			super(source, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a
	 * {@link SCPResult#RC_P2P_BUSY} message, indicating that the receiver in
	 * the inter-SCAMP messaging was busy.
	 */
	public static final class P2PBusy extends TransientProcessException {
		private static final long serialVersionUID = 4445680981367158468L;

		private P2PBusy(SDPLocation source,
				UnexpectedResponseCodeException cause) {
			super(source, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a
	 * {@link SCPResult#RC_P2P_TIMEOUT} message, indicating that the receiver in
	 * the inter-SCAMP messaging did not respond.
	 */
	public static final class P2PTimedOut extends TransientProcessException {
		private static final long serialVersionUID = -7686611958418374003L;

		private P2PTimedOut(SDPLocation source,
				UnexpectedResponseCodeException cause) {
			super(source, cause);
		}
	}

	/**
	 * A process exception cause by the receipt of a {@link SCPResult#RC_PKT_TX}
	 * message, indicating that the packet transmission failed.
	 */
	public static final class PacketTransmissionFailed
			extends TransientProcessException {
		private static final long serialVersionUID = 5119831821960433468L;

		private PacketTransmissionFailed(SDPLocation source,
				UnexpectedResponseCodeException cause) {
			super(source, cause);
		}
	}
}
