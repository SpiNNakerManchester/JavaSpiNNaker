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

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.front_end.BasicExecutor.Tasks;
import uk.ac.manchester.spinnaker.utils.ValueHolder;

class TestBasicExecutor {

	@Test
	void testRunOne() throws Exception {
		BasicExecutor exe = new BasicExecutor(1);
		ValueHolder<Object> val = new ValueHolder<>();
		Tasks t = exe.submitTasks(asList(() -> {
			val.setValue(true);
		}));
		t.awaitAndCombineExceptions();
		assertEquals(true, val.getValue());
	}

	@Test
	void testRunTwo() throws Exception {
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
	void testRunTwoParallel() throws Exception {
		BasicExecutor exe = new BasicExecutor(2);
		ValueHolder<Object> val1 = new ValueHolder<>();
		ValueHolder<Object> val2 = new ValueHolder<>();
		Tasks t = exe.submitTasks(asList(() -> {
			Thread.sleep(100);
			val1.setValue(true);
		}, () -> {
			Thread.sleep(100);
			val2.setValue(false);
		}));
		t.awaitAndCombineExceptions();
		assertEquals(true, val1.getValue());
		assertEquals(false, val2.getValue());
	}

	@Test
	void testRunOneAndThrow() throws Exception {
		BasicExecutor exe = new BasicExecutor(1);
		Tasks t = exe.submitTasks(asList(() -> {
			throw new IOException("hiya");
		}));
		Exception e = assertThrows(IOException.class,
				() -> t.awaitAndCombineExceptions());
		assertEquals("hiya", e.getMessage());
	}

	@Test
	void testRunTwoAndThrow() throws Exception {
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
	void testRunTwoParallelAndThrow() throws Exception {
		BasicExecutor exe = new BasicExecutor(2);
		Tasks t = exe.submitTasks(asList(() -> {
			Thread.sleep(800);
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
	void testRunTwoOverParallelAndThrow() throws Exception {
		BasicExecutor exe = new BasicExecutor(5);
		Tasks t = exe.submitTasks(asList(() -> {
			Thread.sleep(800);
			throw new IOException("hiya");
		},() -> {
			throw new RuntimeException("boo");
		}));
		Exception e = assertThrows(IOException.class,
				() -> t.awaitAndCombineExceptions());
		assertEquals("hiya", e.getMessage());
		assertEquals("boo", e.getSuppressed()[0].getMessage());
	}
}
