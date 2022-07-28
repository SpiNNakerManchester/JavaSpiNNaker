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
package uk.ac.manchester.spinnaker.data_spec.impl;

import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.data_spec.generator.Generator.Field.COMMAND;
import static uk.ac.manchester.spinnaker.data_spec.impl.Commands.PRINT_STRUCT;

import java.awt.AWTError;
import java.io.IOException;
import java.nio.channels.AcceptPendingException;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.data_spec.DataSpecificationException;

class TestOperationMapper {
	static final int KEY = PRINT_STRUCT.value << COMMAND.offset;
	static final int BAD_CMD = 0xEE << COMMAND.offset;

	static class MockFunctions implements FunctionAPI {
		int cmd;

		@Override
		public void unpack(int cmd) {
			this.cmd = cmd;
		}
	}

	@Test
	void test0() throws DataSpecificationException {
		var mock = new MockFunctions();
		assertThrows(UnimplementedDSECommandException.class,
				() -> mock.getOperation(KEY, 0));
		assertThrows(UnimplementedDSECommandException.class,
				() -> mock.getOperation(BAD_CMD, 0));
	}

	@Test
	void test1() throws DataSpecificationException {
		var mock = new MockFunctions() {
			@Operation(PRINT_STRUCT)
			public double foo() {
				return 1.23;
			}
		};
		assertThrows(IllegalArgumentException.class,
				() -> mock.getOperation(KEY, 0));
	}

	@Test
	void test2() throws DataSpecificationException {
		var mock = new MockFunctions() {
			@Operation(PRINT_STRUCT)
			public int foo(int xy) {
				return xy;
			}
		};
		assertThrows(IllegalArgumentException.class,
				() -> mock.getOperation(KEY, 0));
	}

	@Test
	void test3() throws DataSpecificationException {
		var mock = new MockFunctions() {
			@Operation(PRINT_STRUCT)
			public int foo() throws IOException {
				throw new IOException("booyah");
			}
		};
		var op = mock.getOperation(KEY, 0);
		var e = assertThrows(RuntimeException.class, () -> op.execute(KEY));
		assertEquals("bad call", e.getMessage());
		assertEquals(IOException.class, e.getCause().getClass());
		assertEquals("booyah", e.getCause().getMessage());
	}

	@Test
	void test4() throws DataSpecificationException {
		var mock = new MockFunctions() {
			@Operation(PRINT_STRUCT)
			public int foo() {
				// We shouldn't be encountering these except deliberately
				throw new AcceptPendingException();
			}
		};
		var op = mock.getOperation(KEY, 0);
		assertThrows(AcceptPendingException.class, () -> op.execute(KEY));
	}

	@Test
	void test5() throws DataSpecificationException {
		var mock = new MockFunctions() {
			@Operation(PRINT_STRUCT)
			public int foo() {
				// We shouldn't be encountering these except deliberately
				throw new AWTError("");
			}
		};
		var op = mock.getOperation(KEY, 0);
		assertThrows(AWTError.class, () -> op.execute(KEY));
	}

	@Test
	void test6() throws DataSpecificationException {
		var mock = new MockFunctions() {
			@Operation(PRINT_STRUCT)
			public int foo() throws DataSpecificationException {
				throw new DataSpecificationException("boing");
			}
		};
		var op = mock.getOperation(KEY, 0);
		var e = assertThrows(DataSpecificationException.class,
				() -> op.execute(KEY));
		assertEquals("boing", e.getMessage());
	}

	@Test
	void test7() throws DataSpecificationException {
		class MockFunctions7 extends MockFunctions {
			int act;

			@Operation(PRINT_STRUCT)
			public void operation() {
				act = 123454321;
			}
		}
		var mock = new MockFunctions7();
		var op = mock.getOperation(KEY, 0);
		assertEquals(0, mock.cmd);
		assertEquals(0, mock.act);
		op.execute(13579);
		assertEquals(13579, mock.cmd); // was unpack() called?
		assertEquals(123454321, mock.act); // was operation() called?
	}
}
