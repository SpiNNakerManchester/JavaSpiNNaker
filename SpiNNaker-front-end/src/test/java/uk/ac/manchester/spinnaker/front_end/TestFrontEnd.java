/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.front_end;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentest4j.AssertionFailedError;

import uk.ac.manchester.spinnaker.front_end.download.request.Gather;
import uk.ac.manchester.spinnaker.front_end.dse.FastExecuteDataSpecification;
import uk.ac.manchester.spinnaker.front_end.dse.HostExecuteDataSpecification;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.MachineDimensions;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.storage.DSEDatabaseEngine;
import uk.ac.manchester.spinnaker.storage.DSEStorage;
import uk.ac.manchester.spinnaker.storage.ProxyInformation;
import uk.ac.manchester.spinnaker.storage.RegionInfo;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.utils.ValueHolder;

class TestFrontEnd {

	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
	private final PrintStream originalOut = System.out;
	private final PrintStream originalErr = System.err;

	@BeforeAll
	public void setUpStreams() {
		System.setOut(new PrintStream(outContent));
		System.setErr(new PrintStream(errContent));
	}

	@BeforeEach
	public void clearStreams() {
		outContent.reset();
		errContent.reset();
	}

	@AfterAll
	public void restoreStreams() {
		System.setOut(originalOut);
		System.setErr(originalErr);
	}

	/**
	 * Code that should be executed by on of the methods of {@link SystemLambda}.
	 * This code may throw an {@link Exception}. Therefore we cannot use
	 * {@link Runnable}.
	 */
	private interface Statement {
		/**
		 * Execute the statement.
		 *
		 * @throws Exception the statement may throw an arbitrary exception.
		 */
		void execute() throws Exception;
	}

	private String tapSystemOutNormalized(Statement runnable) throws Exception {
		try {
			runnable.execute();
		} finally {
			try {
				outContent.flush();
			} catch (IOException e) {
				// Do Nothing
			}
		}
		return outContent.toString().replace(System.lineSeparator(), "\n");
	}

	private String tapSystemErrNormalized(Statement runnable) throws Exception {
		try {
			runnable.execute();
		} finally {
			try {
				errContent.flush();
			} catch (IOException e) {
				// Do Nothing
			}
		}
		return errContent.toString().replace(System.lineSeparator(), "\n");
	}

	/**
	 * Run the command line, trapping its exit and comparing it with an expected
	 * value.
	 * <p>
	 * This produces <em>one non-suppressable warning</em> due to
	 * deprecation-for-removal in Java 17.
	 *
	 * @param expectedCode
	 *            The expected exit code.
	 * @param args
	 *            The arguments to pass to the command line.
	 * @throws Exception
	 *             If anything goes wrong.
	 * @see <a href="https://github.com/stefanbirkner/system-lambda/issues/27">
	 *      System Lambda Issue #27</a>
	 * @see <a href="https://bugs.openjdk.org/browse/JDK-8199704">JDK Issue
	 *      #8199704</a>
	 */
	private static void runMainExpecting(int expectedCode, String... args)
			throws Exception {
		int code = CommandLineInterface.run(args);
		assertEquals(expectedCode, code);
	}

	/**
	 * Asserts that the message contains all the given substrings, and that
	 * their <em>first</em> occurrences in the message are in the order given.
	 *
	 * @param message
	 *            The message to check within.
	 * @param substrings
	 *            The substrings to look for in order.
	 */
	private static void assertContainsInOrder(String message,
			String... substrings) {
		requireNonNull(message);
		int lastIdx = -2;
		String lastsub = "";
		for (var substring : substrings) {
			requireNonNull(substring);
			int idx = message.indexOf(substring);
			if (idx < 0) {
				throw new AssertionFailedError(
						format("message ‘%s’ does not contain ‘%s’", message,
								substring));
			}
			if (idx <= lastIdx) {
				throw new AssertionFailedError(
						format("message ‘%s’ contains ‘%s’ before ‘%s’",
								message, substring, lastsub));
			}
			lastIdx = idx;
			lastsub = substring;
		}
	}

	@Test
	void testHelp() throws Exception {
		var msg = tapSystemOutNormalized(() -> {
			runMainExpecting(0, "help");
		});
		var requiredSubcommands = List.of("dse_app_mon", "gather");
		var requiredArgs = List.of("<machineFile>", "<logfile>");
		for (var cmd: requiredSubcommands) {
			assertContainsInOrder(msg, cmd);
			var msg2 = tapSystemOutNormalized(() -> {
				runMainExpecting(0, "help", cmd);
			});
			for (var arg : requiredArgs) {
				assertContainsInOrder(msg2, arg);
			}
		}
	}

	@Test
	void testVersion() throws Exception {
		var msg = tapSystemOutNormalized(() -> {
			runMainExpecting(0, "--version");
		});
		assertContainsInOrder(msg, " version ");
	}

