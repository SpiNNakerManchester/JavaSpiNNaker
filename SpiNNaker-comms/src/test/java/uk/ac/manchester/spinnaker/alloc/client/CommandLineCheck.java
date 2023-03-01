/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.client;

import static java.util.stream.Collectors.toList;
import static uk.ac.manchester.spinnaker.machine.ChipLocation.ZERO_ZERO;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import picocli.CommandLine;
import picocli.CommandLine.Parameters;
import uk.ac.manchester.spinnaker.alloc.client.SpallocClient.Machine;
import uk.ac.manchester.spinnaker.machine.board.TriadCoords;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;

public final class CommandLineCheck {
	@Parameters(index = "0", paramLabel = "BaseURL")
	private URI baseUrl;

	@Parameters(index = "1", paramLabel = "UserName")
	private String username;

	@Parameters(index = "2", paramLabel = "PassWord")
	private String password;

	private CommandLineCheck(String[] args) {
		new CommandLine(this).parseArgs(args);
	}

	private static final int SHORT_SLEEP = 100;

	public static void checkTheClient(SpallocClient client) throws IOException {
		System.out.println(client.getVersion());
		System.out.println(client.listMachines().stream()
				.map(Machine::getName).collect(toList()));
		for (var m : client.listMachines()) {
			var where = m.getBoard(new TriadCoords(0, 0, 1));
			if (where == null) {
				System.err.println(
						"board (0,0,1) not in machine " + m.getName());
				continue;
			}
			System.out.println(where.getMachineHandle().getWidth());
			System.out.println(where.getLogicalCoords());
			System.out.println(where.getPhysicalCoords());
			System.out.println(where.getBoardChip());
			System.out.println(where.getChip());
			System.out.println(where.getJobId());
		}
	}

	public static void checkAJob(SpallocClient client)
			throws IOException, InterruptedException, SpinnmanException {
		var cj = new CreateJob();
		cj.setTags(List.of("default"));
		var job = client.createJob(cj);
		System.out.println("allocated " + job);
		try {
			job.waitForPower();
			var where = job.whereIs(ZERO_ZERO);
			if (where == null) {
				System.err.println("chip (0,0) not in job on default machine");
			}
			try (var txrx = job.getTransceiver()) {
				txrx.bootBoard();
				System.out.println("SCAMP version = " + txrx.getScampVersion());
			}
		} finally {
			job.delete("finished test");
		}
	}

	public static void main(String... args)
			throws IOException, InterruptedException, SpinnmanException {
		// TODO add support for using a bearer token with this code
		var a = new CommandLineCheck(args);
		var factory = new SpallocClientFactory(a.baseUrl);
		var client = factory.login(a.username, a.password);

		// Just so that the server gets its logging out the way first
		Thread.sleep(SHORT_SLEEP);

		checkTheClient(client);

		checkAJob(client);
	}
}
