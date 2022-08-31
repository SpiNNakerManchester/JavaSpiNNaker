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
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocServerException;

/**
 * This is a manual tool to help understanding state while running tests.
 * <p>
 * This is <i>not</i> intended to be an automated test.
 *
 * @author Christian-B
 */
public final class ManualSpalloc {
	private static final String SPALLOC = "spinnaker.cs.man.ac.uk";

	private ManualSpalloc() {
	}

	public static void main(String... args)
			throws IOException, SpallocServerException, Exception {
		try (var client = new SpallocClient(SPALLOC, PORT_DEFAULT);
				var c = client.withConnection()) {
			var jobs = client.listJobs();
			for (var job : jobs) {
				System.out.println(job);
			}
		}
	}
}
