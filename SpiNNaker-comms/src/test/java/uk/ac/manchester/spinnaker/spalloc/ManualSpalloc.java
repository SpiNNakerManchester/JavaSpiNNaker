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
package uk.ac.manchester.spinnaker.spalloc;

import static uk.ac.manchester.spinnaker.spalloc.JobConstants.PORT_DEFAULT;

import java.io.IOException;
import java.util.List;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocServerException;
import uk.ac.manchester.spinnaker.spalloc.messages.JobDescription;

/**
 * This is a manual tool to help understanding state while running tests.
 *
 * This NOT intended to be an automated test.
 *
 * @author Christian-B
 */
public class ManualSpalloc {
	private static final String SPALLOC = "spinnaker.cs.man.ac.uk";

	public static void main(String... args)
			throws IOException, SpallocServerException, Exception {
		SpallocClient client = new SpallocClient(SPALLOC, PORT_DEFAULT);
		try (AutoCloseable c = client.withConnection()) {
			List<JobDescription> jobs = client.listJobs();
			for (JobDescription job : jobs) {
				System.out.println(job);
			}
		}
		int two = 2;
		System.out.println(3 / two);
		client.close();
	}
}
