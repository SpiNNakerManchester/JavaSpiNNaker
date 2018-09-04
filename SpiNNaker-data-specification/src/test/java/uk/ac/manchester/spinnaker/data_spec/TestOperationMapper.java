package uk.ac.manchester.spinnaker.data_spec;

import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.data_spec.Commands.PRINT_STRUCT;
import static uk.ac.manchester.spinnaker.data_spec.Generator.Field.COMMAND;

import java.awt.AWTError;
import java.io.IOException;
import java.nio.channels.AcceptPendingException;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.data_spec.exceptions.DataSpecificationException;
import uk.ac.manchester.spinnaker.data_spec.exceptions.UnimplementedDSECommandException;

class TestOperationMapper {
	static final int KEY = PRINT_STRUCT.value << COMMAND.offset;

	@Test
	void test0() throws DataSpecificationException {
		MockFunctions mock = new MockFunctions();
		assertThrows(UnimplementedDSECommandException.class,
				() -> mock.getOperation(KEY, 0));
	}

	@Test
	void test1() throws DataSpecificationException {
		MockFunctions mock = new MockFunctions1();
		assertThrows(IllegalArgumentException.class,
				() -> mock.getOperation(KEY, 0));
	}

	@Test
	void test2() throws DataSpecificationException {
		MockFunctions mock = new MockFunctions2();
		assertThrows(IllegalArgumentException.class,
				() -> mock.getOperation(KEY, 0));
	}

	@Test
	void test3() throws DataSpecificationException {
		MockFunctions mock = new MockFunctions3();
		Callable op = mock.getOperation(KEY, 0);
		RuntimeException e =
				assertThrows(RuntimeException.class, () -> op.execute(KEY));
		assertEquals("bad call", e.getMessage());
		assertEquals(IOException.class, e.getCause().getClass());
		assertEquals("booyah", e.getCause().getMessage());
	}

	@Test
	void test4() throws DataSpecificationException {
		MockFunctions mock = new MockFunctions4();
		Callable op = mock.getOperation(KEY, 0);
		assertThrows(AcceptPendingException.class, () -> op.execute(KEY));
	}

	@Test
	void test5() throws DataSpecificationException {
		MockFunctions mock = new MockFunctions5();
		Callable op = mock.getOperation(KEY, 0);
		assertThrows(AWTError.class, () -> op.execute(KEY));
	}

	@Test
	void test6() throws DataSpecificationException {
		MockFunctions mock = new MockFunctions6();
		Callable op = mock.getOperation(KEY, 0);
		Exception e = assertThrows(DataSpecificationException.class,
				() -> op.execute(KEY));
		assertEquals("boing", e.getMessage());
	}


	@Test
	void test7() throws DataSpecificationException {
		MockFunctions7 mock = new MockFunctions7();
		Callable op = mock.getOperation(KEY, 0);
		assertEquals(0, mock.cmd);
		assertEquals(0, mock.act);
		op.execute(13579);
		assertEquals(13579, mock.cmd); // was unpack() called?
		assertEquals(123454321, mock.act); // was operation() called?
	}
}

class MockFunctions implements FunctionAPI {
	int cmd;

	@Override
	public void unpack(int cmd) {
		this.cmd = cmd;
	}
}

class MockFunctions1 extends MockFunctions {
	@Operation(PRINT_STRUCT)
	public double foo() {
		return 1.23;
	}
}

class MockFunctions2 extends MockFunctions {
	@Operation(PRINT_STRUCT)
	public int foo(int xy) {
		return xy;
	}
}

class MockFunctions3 extends MockFunctions {
	@Operation(PRINT_STRUCT)
	public int foo() throws IOException {
		throw new IOException("booyah");
	}
}

class MockFunctions4 extends MockFunctions {
	@Operation(PRINT_STRUCT)
	public int foo() {
		// We shouldn't be encountering these except deliberately
		throw new AcceptPendingException();
	}
}

class MockFunctions5 extends MockFunctions {
	@Operation(PRINT_STRUCT)
	public int foo() {
		// We shouldn't be encountering these except deliberately
		throw new AWTError("");
	}
}

class MockFunctions6 extends MockFunctions {
	@Operation(PRINT_STRUCT)
	public int foo() throws DataSpecificationException {
		throw new DataSpecificationException("boing");
	}
}

class MockFunctions7 extends MockFunctions {
	int act;
	@Operation(PRINT_STRUCT)
	public void operation() {
		act = 123454321;
	}
}
