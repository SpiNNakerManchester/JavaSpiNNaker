package uk.ac.manchester.spinnaker.processes;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.List;

import javax.xml.ws.Holder;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.messages.scp.IPTagGet;
import uk.ac.manchester.spinnaker.messages.scp.IPTagGetInfo;

public class GetTagsProcess extends MultiConnectionProcess {
	public GetTagsProcess(ConnectionSelector connectionSelector) {
		super(connectionSelector);
	}

	public List<Tag> getTags(SCPConnection connection)
			throws IOException, Exception {
		Holder<IPTagGetInfo.Response> tag_info = new Holder<>();
		sendRequest(new IPTagGetInfo(connection.getChip()),
				response -> tag_info.value = response);
		finish();
		checkForError();

		int numTags = tag_info.value.poolSize + tag_info.value.fixedSize;
		List<Tag> tags = asList(new Tag[numTags]);
		for (int t = 0; t < numTags; t++) {
			final int tag = t;
			sendRequest(new IPTagGet(connection.getChip(), tag), response -> {
				if (response.isInUse()) {
					if (response.isReverse()) {
						tags.set(tag,
								new ReverseIPTag(
										connection.getRemoteIPAddress(), tag,
										response.rxPort, response.spinCore,
										response.spinPort));
					} else {
						tags.set(tag,
								new IPTag(connection.getRemoteIPAddress(),
										response.sdpHeader.getSource()
												.asChipLocation(),
										tag, response.ipAddress, response.port,
										response.isStrippingSDP()));
					}
				}
			});
		}
		finish();
		checkForError();
		return tags.stream().filter(t -> t != null).collect(toList());
	}

}
