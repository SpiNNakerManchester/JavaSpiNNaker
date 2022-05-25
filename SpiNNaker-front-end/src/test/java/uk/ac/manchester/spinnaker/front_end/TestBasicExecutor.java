/*
 * Copyright (c) 2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.utils.ValueHolder;

public class TestBasicExecutor {
	private static final int SHORT = 50;
	private static final int LONG = 500;
	private static final String TOKEN = "arbitrary";
	private static final String EXN_MSG_1 = "hiya";
	private static final String EXN_MSG_2 = "boo";

	@Test
	public void testRunOne() throws Exception {
		var val = new ValueHolder<>();

		try (var exe = new BasicExecutor(1)) {
			var t = exe.submitTasks(asList(() -> {
				val.setValue(true);
			}));
			t.awaitAndCombineExceptions();
		}

		assertEquals(true, val.getValue());
	}

	@Test
	public void testRunTwo() throws Exception {
		var val1 = new ValueHolder<>();
		var val2 = new ValueHolder<>();

		try (var exe = new BasicExecutor(1)) {
			var t = exe.submitTasks(asList(() -> {
				val1.setValue(true);
			}, () -> {
				val2.setValue(false);
			}));
			t.awaitAndCombineExceptions();
		}

		assertEquals(true, val1.getValue());
		assertEquals(false, val2.getValue());
	}

	@Test
	public void testRunThreeStream() throws Exception {
		var val1 = new ValueHolder<>();
		var val2 = new ValueHolder<>();
		var val3 = new ValueHolder<>();

		try (var exe = new BasicExecutor(1)) {
			val3.setValue(TOKEN);
			var t = exe.submitTasks(asList(val1, val2, val3).parallelStream()
					.map(val -> () -> val.setValue(val == val1)));
			t.awaitAndCombineExceptions();
		}

		assertEquals(true, val1.getValue());
		assertEquals(false, val2.getValue());
		assertEquals(false, val3.getValue());
	}

	@Test
	public void testRunTwoParallel() throws Exception {
		var val1 = new ValueHolder<>();
		var val2 = new ValueHolder<>();

		try (var exe = new BasicExecutor(2)) {
			var t = exe.submitTasks(asList(() -> {
				Thread.sleep(SHORT);
				val1.setValue(TOKEN);
			}, () -> {
				Thread.sleep(SHORT);
				val2.setValue(false);
			}));
			t.awaitAndCombineExceptions();
		}

		assertEquals(TOKEN, val1.getValue());
		assertEquals(false, val2.getValue());
	}

	@Test
	public void testRunTenParallel() throws Exception {
		final int SCALE = 10;
		var sum = new AtomicInteger(0);
		long before, after;

		try (var exe = new BasicExecutor(SCALE)) {
			before = currentTimeMillis();
			var t = exe.submitTasks(range(0, SCALE).mapToObj(i -> () -> {
				Thread.sleep(SHORT);
				sum.addAndGet(i);
			}));
			t.awaitAndCombineExceptions();
			after = currentTimeMillis();
		}

		assertEquals(45, sum.get());
		long delta = after - before;
		long bound = SCALE * SHORT;
		assertNull(delta < bound ? null
				: "time taken (" + delta + "ms) was longer than " + bound
						+ "ms");
	}

	@Test
	public void testRunOneAndThrow() throws Exception {
		Exception e;

		try (var exe = new BasicExecutor(1)) {
			var t = exe.submitTasks(asList(() -> {
				throw new IOException(EXN_MSG_1);
			}));
			e = assertThrows(IOException.class,
					() -> t.awaitAndCombineExceptions());
		}

		assertEquals(EXN_MSG_1, e.getMessage());
	}

	@Test
	public void testRunTwoAndThrow() throws Exception {
		Exception e;

		try (var exe = new BasicExecutor(1)) {
			var t = exe.submitTasks(asList(() -> {
				throw new IOException(EXN_MSG_1);
			}, () -> {
				throw new RuntimeException(EXN_MSG_2);
			}));
			e = assertThrows(IOException.class,
					() -> t.awaitAndCombineExceptions());
		}

		assertEquals(EXN_MSG_1, e.getMessage());
		assertEquals(EXN_MSG_2, e.getSuppressed()[0].getMessage());
	}

	@Test
	public void testRunTwoParallelAndThrow() throws Exception {
		Exception e;

		try (var exe = new BasicExecutor(2)) {
			var t = exe.submitTasks(asList(() -> {
				Thread.sleep(LONG);
				throw new IOException(EXN_MSG_1);
			}, () -> {
				throw new RuntimeException(EXN_MSG_2);
			}));
			e = assertThrows(IOException.class,
					() -> t.awaitAndCombineExceptions());
		}

		assertEquals(EXN_MSG_1, e.getMessage());
		assertEquals(EXN_MSG_2, e.getSuppressed()[0].getMessage());
	}

	@Test
	public void testRunTwoOverParallelAndThrow() throws Exception {
		Exception e;

		try (var exe = new BasicExecutor(5)) {
			var t = exe.submitTasks(asList(() -> {
				Thread.sleep(LONG);
				throw new IOException(EXN_MSG_1);
			}, () -> {
				throw new RuntimeException(EXN_MSG_2);
			}));
			e = assertThrows(IOException.class,
					() -> t.awaitAndCombineExceptions());
		}

		assertEquals(EXN_MSG_1, e.getMessage());
		assertEquals(EXN_MSG_2, e.getSuppressed()[0].getMessage());
	}

	@Test
	public void testRunAgain() throws Exception {
		var val = new ValueHolder<>();
		Exception e;

		try (var exe = new BasicExecutor(2)) {
			var t = exe.submitTasks(asList(() -> {
				Thread.sleep(LONG);
				throw new IOException(EXN_MSG_1);
			}, () -> {
				throw new RuntimeException(EXN_MSG_2);
			}));
			e = assertThrows(IOException.class,
					() -> t.awaitAndCombineExceptions());

			var t2 = exe.submitTasks(asList(() -> {
				val.setValue(TOKEN);
			}));
			t2.awaitAndCombineExceptions();
		}

		assertEquals(EXN_MSG_1, e.getMessage());
		assertEquals(EXN_MSG_2, e.getSuppressed()[0].getMessage());
		assertEquals(TOKEN, val.getValue());
	}

	@Test
	public void testRunInterleaved() throws Exception {
		var val = new ValueHolder<>();
		Exception e;

		try (var exe = new BasicExecutor(2)) {
			var t = exe.submitTasks(asList(() -> {
				Thread.sleep(LONG);
				throw new IOException(EXN_MSG_1);
			}, () -> {
				throw new RuntimeException(EXN_MSG_2);
			}));
			var t2 = exe.submitTasks(asList(() -> {
				Thread.sleep(SHORT);
				val.setValue(TOKEN);
			}));

			t2.awaitAndCombineExceptions();
			e = assertThrows(IOException.class,
					() -> t.awaitAndCombineExceptions());
		}

		assertEquals(EXN_MSG_1, e.getMessage());
		assertEquals(EXN_MSG_2, e.getSuppressed()[0].getMessage());
		assertEquals(TOKEN, val.getValue());
	}

	@Test
	public void testRunInterleaved2() throws Exception {
		var val = new ValueHolder<>();
		Exception e;

		try (var exe = new BasicExecutor(2)) {
			var t = exe.submitTasks(asList(() -> {
				Thread.sleep(LONG);
				throw new IOException(EXN_MSG_1);
			}, () -> {
				throw new RuntimeException(EXN_MSG_2);
			}));
			var t2 = exe.submitTasks(asList(() -> {
				Thread.sleep(SHORT);
				val.setValue(TOKEN);
			}));

			e = assertThrows(IOException.class,
					() -> t.awaitAndCombineExceptions());
			t2.awaitAndCombineExceptions();
		}

		assertEquals(EXN_MSG_1, e.getMessage());
		assertEquals(EXN_MSG_2, e.getSuppressed()[0].getMessage());
		assertEquals(TOKEN, val.getValue());
	}

	@Test
	public void testMixedSuccess() throws Exception {
		var val = new ValueHolder<>();
		Exception e;

		try (var exe = new BasicExecutor(5)) {
			var t = exe.submitTasks(asList(() -> {
				Thread.sleep(LONG);
				throw new IOException(EXN_MSG_1);
			}, () -> {
				throw new RuntimeException(EXN_MSG_2);
			}, () -> {
				Thread.sleep(SHORT);
				val.setValue(TOKEN);
			}));

			e = assertThrows(IOException.class,
					() -> t.awaitAndCombineExceptions());
		}

		assertEquals(EXN_MSG_1, e.getMessage());
		assertEquals(EXN_MSG_2, e.getSuppressed()[0].getMessage());
		assertEquals(TOKEN, val.getValue());
	}
}
