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
		return baos.toString(UTF_8).split(NEWLINE, -1);
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