	@ParameterizedTest
	@ValueSource(strings = {"dse_sys", "dse_app"})
	@SuppressWarnings("MustBeClosed")
	void testSimpleDSE(String cmd) throws Exception {
		var machineFile = getClass().getResource("/machine.json").getFile();
		var dsFile = getClass().getResource("/ds.sqlite3").getFile();
		var logfile = "target/test/SimpleDSE/jspin.log";

		var saved = CommandLineInterface.hostFactory;
		var called = new ValueHolder<>("none");
		try {
			CommandLineInterface.hostFactory =
					(m, db) -> new HostExecuteDataSpecification(m, null) {
						@Override
						public void loadCores(boolean system) {
							if (system) {
								called.setValue("dse_sys");
							} else {
								called.setValue("dse_app");
							}
						}
					};

			var msg = tapSystemErrNormalized(() -> {
				runMainExpecting(2, cmd);
			});
			assertContainsInOrder(msg, "<machineFile>", "<logfile>");

			tapSystemErrNormalized(() -> {
				runMainExpecting(2, cmd, machineFile);
			});

			tapSystemErrNormalized(() -> {
				runMainExpecting(2, cmd, machineFile, dsFile, logfile,
						"gorp");
			});

			assertEquals("none", called.getValue());
			runMainExpecting(0, cmd, machineFile, dsFile, logfile);
			assertEquals(cmd, called.getValue());
		} finally {
			CommandLineInterface.hostFactory = saved;
		}
	}

	@Test
	@SuppressWarnings("MustBeClosed")
	void testAdvancedDSE() throws Exception {
		var cls = getClass();
		var gatherFile = cls.getResource("/gather.json").getFile();
		var machineFile = cls.getResource("/machine.json").getFile();
		var dsFile = getClass().getResource("/ds.sqlite3").getFile();
		var logfile = "target/test/AdvancedDSE/jspin.log";

		var saved = CommandLineInterface.fastFactory;
		var called = new ValueHolder<>("none");
		try {
			CommandLineInterface.fastFactory = (m, g, r, db) -> {
				var mockDB = new DSEDatabaseEngine() {
					public DSEStorage getStorageInterface() {
						return new MockDSEStorage();
					}
				};
				return new FastExecuteDataSpecification(m, g, r, mockDB) {
					@Override
					public void loadCores(List<Gather> gatherers) {
						called.setValue("mon");
					}
				};
			};

			var msg = tapSystemErrNormalized(() -> {
				runMainExpecting(2, "dse_app_mon");
			});
			assertContainsInOrder(msg, "<gatherFile>", "<machineFile>",
					"<logfile>", "[<reportFolder>]");

			assertEquals("none", called.getValue());
			runMainExpecting(0, "dse_app_mon", gatherFile, machineFile, dsFile,
					logfile);
			assertEquals("mon", called.getValue());
		} finally {
			CommandLineInterface.fastFactory = saved;
		}
	}

	@Test
	void testScampDownload() throws Exception {
		var msg = tapSystemErrNormalized(() -> {
			runMainExpecting(2, "download");
		});
		assertContainsInOrder(msg, "<placementFile>", "<machineFile>",
				"<logfile");
	}

	@Test
	void testStreamDownload() throws Exception {
		var msg = tapSystemErrNormalized(() -> {
			runMainExpecting(2, "gather");
		});
		assertContainsInOrder(msg, "<gatherFile>", "<machineFile>",
				"<logfile>");
	}
}

final class MockDSEStorage implements DSEStorage {

	@Override
	public ProxyInformation getProxyInformation() throws StorageException {
		return null;
	}

	@Override
	public List<Ethernet> listEthernetsToLoad() throws StorageException {
		return List.of();
	}

	@Override
	public List<CoreLocation> listCoresToLoad(Ethernet ethernet,
			boolean loadSystemCores)
			throws StorageException {
		return List.of();
	}

	@Override
	public LinkedHashMap<Integer, Integer> getRegionSizes(CoreLocation xyp)
			throws StorageException {
		return new LinkedHashMap<>();
	}

	@Override
	public void setStartAddress(CoreLocation xyp, MemoryLocation start)
			throws StorageException {
		// Do Nothing
	}

	@Override
	public MemoryLocation getStartAddress(CoreLocation xyp)
			throws StorageException {
		throw new NotImplementedException();
	}

	@Override
	public int getAppId() throws StorageException {
		return 0;
	}

	@Override
	public MachineDimensions getMachineDimensions() throws StorageException {
		return new MachineDimensions(8, 8);
	}

	@Override
	public void setRegionPointer(CoreLocation xyp, int regionNum, int pointer)
			throws StorageException {
		// Do Nothing
	}

	@Override
	public Map<Integer, RegionInfo> getRegionPointersAndContent(
			CoreLocation xyp) throws StorageException {
		return Map.of();
	}
}
