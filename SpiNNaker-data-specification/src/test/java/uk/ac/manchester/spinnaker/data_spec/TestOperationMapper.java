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
package uk.ac.manchester.spinnaker.data_spec;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static uk.ac.manchester.spinnaker.data_spec.Commands.PRINT_STRUCT;
import static uk.ac.manchester.spinnaker.data_spec.Generator.Field.COMMAND;

import java.io.EOFException;
import java.io.IOError;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

class TestOperationMapper {
	private static final int KEY = PRINT_STRUCT.value << COMMAND.offset;

	private static final int BAD_CMD = 0xEE << COMMAND.offset;

	private static class MockFunctions implements FunctionAPI {
		int cmd;

		int act;

		@Override
		public void unpack(int cmd) {
			this.cmd = cmd;
		}
	}

	private MockFunctions mock;

	@AfterEach
	void stopStupid() {
		assertNotNull(mock);
		mock = null;
	}

	/**
	 * Misuses of the API. Disables enforcement of {@link Operation} method
	 * constraints while running these tests.
	 */
	@Nested
	@TestInstance(PER_CLASS)
	class Failures {
		@BeforeAll
		@SuppressWarnings("deprecation")
		void makePermissive() {
			OperationMapper.looseValidation = true;
		}

		@AfterAll
		@SuppressWarnings("deprecation")
		void makeStrict() {
			OperationMapper.looseValidation = false;
		}

		@Test
		void unimplemented() {
			mock = new MockFunctions();
			assertThrows(UnimplementedDSECommandException.class,
					() -> mock.getOperation(KEY, 0));
			assertThrows(UnimplementedDSECommandException.class,
					() -> mock.getOperation(BAD_CMD, 0));
		}

		@Test
		void badResultType() {
			mock = new MockFunctions() {
				@Operation(PRINT_STRUCT)
				public double foo() {
					return 1.23;
				}
			};
			assertThrows(IllegalArgumentException.class,
					() -> mock.getOperation(KEY, 0));
		}

		@Test
		void argumentNotAllowed() {
			mock = new MockFunctions() {
				@Operation(PRINT_STRUCT)
				public int foo(int xy) {
					return xy;
				}
			};
			assertThrows(IllegalArgumentException.class,
					() -> mock.getOperation(KEY, 0));
		}

		@Test
		void unexpectedCheckedException() {
			mock = new MockFunctions() {
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
	}

	/** Various thrown things. */
	@Nested
	class Exceptions {
		@Test
		void runtimeException() {
			mock = new MockFunctions() {
				@Operation(PRINT_STRUCT)
				public int foo() {
					// We shouldn't be encountering this except deliberately
					throw new UncheckedIOException(new EOFException());
				}
			};
			var op = mock.getOperation(KEY, 0);
			assertThrows(UncheckedIOException.class, () -> op.execute(KEY));
		}

		@Test
		void error() {
			mock = new MockFunctions() {
				@Operation(PRINT_STRUCT)
				public int foo() {
					// We shouldn't be encountering this except deliberately
					throw new IOError(null);
				}
			};
			var op = mock.getOperation(KEY, 0);
			assertThrows(IOError.class, () -> op.execute(KEY));
		}

		@Test
		void expectedException() {
			mock = new MockFunctions() {
				@Operation(PRINT_STRUCT)
				public int foo() throws DataSpecificationException {
					throw new DataSpecificationException("boing");
				}
			};
			var op = mock.getOperation(KEY, 0);
			var e = assertThrows(DataSpecificationException.class,
					() -> op.execute(KEY));
			assertEquals("boing", e.getMessage());
			assertNull(e.getCause()); // No cause, no wrapping
		}
	}

	/** Operations working correctly. Whatever that means. */
	@Nested
	class Working {
		@Test
		void voidCall() throws DataSpecificationException {
			mock = new MockFunctions() {
				@Operation(PRINT_STRUCT)
				public void operation() {
					act = 123454321;
				}
			};
			var op = mock.getOperation(KEY, 0);
			assertEquals(0, mock.cmd);
			assertEquals(0, mock.act);
			assertEquals(0, op.execute(13579));
			assertEquals(13579, mock.cmd); // was unpack() called?
			assertEquals(123454321, mock.act); // was operation() called?
		}

		@Test
		void intCall() throws DataSpecificationException {
			mock = new MockFunctions() {
				@Operation(PRINT_STRUCT)
				public int operation() {
					return 2345432;
				}
			};
			var op = mock.getOperation(KEY, 0);
			assertEquals(2345432, op.execute(0));
		}

		@Test
		void integerCall() throws DataSpecificationException {
			mock = new MockFunctions() {
				@Operation(PRINT_STRUCT)
				public Integer operation() {
					return Integer.valueOf(3456543);
				}
			};
			var op = mock.getOperation(KEY, 0);
			assertEquals(3456543, op.execute(0));
		}
	}
}
