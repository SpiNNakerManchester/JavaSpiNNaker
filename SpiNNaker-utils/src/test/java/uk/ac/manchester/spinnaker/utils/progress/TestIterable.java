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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
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

	public TestIterable() {
	}

	@Test
	public void testBasic() {
		String description = "Easiest";
		ProgressIterable<Integer> pb = new ProgressIterable<>(
				Arrays.asList(1, 2, 3, 4, 5), description,
				new PrintStream(new ByteArrayOutputStream()));
		int sum = 0;
		for (int i : pb) {
			sum += i;
		}
		assertEquals(1 + 2 + 3 + 4 + 5, sum);
		pb.close();
	}

	@Test
	public void testSimple() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		String description = "Easiest";
		ProgressIterable<Integer> pb =
				new ProgressIterable<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7),
						description, new PrintStream(baos));
		int sum = 0;
		for (int i : pb) {
			sum += i;
		}
		assertEquals(1 + 2 + 3 + 4 + 5 + 6 + 7, sum);
		String lines[] = baos.toString().split("\\r?\\n");
		assertEquals(4, lines.length);
		assertEquals(description, lines[0]);
		assertEquals(PERCENTS, lines[1]);
		assertEquals(DASHES, lines[2]);
		pb.close();
	}

	@Test
	public void testStopEarly() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		String description = "Early";
		try (ProgressIterable<Integer> bar = new ProgressIterable<Integer>(
				Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), description,
				new PrintStream(baos))) {
			for (int i : bar) {
				if (i == 3) {
					break;
				}
			}
		}
		String lines[] = baos.toString().split("\\r?\\n");
		assertEquals(4, lines.length);
		assertEquals(description, lines[0]);
		assertEquals(PERCENTS, lines[1]);
		assertEquals(60 / 10 * 3 + 1, lines[2].length());
	}

	@Test
	public void testForEachRemaining() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		String description = "Easiest";
		ProgressIterable<Integer> pb =
				new ProgressIterable<>(Arrays.asList(1, 2, 3, 4, 5),
						description, new PrintStream(baos));
		Counter sum = new Counter();
		pb.forEach(sum::add);
		assertEquals(1 + 2 + 3 + 4 + 5, sum.get());
		String lines[] = baos.toString().split("\\r?\\n");
		assertEquals(4, lines.length);
		assertEquals(description, lines[0]);
		assertEquals(PERCENTS, lines[1]);
		assertEquals(DASHES, lines[2]);
		pb.close();
	}
}
