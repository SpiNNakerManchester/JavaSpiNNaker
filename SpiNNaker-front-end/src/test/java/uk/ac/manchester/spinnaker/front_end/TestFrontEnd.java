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
package uk.ac.manchester.spinnaker.front_end;

import static com.github.stefanbirkner.systemlambda.SystemLambda.catchSystemExit;
import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErrNormalized;
import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOutNormalized;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentest4j.AssertionFailedError;

import uk.ac.manchester.spinnaker.front_end.download.request.Gather;
import uk.ac.manchester.spinnaker.front_end.dse.FastExecuteDataSpecification;
import uk.ac.manchester.spinnaker.front_end.dse.HostExecuteDataSpecification;
import uk.ac.manchester.spinnaker.utils.ValueHolder;

class TestFrontEnd {

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
		int code = catchSystemExit(() -> CommandLineInterface.main(args));
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
		var requiredArgs = List.of("<machineFile>", "<runFolder>");
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
	@ValueSource(strings = {"dse", "dse_sys", "dse_app"})
	void testSimpleDSE(String cmd) throws Exception {
		var machineFile = getClass().getResource("/machine.json").getFile();
		var runFolder = "target/test/SimpleDSE";
		new File(runFolder).mkdirs();

		var saved = CommandLineInterface.hostFactory;
		var called = new ValueHolder<>("none");
		try {
			CommandLineInterface.hostFactory =
					(m, db) -> new HostExecuteDataSpecification(m, null) {
						@Override
						public void loadSystemCores() {
							called.setValue("dse_sys");
						}

						@Override
						public void loadApplicationCores() {
							called.setValue("dse_app");
						}

						@Override
						public void loadAllCores() {
							called.setValue("dse");
						}
					};

			var msg = tapSystemErrNormalized(() -> {
				runMainExpecting(2, cmd);
			});
			assertContainsInOrder(msg, "<machineFile>", "<runFolder>");

			tapSystemErrNormalized(() -> {
				runMainExpecting(2, cmd, machineFile);
			});

			tapSystemErrNormalized(() -> {
				runMainExpecting(2, cmd, machineFile, runFolder, "gorp");
			});

			assertEquals("none", called.getValue());
			runMainExpecting(0, cmd, machineFile, runFolder);
			assertEquals(cmd, called.getValue());
		} finally {
			CommandLineInterface.hostFactory = saved;
		}
	}

	@Test
	void testAdvancedDSE() throws Exception {
		var cls = getClass();
		var gatherFile = cls.getResource("/gather.json").getFile();
		var machineFile = cls.getResource("/machine.json").getFile();
		var runFolder = "target/test/AdvancedDSE";
		new File(runFolder).mkdirs();

		var saved = CommandLineInterface.fastFactory;
		var called = new ValueHolder<>("none");
		try {
			CommandLineInterface.fastFactory = (m, g, r,
					db) -> new FastExecuteDataSpecification(m, g, r, null) {
						@Override
						public void loadCores() {
							called.setValue("mon");
						}

						@Override
						protected void buildMaps(List<Gather> gatherers) {
							assertEquals(g, gatherers);
						}
					};

			var msg = tapSystemErrNormalized(() -> {
				runMainExpecting(2, "dse_app_mon");
			});
			assertContainsInOrder(msg, "<gatherFile>", "<machineFile>",
					"<runFolder>", "[<reportFolder>]");

			assertEquals("none", called.getValue());
			runMainExpecting(0, "dse_app_mon", gatherFile, machineFile,
					runFolder);
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
				"<runFolder>");
	}

	@Test
	void testStreamDownload() throws Exception {
		var msg = tapSystemErrNormalized(() -> {
			runMainExpecting(2, "gather");
		});
		assertContainsInOrder(msg, "<gatherFile>", "<machineFile>",
				"<runFolder>");
	}
}
