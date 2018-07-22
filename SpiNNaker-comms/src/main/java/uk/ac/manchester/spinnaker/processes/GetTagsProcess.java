package uk.ac.manchester.spinnaker.processes;

import static java.util.stream.IntStream.range;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.machine.tags.ReverseIPTag;
import uk.ac.manchester.spinnaker.machine.tags.Tag;
import uk.ac.manchester.spinnaker.messages.scp.IPTagGet;
import uk.ac.manchester.spinnaker.messages.scp.IPTagGetInfo;
import uk.ac.manchester.spinnaker.messages.scp.IPTagGetInfo.Response;

public class GetTagsProcess extends MultiConnectionProcess {
	public GetTagsProcess(ConnectionSelector connectionSelector) {
		super(connectionSelector);
	}

	public List<Tag> getTags(SCPConnection connection)
			throws IOException, Exception {
		Response tag_info = synchronousCall(
				new IPTagGetInfo(connection.getChip()));

		int numTags = tag_info.poolSize + tag_info.fixedSize;
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
