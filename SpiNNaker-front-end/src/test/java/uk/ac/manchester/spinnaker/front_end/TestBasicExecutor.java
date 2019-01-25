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

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.front_end.BasicExecutor.Tasks;
import uk.ac.manchester.spinnaker.utils.ValueHolder;

public class TestBasicExecutor {
	private static final int SHORT = 50;
	private static final int LONG = 500;

	@Test
	public void testRunOne() throws Exception {
		BasicExecutor exe = new BasicExecutor(1);
		ValueHolder<Object> val = new ValueHolder<>();
		Tasks t = exe.submitTasks(asList(() -> {
			val.setValue(true);
		}));
		t.awaitAndCombineExceptions();
		assertEquals(true, val.getValue());
	}

	@Test
	public void testRunTwo() throws Exception {
		BasicExecutor exe = new BasicExecutor(1);
		ValueHolder<Object> val1 = new ValueHolder<>();
		ValueHolder<Object> val2 = new ValueHolder<>();
		Tasks t = exe.submitTasks(asList(() -> {
			val1.setValue(true);
		}, () -> {
			val2.setValue(false);
		}));
		t.awaitAndCombineExceptions();
		assertEquals(true, val1.getValue());
		assertEquals(false, val2.getValue());
	}

	@Test
	public void testRunThreeStream() throws Exception {
		BasicExecutor exe = new BasicExecutor(1);
		ValueHolder<Object> val1 = new ValueHolder<>();
		ValueHolder<Object> val2 = new ValueHolder<>();
		ValueHolder<Object> val3 = new ValueHolder<>();
		val3.setValue(123);
		Tasks t = exe.submitTasks(asList(val1, val2, val3).parallelStream()
				.map(val -> () -> val.setValue(val == val1)));
		t.awaitAndCombineExceptions();
		assertEquals(true, val1.getValue());
		assertEquals(false, val2.getValue());
		assertEquals(false, val3.getValue());
	}

	@Test
	public void testRunTwoParallel() throws Exception {
		BasicExecutor exe = new BasicExecutor(2);
		ValueHolder<Object> val1 = new ValueHolder<>();
		ValueHolder<Object> val2 = new ValueHolder<>();
		Tasks t = exe.submitTasks(asList(() -> {
			Thread.sleep(SHORT);
			val1.setValue(true);
		}, () -> {
			Thread.sleep(SHORT);
			val2.setValue(false);
		}));
		t.awaitAndCombineExceptions();
		assertEquals(true, val1.getValue());
		assertEquals(false, val2.getValue());
	}

	@Test
	public void testRunTenParallel() throws Exception {
		final int SCALE = 10;
		BasicExecutor exe = new BasicExecutor(SCALE);
		AtomicInteger sum = new AtomicInteger(0);

		long before = System.currentTimeMillis();
		Tasks t = exe.submitTasks(IntStream.range(0, SCALE).mapToObj(i -> () -> {
			Thread.sleep(SHORT);
			sum.addAndGet(i);
		}));
		t.awaitAndCombineExceptions();
		long after = System.currentTimeMillis();

		assertEquals(45, sum.get());
		long delta = after - before;
		long bound = SCALE * SHORT;
		assertNull(delta < bound ? null
				: "time taken (" + delta + "ms) was longer than " + bound
						+ "ms");
	}

	@Test
	public void testRunOneAndThrow() throws Exception {
		BasicExecutor exe = new BasicExecutor(1);
		Tasks t = exe.submitTasks(asList(() -> {
			throw new IOException("hiya");
		}));
		Exception e = assertThrows(IOException.class,
				() -> t.awaitAndCombineExceptions());
		assertEquals("hiya", e.getMessage());
	}

	@Test
	public void testRunTwoAndThrow() throws Exception {
		BasicExecutor exe = new BasicExecutor(1);
		Tasks t = exe.submitTasks(asList(() -> {
			throw new IOException("hiya");
		},() -> {
			throw new RuntimeException("boo");
		}));
		Exception e = assertThrows(IOException.class,
				() -> t.awaitAndCombineExceptions());
		assertEquals("hiya", e.getMessage());
		assertEquals("boo", e.getSuppressed()[0].getMessage());
	}

