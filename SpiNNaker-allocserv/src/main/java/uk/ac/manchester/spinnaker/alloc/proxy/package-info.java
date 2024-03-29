/*
 * Copyright (c) 2022 The University of Manchester
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
/**
 * SpiNNaker message proxying support code. The protocol uses binary message
 * frames within a websocket. That websocket will have been opened within an
 * HTTPS-encrypted session where the session's user was authorised to access the
 * particular job that is the context for the websocket. Messages sent via the
 * protocol can only ever access boards allocated to the job.
 * <p>
 * The protocol has five messages that clients can send, three of which have
 * replies and one of which can also be sent from the server/proxy to the
 * client. All messages consist of a sequence of little-endian words, possibly
 * followed by some payload data (of length limited by the maximum size of a
 * SpiNNaker SDP or SpiNNaker boot message; the framing does not itself impose a
 * maximum message length):
 * </p>
 * <table border="1" class="protocol">
 * <caption style="display:none">Protocol inside web socket</caption>
 * <tr>
 * <th>Name</th>
 * <th>Request Layout (words)</th>
 * <th>Response Layout (words)</th>
 * </tr>
 * <tr>
 * <td rowspan=2>{@linkplain ProxyCore#openConnectedChannel(ByteBuffer) Open
 * Connected Channel}</td>
 * <td>
 * <table border="1" class="protocolrequest">
 * <caption style="display:none">Request</caption>
 * <tr>
 * <td>{@link ProxyOp#OPEN 0}
 * <td>Correlation&nbsp;ID
 * <td>Chip&nbsp;X
 * <td>Chip&nbsp;Y
 * <td>UDP&nbsp;Port on Chip
 * </tr>
 * </table>
 * </td>
 * <td>
 * <table border="1" class="protocolresponse">
 * <caption style="display:none">Response</caption>
 * <tr>
 * <td>{@link ProxyOp#OPEN 0}
 * <td>Correlation&nbsp;ID
 * <td>Channel&nbsp;ID
 * </tr>
 * </table>
 * </td>
 * </tr>
 * <tr>
 * <td colspan=2>Establish a UDP socket that will talk to the given Ethernet
 * chip within the allocation. Returns an ID that can be used to refer to that
 * channel. Note that opening a socket declares that you are prepared to receive
 * messages from SpiNNaker on it, but does not mean that SpiNNaker will send any
 * messages that way. The correlation ID is caller-nominated, and just passed
 * back uninterpreted in the response message.</td>
 * </tr>
 * <tr>
 * <td rowspan=2>{@linkplain ProxyCore#closeChannel(ByteBuffer) Close
 * Channel}</td>
 * <td>
 * <table border="1" class="protocolrequest">
 * <caption style="display:none">Request</caption>
 * <tr>
 * <td>{@link ProxyOp#CLOSE 1}
 * <td>Correlation&nbsp;ID
 * <td>Channel&nbsp;ID
 * </tr>
 * </table>
 * </td>
 * <td>
 * <table border="1" class="protocolresponse">
 * <caption style="display:none">Response</caption>
 * <tr>
 * <td>{@link ProxyOp#CLOSE 1}
 * <td>Correlation&nbsp;ID
 * <td>Channel&nbsp;ID (if closed) or {@code 0} (not closed)
 * </tr>
 * </table>
 * </td>
 * </tr>
 * <tr>
 * <td colspan=2>Close an established UDP socket, given its ID. Returns the ID
 * on success, and zero on failure (e.g., because the socket is already closed).
 * The correlation ID is caller-nominated, and just passed back uninterpreted in
 * the response message. The channel may have been opened with either <em>Open
 * Connected Channel</em> or <em>Open Unconnected Channel</em>.</td>
 * </tr>
 * <tr>
 * <td rowspan=2>{@linkplain ProxyCore#sendMessage(ByteBuffer) Send
 * Message}</td>
 * <td>
 * <table border="1" class="protocolrequest">
 * <caption style="display:none">Request</caption>
 * <tr>
 * <td>{@link ProxyOp#MESSAGE 2}
 * <td>Channel&nbsp;ID
 * <td>Raw&nbsp;message&nbsp;bytes...
 * </tr>
 * </table>
 * </td>
 * <td>N/A</td>
 * </tr>
 * <tr>
 * <td colspan=2>Send a message to SpiNNaker on a particular established UDP
 * configuration. This is technically one-way, but messages come back in the
 * same format (i.e., a 4 byte prefix to say that it is a message, and another 4
 * bytes to say what socket this is talking about). The raw message bytes
 * (<em>including</em> the half-word of ethernet frame padding) follow the
 * header. Messages sent on connections opened with <em>Open Unconnected
 * Channel</em> will be ignored.</td>
 * </tr>
 * <tr>
 * <td rowspan=2>{@linkplain ProxyCore#openUnconnectedChannel(ByteBuffer) Open
 * Unconnected Channel}</td>
 * <td>
 * <table border="1" class="protocolrequest">
 * <caption style="display:none">Request</caption>
 * <tr>
 * <td>{@link ProxyOp#OPEN_UNCONNECTED 3}
 * <td>Correlation&nbsp;ID
 * </tr>
 * </table>
 * </td>
 * <td>
 * <table border="1" class="protocolresponse">
 * <caption style="display:none">Response</caption>
 * <tr>
 * <td>{@link ProxyOp#OPEN_UNCONNECTED 3}
 * <td>Correlation&nbsp;ID
 * <td>Channel&nbsp;ID
 * <td>IP&nbsp;Address
 * <td>UDP&nbsp;Port on Server
 * </tr>
 * </table>
 * </td>
 * </tr>
 * <tr>
 * <td colspan=2>Establish a UDP socket that will receive from the allocation.
 * Returns an ID that can be used to refer to that channel. Note that opening a
 * socket declares that you are prepared to receive messages from SpiNNaker on
 * it, but does not mean that SpiNNaker will send any messages that way. The
 * correlation ID is caller-nominated, and just passed back uninterpreted in the
 * response message. Also included in the response message is the IPv4 address
 * (big-endian binary encoding; one word) and server UDP port for the
 * connection, allowing the client to instruct SpiNNaker to send messages to the
 * socket on the server side of the channel (which is not necessarily accessible
 * from anything other than SpiNNaker). No guarantee is made about whether any
 * message from anything other than a board in the job will be passed on.
 * Sending on the channel will only be possible with the <em>Send Message
 * To</em> operation.</td>
 * </tr>
 * <tr>
 * <td rowspan=2>{@linkplain ProxyCore#sendMessageTo(ByteBuffer) Send Message
 * To}</td>
 * <td>
 * <table border="1" class="protocolrequest">
 * <caption style="display:none">Request</caption>
 * <tr>
 * <td>{@link ProxyOp#MESSAGE_TO 4}
 * <td>Channel&nbsp;ID
 * <td>Chip&nbsp;X
 * <td>Chip&nbsp;Y
 * <td>UDP&nbsp;Port on Chip
 * <td>Raw&nbsp;message&nbsp;bytes...
 * </tr>
 * </table>
 * </td>
 * <td>N/A</td>
 * </tr>
 * <tr>
 * <td colspan=2>Send a message to a SpiNNaker board (identified by coordinates
 * of its ethernet chip) to a given UDP port. This is one-way. The raw message
 * bytes (<em>including</em> the half-word of ethernet frame padding) follow the
 * header. The channel must have been opened with <em>Open Unconnected
 * Channel</em>. Any responses come back as standard messages; if doing calls
 * with this, it is advised to only have one in flight at a time.</td>
 * </tr>
 * <tr>
 * <td rowspan=2>Error</td>
 * <td>N/A</td>
 * <td>
 * <table border="1" class="protocolresponse">
 * <caption style="display:none">Response</caption>
 * <tr>
 * <td>{@link ProxyOp#ERROR 5}
 * <td>Correlation&nbsp;ID
 * <td>UTF-8&nbsp;error&nbsp;message&nbsp;bytes...
 * </tr>
 * </table>
 * </td>
 * </tr>
 * <tr>
 * <td colspan=2>A response that may be given to <em>Open Connected
 * Channel</em>, <em>Close Channel</em>, or <em>Open Unconnected Channel</em>.
 * This indicates that a failure to perform an operation occurred. The
 * correlation ID is whatever ID was provided in the request message that this
 * is a response to. The remainder of the message is UTF-8 text that provides
 * some human-readable description of what the problem was, if the server
 * chooses to provide one.
 * Note that the server is never required to send this message; it may summarily
 * close the websocket instead.</td>
 * </tr>
 * </table>
 * <p>
 * <em>Protocol-level errors</em> (such as not following the protocol!) may be
 * responded to by the server arbitrarily closing the websocket, in which case
 * any proxied connections it has open through the websocket will also be
 * closed.
 */
package uk.ac.manchester.spinnaker.alloc.proxy;

import java.nio.ByteBuffer;
