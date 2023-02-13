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
package uk.ac.manchester.spinnaker.utils.progress;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.utils.Counter;

/**
 *
 * @author Christian-B
 */
public class TestIterable {

	private static final String PERCENTS =
			"|0%                          50%                         100%|";

	private static final String DASHES =
			" ------------------------------------------------------------";

	/**
	 * How to match a platform-independent newline. Fortunately, CR-only
	 * platforms are gone.
	 */
	private static final String NEWLINE = "\\r?\\n";

	private static PrintStream printer(ByteArrayOutputStream baos) {
		return new PrintStream(baos, true, UTF_8);
	}

	private static String[] lines(ByteArrayOutputStream baos) {
		return baos.toString(UTF_8).stripTrailing().split(NEWLINE, -1);
	}

	@Test
	public void testBasic() {
		var description = "Easiest";
		try (var pb = new ProgressIterable<>(
				List.of(1, 2, 3, 4, 5), description,
				printer(new ByteArrayOutputStream()))) {
			int sum = 0;
			for (int i : pb) {
				sum += i;
			}
			assertEquals(1 + 2 + 3 + 4 + 5, sum);
		}
	}

	@Test
	public void testSimple() {
		var baos = new ByteArrayOutputStream();
		var description = "Easiest";
		try (var pb = new ProgressIterable<>(List.of(1, 2, 3, 4, 5, 6, 7),
				description, printer(baos))) {
			int sum = 0;
			for (int i : pb) {
				sum += i;
			}
			assertEquals(1 + 2 + 3 + 4 + 5 + 6 + 7, sum);
			var lines = lines(baos);
			assertEquals(4, lines.length);
			assertEquals(description, lines[0]);
			assertEquals(PERCENTS, lines[1]);
			assertEquals(DASHES, lines[2]);
		}
	}

	@Test
	public void testStopEarly() {
		var baos = new ByteArrayOutputStream();
		var description = "Early";
		try (var bar =
				new ProgressIterable<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
						description, printer(baos))) {
			for (int i : bar) {
				if (i == 3) {
					break;
				}
			}
		}
		var lines = lines(baos);
		assertEquals(4, lines.length);
		assertEquals(description, lines[0]);
		assertEquals(PERCENTS, lines[1]);
		assertEquals(60 / 10 * 3 + 1, lines[2].length());
	}

	@Test
	public void testForEachRemaining() {
		var baos = new ByteArrayOutputStream();
		var description = "Easiest";
		try (var pb = new ProgressIterable<>(List.of(1, 2, 3, 4, 5),
				description, printer(baos))) {
			var sum = new Counter();
			pb.forEach(sum::add);
			assertEquals(1 + 2 + 3 + 4 + 5, sum.get());
			String[] lines = lines(baos);
			assertEquals(4, lines.length);
			assertEquals(description, lines[0]);
			assertEquals(PERCENTS, lines[1]);
			assertEquals(DASHES, lines[2]);
		}
	}
}