	@Test
	public void testRunTwoParallelAndThrow() throws Exception {
		BasicExecutor exe = new BasicExecutor(2);
		Tasks t = exe.submitTasks(asList(() -> {
			Thread.sleep(LONG);
			throw new IOException("hiya");
		},() -> {
			throw new RuntimeException("boo");
		}));
		Exception e = assertThrows(IOException.class,
				() -> t.awaitAndCombineExceptions());
		assertEquals("hiya", e.getMessage());
		assertEquals("boo", e.getSuppressed()[0].getMessage());
	}

	@Test
	public void testRunTwoOverParallelAndThrow() throws Exception {
		BasicExecutor exe = new BasicExecutor(5);
		Tasks t = exe.submitTasks(asList(() -> {
			Thread.sleep(LONG);
			throw new IOException("hiya");
		},() -> {
			throw new RuntimeException("boo");
		}));
		Exception e = assertThrows(IOException.class,
				() -> t.awaitAndCombineExceptions());
		assertEquals("hiya", e.getMessage());
		assertEquals("boo", e.getSuppressed()[0].getMessage());
	}

	@Test
	public void testRunAgain() throws Exception {
		BasicExecutor exe = new BasicExecutor(2);

		Tasks t = exe.submitTasks(asList(() -> {
			Thread.sleep(LONG);
			throw new IOException("hiya");
		},() -> {
			throw new RuntimeException("boo");
		}));
		Exception e = assertThrows(IOException.class,
				() -> t.awaitAndCombineExceptions());
		assertEquals("hiya", e.getMessage());
		assertEquals("boo", e.getSuppressed()[0].getMessage());

		ValueHolder<Object> val = new ValueHolder<>();
		Tasks t2 = exe.submitTasks(asList(() -> {
			val.setValue(true);
		}));
		t2.awaitAndCombineExceptions();
		assertEquals(true, val.getValue());
	}

	@Test
	public void testRunInterleaved() throws Exception {
		BasicExecutor exe = new BasicExecutor(2);

		Tasks t = exe.submitTasks(asList(() -> {
			Thread.sleep(LONG);
			throw new IOException("hiya");
		},() -> {
			throw new RuntimeException("boo");
		}));
		ValueHolder<Object> val = new ValueHolder<>();
		Tasks t2 = exe.submitTasks(asList(() -> {
			Thread.sleep(SHORT);
			val.setValue(true);
		}));

		t2.awaitAndCombineExceptions();
		Exception e = assertThrows(IOException.class,
				() -> t.awaitAndCombineExceptions());
		assertEquals("hiya", e.getMessage());
		assertEquals("boo", e.getSuppressed()[0].getMessage());
		assertEquals(true, val.getValue());
	}

	@Test
	public void testRunInterleaved2() throws Exception {
		BasicExecutor exe = new BasicExecutor(2);

		Tasks t = exe.submitTasks(asList(() -> {
			Thread.sleep(LONG);
			throw new IOException("hiya");
		},() -> {
			throw new RuntimeException("boo");
		}));
		ValueHolder<Object> val = new ValueHolder<>();
		Tasks t2 = exe.submitTasks(asList(() -> {
			Thread.sleep(SHORT);
			val.setValue(true);
		}));

		Exception e = assertThrows(IOException.class,
				() -> t.awaitAndCombineExceptions());
		t2.awaitAndCombineExceptions();
		assertEquals("hiya", e.getMessage());
		assertEquals("boo", e.getSuppressed()[0].getMessage());
		assertEquals(true, val.getValue());
	}

	@Test
	public void testMixedSuccess() throws Exception {
		BasicExecutor exe = new BasicExecutor(5);

		ValueHolder<Object> val = new ValueHolder<>();
		Tasks t = exe.submitTasks(asList(() -> {
			Thread.sleep(LONG);
			throw new IOException("hiya");
		},() -> {
			throw new RuntimeException("boo");
		},() -> {
			Thread.sleep(SHORT);
			val.setValue(true);
		}));

		Exception e = assertThrows(IOException.class,
				() -> t.awaitAndCombineExceptions());
		assertEquals("hiya", e.getMessage());
		assertEquals("boo", e.getSuppressed()[0].getMessage());
		assertEquals(true, val.getValue());
	}
}
