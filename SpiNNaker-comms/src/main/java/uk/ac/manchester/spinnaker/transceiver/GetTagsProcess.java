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

import static java.util.stream.IntStream.range;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.tags.Tag;
import uk.ac.manchester.spinnaker.messages.scp.IPTagGet;
import uk.ac.manchester.spinnaker.messages.scp.IPTagGetInfo;

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
		for (var tagId : range(0, getTagCount(connection)).toArray()) {
			sendGet(new IPTagGet(connection.getChip(), tagId), info -> {
				if (info.inUse()) {
					tags.put(tagId, info.tag());
				}
			});
		}
		finishBatch();
		return tags.values();
	}

	private int getTagCount(SCPConnection connection)
			throws IOException, ProcessException, InterruptedException {
		var tagInfo = retrieve(new IPTagGetInfo(connection.getChip()));
		return tagInfo.poolSize() + tagInfo.fixedSize();
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
		for (var tagId : range(0, getTagCount(connection)).toArray()) {
			sendGet(new IPTagGet(connection.getChip(), tagId), info -> {
				if (info.inUse()) {
					tagUsages.put(info.tag(), info.count());
				}
			});
		}
		finishBatch();
		return tagUsages;
	}
}
