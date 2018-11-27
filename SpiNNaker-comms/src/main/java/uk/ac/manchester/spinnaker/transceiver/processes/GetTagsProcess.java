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
package uk.ac.manchester.spinnaker.transceiver.processes;

import static java.util.stream.IntStream.range;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.machine.tags.ReverseIPTag;
import uk.ac.manchester.spinnaker.machine.tags.Tag;
import uk.ac.manchester.spinnaker.messages.scp.IPTagGet;
import uk.ac.manchester.spinnaker.messages.scp.IPTagGetInfo;
import uk.ac.manchester.spinnaker.messages.scp.IPTagGetInfo.Response;
import uk.ac.manchester.spinnaker.transceiver.RetryTracker;

/** Gets IP tags and reverse IP tags. */
public class GetTagsProcess extends MultiConnectionProcess<SCPConnection> {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	public GetTagsProcess(ConnectionSelector<SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
	}

	/**
	 * Get the set of tags that are associated with a connection.
	 *
	 * @param connection
	 *            The connection that the tags are associated with.
	 * @return A list of allocated tags in ID order. Unallocated tags are
	 *         absent.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	public List<Tag> getTags(SCPConnection connection)
			throws IOException, ProcessException {
		Response tagInfo =
				synchronousCall(new IPTagGetInfo(connection.getChip()));

		int numTags = tagInfo.poolSize + tagInfo.fixedSize;
		Map<Integer, Tag> tags = new TreeMap<>();
		for (final int tag : range(0, numTags).toArray()) {
			sendRequest(new IPTagGet(connection.getChip(), tag), response -> {
				if (response.isInUse()) {
					tags.put(tag, createTag(connection.getRemoteIPAddress(),
							tag, response));
				}
			});
		}
		finish();
		checkForError();
		return new ArrayList<>(tags.values());
	}

	private static Tag createTag(InetAddress host, int tag,
			IPTagGet.Response res) {
		if (res.isReverse()) {
			return new ReverseIPTag(host, tag, res.rxPort, res.spinCore,
					res.spinPort);
		} else {
			return new IPTag(host, res.sdpHeader.getSource().asChipLocation(),
					tag, res.ipAddress, res.port, res.isStrippingSDP());
		}
	}

}
