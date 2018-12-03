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

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.front_end.download.DataOut;
import uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management.DataGatherRunner;
import uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management.DataReceiverRunner;

/**
 * The main command line interface.
 *
 * @author Donal Fellows
 */
public final class CommandLineInterface {
	private CommandLineInterface() {
	}

	private static final Logger log = getLogger(CommandLineInterface.class);
	private static final String JAR_FILE;
	@SuppressWarnings("unused")
	private static final String MAIN_CLASS;
	private static final String VERSION;

	static {
		Properties prop = new Properties();
		try {
			prop.load(CommandLineInterface.class.getClassLoader()
					.getResourceAsStream("command-line.properties"));
		} catch (IOException e) {
			log.error("failed to read properties", e);
			System.exit(2);
		}
		JAR_FILE = prop.getProperty("jar");
		MAIN_CLASS = prop.getProperty("mainClass");
		VERSION = prop.getProperty("version");
	}

	/**
	 * The main command line interface. Dispatches to other classes based on the
	 * first argument, which is a command word.
	 *
	 * @param args
	 *            The command line arguments.
	 */
	public static void main(String... args) {
		if (args.length < 1) {
			System.err.printf(
                "wrong # args: must be \"java -jar %s <command> ...\"\n",
                JAR_FILE);
			System.exit(1);
		}
		try {
			switch (args[0]) {
			case "upload":
				DataReceiverRunner.main(args);
				System.exit(0);
			case "gather":
				DataGatherRunner.main(args);
				System.exit(0);
			case "download":
				download(args);
				System.exit(0);
			case "version":
				System.out.println(VERSION);
				System.exit(0);
			default:
				System.err.printf("unknown command \"%s\": must be one of %s\n",
                    args[0], "download, or version");
				System.exit(1);
			}
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			System.exit(2);
		}
	}

	private static void download(String[] args) throws Exception {
		// Shim
		String[] real = new String[args.length - 1];
		System.arraycopy(args, 1, real, 0, real.length);
		DataOut.main(real);
	}
}
