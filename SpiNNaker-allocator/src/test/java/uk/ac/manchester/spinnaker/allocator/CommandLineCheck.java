/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.allocator;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static picocli.CommandLine.populateCommand;
import static uk.ac.manchester.spinnaker.allocator.SpallocClientFactory.JSON_MAPPER;

import java.io.IOException;
import java.net.URI;

import picocli.CommandLine.Parameters;

public final class CommandLineCheck {
	private CommandLineCheck() {
	}

	private static class TestingClientArgs {
		@Parameters(index = "0", paramLabel = "BaseURL")
		private URI baseUrl;

		@Parameters(index = "1", paramLabel = "UserName")
		private String username;

		@Parameters(index = "2", paramLabel = "PassWord")
		private String password;
	}

	private static final int SHORT_SLEEP = 100;

	public static void main(String... args)
			throws IOException, InterruptedException {
		var a = populateCommand(new TestingClientArgs(), args);
		var factory = new SpallocClientFactory();
		var client = factory.createClient(a.baseUrl, a.username, a.password);

		// Just so that the server gets its logging out the way first
		Thread.sleep(SHORT_SLEEP);

		System.out.println(client.getVersion());
		System.out.println(client.listMachines().stream()
				.map(m -> m.getName()).collect(toList()));
		for (var m : client.listMachines()) {
			var where = m.getBoardByTriad(0, 0, 1);
			if (isNull(where)) {
				System.out.println(
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
		// Check this directly here
		JSON_MAPPER.readValue("{\"x\":1,\"y\":2,\"z\":3,"
				+ "\"cabinet\":4,\"frame\":5,\"board\":6,"
				+ "\"address\":\"127.0.0.2\"}", BoardCoords.class);
	}
}
