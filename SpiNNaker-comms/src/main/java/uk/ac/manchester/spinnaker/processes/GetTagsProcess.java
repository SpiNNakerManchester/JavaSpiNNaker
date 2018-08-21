package uk.ac.manchester.spinnaker.processes;

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

/** Gets IP tags and reverse IP tags. */
public class GetTagsProcess extends MultiConnectionProcess<SCPConnection> {
	/**
	 * @param connectionSelector
	 *            How to select how to communicate.
	 */
	public GetTagsProcess(
			ConnectionSelector<SCPConnection> connectionSelector) {
		super(connectionSelector);
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
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	public List<Tag> getTags(SCPConnection connection)
			throws IOException, Exception {
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
