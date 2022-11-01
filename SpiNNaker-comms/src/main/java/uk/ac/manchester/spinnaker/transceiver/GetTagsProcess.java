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
package uk.ac.manchester.spinnaker.transceiver;

import static java.util.stream.IntStream.range;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.machine.tags.ReverseIPTag;
import uk.ac.manchester.spinnaker.machine.tags.Tag;
import uk.ac.manchester.spinnaker.messages.scp.IPTagGet;
import uk.ac.manchester.spinnaker.messages.scp.IPTagGetInfo;
import uk.ac.manchester.spinnaker.transceiver.exceptions.ProcessException;

/** Gets IP tags and reverse IP tags. */
class GetTagsProcess extends TxrxProcess {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	GetTagsProcess(
			ConnectionSelector<? extends SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
	}

	/**
	 * Get the set of tags that are associated with a connection.
	 *
	 * @param connection
	 *            The connection that the tags are associated with.
	 * @return The allocated tags in ID order. Unallocated tags are absent.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	Collection<Tag> getTags(SCPConnection connection)
			throws IOException, ProcessException, InterruptedException {
		var tags = new TreeMap<Integer, Tag>();
		for (var tag : range(0, getTagCount(connection)).toArray()) {
			sendRequest(new IPTagGet(connection.getChip(), tag), response -> {
				if (response.isInUse()) {
					tags.put(tag, createTag(connection.getRemoteIPAddress(),
							tag, response));
				}
			});
		}
		finishBatch();
		return tags.values();
	}

	private int getTagCount(SCPConnection connection)
			throws IOException, ProcessException, InterruptedException {
		var tagInfo = synchronousCall(new IPTagGetInfo(connection.getChip()));
		return tagInfo.poolSize + tagInfo.fixedSize;
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

	/**
	 * Get the usage of each of the (active) tags associated with a connection.
	 *
	 * @param connection
	 *            The connection that the tags are associated with.
	 * @return A map from each active tag to the number of packets sent through
	 *         that tag.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	Map<Tag, Integer> getTagUsage(SCPConnection connection)
			throws IOException, ProcessException, InterruptedException {
		var tagUsages = new TreeMap<Tag, Integer>();
		for (var tag : range(0, getTagCount(connection)).toArray()) {
			sendRequest(new IPTagGet(connection.getChip(), tag), response -> {
				if (response.isInUse()) {
					tagUsages.put(createTag(connection.getRemoteIPAddress(),
							tag, response), response.count);
				}
			});
		}
		finishBatch();
		return tagUsages;
	}
}
