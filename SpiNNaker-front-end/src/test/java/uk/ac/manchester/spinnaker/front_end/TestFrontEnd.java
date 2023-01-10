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
import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOutNormalized;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

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
	private static void runExpecting(int expectedCode, String... args)
			throws Exception {
		int code = catchSystemExit(() -> CommandLineInterface.main(args));
		assertEquals(expectedCode, code);
	}

	@Test
	void testHelp() throws Exception {
		var msg = tapSystemOutNormalized(() -> {
			runExpecting(0, "help");
		});
		var requiredSubcommands = List.of("dse_app_mon", "gather");
		var requiredArgs = List.of("<machineFile>", "<runFolder>");
		for (var cmd: requiredSubcommands) {
			assertTrue(msg.contains(cmd),
					() -> format("help message does not contain '%s': %s", cmd,
							msg));
			var msg2 = tapSystemOutNormalized(() -> {
				runExpecting(0, "help", cmd);
			});
			for (var arg : requiredArgs) {
				assertTrue(msg2.contains(arg),
						() -> format("help message does not contain '%s': %s",
								arg, msg2));
			}
		}
	}

	@Test
	void testVersion() throws Exception {
		var msg = tapSystemOutNormalized(() -> {
			runExpecting(0, "--version");
		});
		assertTrue(msg.contains(" version "),
				() -> format("message '%s' does not contain 'version'", msg));
	}

}
