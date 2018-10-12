/*
 * Copyright (c) 2018 The University of Manchester
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
	public void testbasic() {
		String description = "Easiest";
		@SuppressWarnings("resource")
		ProgressIterable<Integer> pb = new ProgressIterable<>(
				Arrays.asList(1, 2, 3, 4, 5), description,
				new PrintStream(new ByteArrayOutputStream()));
		int sum = 0;
		for (int i : pb) {
			sum += i;
		}
		assertEquals(1 + 2 + 3 + 4 + 5, sum);
	}

	@Test
	public void testSimple() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		String description = "Easiest";
		@SuppressWarnings("resource")
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
		@SuppressWarnings("resource")
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
	}

}
