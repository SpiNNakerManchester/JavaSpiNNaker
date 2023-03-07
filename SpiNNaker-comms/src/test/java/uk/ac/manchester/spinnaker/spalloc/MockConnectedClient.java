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
package uk.ac.manchester.spinnaker.spalloc;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;

import com.google.errorprone.annotations.MustBeClosed;

import uk.ac.manchester.spinnaker.spalloc.messages.Command;
import uk.ac.manchester.spinnaker.spalloc.messages.CreateJobCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.GetBoardAtPositionCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.GetBoardPositionCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.GetJobMachineInfoCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.GetJobStateCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.ListJobsCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.ListMachinesCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.VersionCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.WhereIsJobChipCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.WhereIsMachineBoardLogicalCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.WhereIsMachineBoardPhysicalCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.WhereIsMachineChipCommand;

/**
 * A possibly Mock Client that if it can not create an actual connection
 *      provides a number of canned replies.
 *
 * @author Christian
 */
public class MockConnectedClient extends SpallocClient {
	private static final Logger log = getLogger(MockConnectedClient.class);

	private static final String LIST_JOBS_R = "["
			+ "{\"job_id\":47224,"
			+ "\"owner\":\"someone@manchester.ac.uk\","
			+ "\"start_time\":1.536925243666607E9,"
			+ "\"keepalive\":60.0,"
			+ "\"state\":3,"
			+ "\"power\":true,"
			+ "\"args\":[1],"
			+ "\"kwargs\":{\"tags\":null,"
			+ "\"max_dead_boards\":0,"
			+ "\"machine\":null,"
			+ "\"min_ratio\":0.333,"
			+ "\"max_dead_links\":null,"
			+ "\"require_torus\":false},"
			+ "\"allocated_machine_name\":\"Spin24b-223\","
			+ "\"boards\":[[1,1,2]],"
			+ "\"keepalivehost\":\"130.88.198.171\"},"
			+ "{\"job_id\":47444,"
			+ "\"owner\":\"another.person@manchester.ac.uk\","
			+ "\"start_time\":1.537098968439959E9,"
			+ "\"keepalive\":60.0,"
			+ "\"state\":3,"
			+ "\"power\":true,"
			+ "\"args\":[1],"
			+ "\"kwargs\":{\"tags\":null,"
			+ "\"max_dead_boards\":0,"
			+ "\"machine\":null,"
			+ "\"min_ratio\":0.333,"
			+ "\"max_dead_links\":null,"
			+ "\"require_torus\":false},"
			+ "\"allocated_machine_name\":\"Spin24b-223\","
			+ "\"boards\":[[2,0,2]],"
			+ "\"keepalivehost\":\"130.88.198.171\"}]";

	private static final String LIST_MACHINE_R = "["
			+ "{\"name\":\"Spin24b-223\","
			+ "\"tags\":[\"default\",\"machine-room\"],"
			+ "\"width\":4,"
			+ "\"height\":2,"
			+ "\"dead_boards\":[],"
			+ "\"dead_links\":[]},"
			+ "	{\"name\":\"Spin24b-225\","
			+ "\"tags\":[\"default\",\"machine-room\"],"
			+ "\"width\":4,"
			+ "\"height\":2,"
			+ "\"dead_boards\":[],"
			+ "\"dead_links\":[]},"
			+ "{\"name\":\"Spin24b-226\","
			+ "\"tags\":[\"default\","
			+ "\"machine-room\"],"
			+ "\"width\":4,"
			+ "\"height\":2,"
			+ "\"dead_boards\":[],"
			+ "\"dead_links\":[]}"
			+ "]";

	/** ID used by mock job. */
	public static final int MOCK_ID = 9999;

	private static final String JOB_MACHINE_INFO_R =
			"{\"connections\":[[[0,0],\"10.11.223.33\"]],"
					+ "\"width\":8,"
					+ "\"machine_name\":\"Spin24b-223\","
					+ "\"boards\":[[0,0,2]],"
					+ "\"height\":8}";

	private static final String STATE_POWER_R =
			"{\"state\":2,"
					+ "\"power\":true,"
					+ "\"keepalive\":60.0,"
					+ "\"reason\":null,"
					+ "\"start_time\":1.537284307847865E9,"
					+ "\"keepalivehost\":\"86.82.216.229\"}";

	private static final String WHERE_IS_R =
			"{\"job_chip\":[1,1],"
					+ "\"job_id\":9999,"
					+ "\"chip\":[5,9],"
					+ "\"logical\":[0,0,2],"
					+ "\"machine\":\"Spin24b-223\","
					+ "\"board_chip\":[1,1],"
					+ "\"physical\":[0,0,4]}";

	private static final String POSITION_R = "[0,0,8]";

	private static final String AT_R = "[0,0,1]";

	/**
	 * The version as it comes from spalloc.
	 *
	 * Note the quotes in the String are as being returned by spalloc.
	 */
	static final String VERSION = "\"1.0.0\"";

	private static final Map<Class<?>, String> RESPONSES = Map.ofEntries(
			Map.entry(ListJobsCommand.class, LIST_JOBS_R),
			Map.entry(ListMachinesCommand.class, LIST_MACHINE_R),
			Map.entry(CreateJobCommand.class, "" + MOCK_ID),
			Map.entry(GetJobMachineInfoCommand.class, JOB_MACHINE_INFO_R),
			Map.entry(GetJobStateCommand.class, STATE_POWER_R),
			Map.entry(WhereIsJobChipCommand.class, WHERE_IS_R),
			Map.entry(WhereIsMachineBoardLogicalCommand.class, WHERE_IS_R),
			Map.entry(WhereIsMachineBoardPhysicalCommand.class, WHERE_IS_R),
			Map.entry(WhereIsMachineChipCommand.class, WHERE_IS_R),
			Map.entry(VersionCommand.class, VERSION),
			Map.entry(GetBoardPositionCommand.class, POSITION_R),
			Map.entry(GetBoardAtPositionCommand.class, AT_R));

	private boolean actual;

	private static final int PORT = 22245;

	@MustBeClosed
	public MockConnectedClient(int timeout) {
		// Main
		//super("spinnaker.cs.man.ac.uk", 22244, timeout);
		// Spin2
		super("spinnaker.cs.man.ac.uk", PORT, timeout);
		// Bad
		// super("127.0.0.0", 22244, timeout);
		actual = true;
	}

	@Override
	public void connect(Integer timeout) throws IOException {
		if (actual) {
			try {
				super.connect(timeout);
				actual = true;
			} catch (Exception ex) {
				actual = false;
				log.warn("Connect fail using mock", ex);
			}
		}
	}

	@Override
	protected String call(Command<?> command, Integer timeout) {
		if (actual) {
			try {
				return super.call(command, timeout);
			} catch (Exception ex) {
				actual = false;
				log.warn("Call fail using mock", ex);
			}
		}
		return RESPONSES.get(command.getClass());
	}

	/**
	 * Specifies if an actual connection is being used.
	 *
	 * @return True if real replies are being obtained, False if canned replies
	 *         are being used.
	 */
	public boolean isActual() {
		return actual;
	}

}
