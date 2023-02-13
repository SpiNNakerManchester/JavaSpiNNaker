/*
 * Copyright (c) 2018-2023 The University of Manchester
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
