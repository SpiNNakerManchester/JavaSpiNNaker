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
import static uk.ac.manchester.spinnaker.machine.bean.MapperFactory.createMapper;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.manchester.spinnaker.data_spec.exceptions.DataSpecificationException;
import uk.ac.manchester.spinnaker.front_end.download.DataOut;
import uk.ac.manchester.spinnaker.front_end.dse.HostExecuteDataSpecification;
import uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management.DataGatherRunner;
import uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management.DataReceiverRunner;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.bean.MachineBean;
import uk.ac.manchester.spinnaker.storage.DSEDatabaseEngine;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;

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
	private static final ObjectMapper MAPPER = createMapper();

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
			case "dse":
				dseRun(args);
				System.exit(0);
			case "version":
				System.out.println(VERSION);
				System.exit(0);
			default:
				System.err.printf("unknown command \"%s\": must be one of %s\n",
                    args[0], "download, dse, or version");
				System.exit(1);
			}
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			System.exit(2);
		}
	}

	private static final int NUM_DSE_ARGS = 3;
	private static void dseRun(String[] args)
			throws UnknownHostException, IOException, SpinnmanException,
			ProcessException, StorageException, ExecutionException,
			InterruptedException, DataSpecificationException {
		if (args.length != NUM_DSE_ARGS) {
			System.err.printf("wrong # args: must be \"java -jar %s "
					+ "dse <machineFile> <database>\"\n", JAR_FILE);
			System.exit(1);
		}
		Machine machine = readMachineJson(args[1]);
		File db = new File(args[2]);

		HostExecuteDataSpecification dseExec =
				new HostExecuteDataSpecification(machine);
		dseExec.loadAll(new DSEDatabaseEngine(db));
	}

	private static void download(String[] args) throws Exception {
		// Shim
		String[] real = new String[args.length - 1];
		System.arraycopy(args, 1, real, 0, real.length);
		DataOut.main(real);
	}

	private static Machine readMachineJson(String filename)
			throws JsonParseException, JsonMappingException, IOException {
		try (FileReader machineReader = new FileReader(filename)) {
			return new Machine(
					MAPPER.readValue(machineReader, MachineBean.class));
		}
	}
}
