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
package uk.ac.manchester.spinnaker.utils;

import java.net.UnknownHostException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import uk.ac.manchester.spinnaker.utils.InetFactory.Inet6NotSupportedException;

/**
 *
 * @author Christian-B
 */
public class TestInetFactory {
	/** Name of a host that isn't going away soon because we control it. */
	private static final String WELL_KNOWN_NAME = "apt.cs.manchester.ac.uk";

	public TestInetFactory() {
	}

	@Test
	public void testByBytes() throws UnknownHostException {
		byte[] bytes = {1, 2, 3, 4};
		InetFactory.getByAddress(bytes);
	}

	public void testByName() throws UnknownHostException {
		InetFactory.getByName(WELL_KNOWN_NAME);
	}

	@Test
	public void testByBytes6() {
		byte[] bytes = {
			1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16
		};
		assertThrows(Inet6NotSupportedException.class, () -> {
			InetFactory.getByAddress(bytes);
		});
	}

	@Test
	public void testByName6() {
		var bytes = "3731:54:65fe:2::a7";
		assertThrows(Inet6NotSupportedException.class, () -> {
			InetFactory.getByName(bytes);
		});
	}

}
