package uk.ac.manchester.spinnaker.front_end.download;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;

/**
 * A very simple command line interface to the host data receiver engine.
 *
 * @author Donal Fellows
 * @see HostDataReceiver
 */
public class DataOut implements Callable<Boolean> {
	/**
	 * Used to describe the binding of positions in arguments to fields of the
	 * {@link DataOut} class.
	 *
	 * @author Donal Fellows
	 */
	@Documented
	@Retention(RUNTIME)
	@Target(FIELD)
	public @interface Bind {
		/**
		 * Which position/optional argument this field is bound to.
		 *
		 * @return The position indicator.
		 */
		Argument value();
	}

	private static final boolean USE_NAMED_OPTIONS = false;

	/**
	 * Used to describe the binding of positions in arguments to fields of the
	 * {@link DataOut} class.
	 *
	 * @author Donal Fellows
	 */
	public enum Argument {
		/** The hostname. */
		HOSTNAME("H"),
		/** The port number. */
		PORT_NUMBER("P"),
		/** The X coord of the CPU to read from. */
		PLACEMENT_X("p", 0),
		/** The Y coord of the CPU to read from. */
		PLACEMENT_Y("p", 1),
		/** The P coord of the CPU to read from. */
		PLACEMENT_P("p", 2),
		/** Where to write data to. */
		DATA_FILE("D"),
		/** Where to report missing sequence numbers. */
		MISSING_SEQS_FILE("S"),
		/** How many bytes to read. */
		LENGTH_IN_BYTES("l"),
		/** Where to read from. */
		MEMORY_ADDRESS("a"),
		/** X coord for IPtag setting. */
		CHIP_X("C", 0),
		/** Y coord for IPtag setting. */
		CHIP_Y("C", 1),
		/** The ID of the IPtag. */
		IPTAG("T");
		private String name;
		private int position;

		Argument(String name) {
			this.name = name;
			position = 0;
		}

		Argument(String name, int position) {
			this.name = name;
			this.position = position;
		}
	}

	/** The X coord of the CPU to read from. */
	@Bind(Argument.PLACEMENT_X)
	int x;
	/** The Y coord of the CPU to read from. */
	@Bind(Argument.PLACEMENT_Y)
	int y;
	/** The P coord of the CPU to read from. */
	@Bind(Argument.PLACEMENT_P)
	int p;
	/** The port number. */
	@Bind(Argument.PORT_NUMBER)
	int portConnection;
	/** How many bytes to read. */
	@Bind(Argument.LENGTH_IN_BYTES)
	int length;
	/** Where to read from. */
	@Bind(Argument.MEMORY_ADDRESS)
	int address;
	/** The hostname. */
	@Bind(Argument.HOSTNAME)
	String hostname;
	/** Where to write data to. */
	@Bind(Argument.DATA_FILE)
	String dataFile;
	/** Where to report missing sequence numbers. */
	@Bind(Argument.MISSING_SEQS_FILE)
	String missingFile;
	/** X coord for IPtag setting. */
	@Bind(Argument.CHIP_X)
	int chipX;
	/** Y coord for IPtag setting. */
	@Bind(Argument.CHIP_Y)
	int chipY;
	/** The ID of the IPtag. */
	@Bind(Argument.IPTAG)
	int iptag;

	private static final int NUM_CHIP_ARGS = 2;
	private static final int NUM_CORE_ARGS = 3;

	private static CommandLine parse(String[] args) throws ParseException {
		Options options = new Options();
		if (USE_NAMED_OPTIONS) {
			// Options for building the transceiver
			options.addOption(Option.builder("H").longOpt("hostname")
					.desc("the SpiNNaker host name/IP address").hasArg()
					.build());
			options.addOption(Option.builder("P").longOpt("port")
					.desc("the SpiNNaker port number").hasArg()
					.type(Number.class).build());

			// What are we retrieving from
			options.addOption(Option.builder("p").longOpt("placement")
					.desc("the coordinates (X, Y, P) of the placement")
					.numberOfArgs(NUM_CORE_ARGS).build());
			options.addOption(Option.builder("l").longOpt("length")
					.desc("the number of bytes to download").hasArg()
					.type(Number.class).build());
			options.addOption(Option.builder("a").longOpt("address")
					.desc("the address to download from").hasArg()
					.type(Number.class).build());

			// Where will we write
			options.addOption(Option.builder("D").longOpt("datafile")
					.desc("the file to write the downloaded data into").hasArg()
					.type(File.class).build());
			options.addOption(Option.builder("S").longOpt("seqsfile").desc(
					"the file to write the sequence number information into")
					.hasArg().type(File.class).build());

			// How do we configure the iptag
			options.addOption(Option.builder("C").longOpt("chip").desc(
					"the coordinates (X, Y) of the chip for IPtag setting")
					.numberOfArgs(NUM_CHIP_ARGS).build());
			options.addOption(Option.builder("T").longOpt("iptag")
					.desc("the IPtag ID number").hasArg().type(Number.class)
					.build());
		}

		DefaultParser parser = new DefaultParser();
		return parser.parse(options, args);
	}

	private static String join(Object[] vals) {
		StringBuilder sb = new StringBuilder();
		String sep = "";
		for (Object o : vals) {
			sb.append(sep).append(o);
			sep = " ";
		}
		return sb.toString();
	}

	/**
	 * Run this class as a command line program.
	 *
	 * @param args
	 *            The command line arguments
	 * @throws Exception
	 *             If anything goes wrong.
	 */
	public static void main(String... args) throws Exception {
		CommandLine cl = parse(args);
		List<String> positionals = cl.getArgList();
		DataOut operation = new DataOut();
		if (USE_NAMED_OPTIONS) {
			if (!positionals.isEmpty()) {
				System.err.println("not all arguments supplied as options");
				System.exit(1);
			}
			setFields(a -> cl.getOptionValues(a.name)[a.position], operation);
		} else {
			if (positionals.size() != Argument.values().length) {
				System.err.println("wrong # args: should be \""
						+ System.getProperty("java.home") + "/bin/java "
						+ DataOut.class + " " + join(Argument.values()) + "\"");
				System.exit(1);
			}
			setFields(a -> positionals.get(a.ordinal()), operation);
		}
		operation.call();
	}

	/**
	 * Describes how to get the value of an argument. This varies according to
	 * whether an argument is by option or by position.
	 *
	 * @author Donal Fellows
	 */
	private interface ArgumentGetter {
		String getValue(Argument argument);
	}

	private static void setFields(ArgumentGetter argGetter, DataOut operation)
			throws IllegalAccessException {
		// Build map of annotated fields
		HashMap<Argument, Field> map = new HashMap<>();
		for (Field f : operation.getClass().getDeclaredFields()) {
			Bind b = f.getAnnotation(Bind.class);
			if (b != null) {
				map.put(b.value(), f);
			}
		}

		// Use the map to set the fields
		for (Argument a : map.keySet()) {
			String s = argGetter.getValue(a);
			Field f = map.get(a);
			f.setAccessible(true);
			if (f.getType() == Integer.TYPE) {
				f.set(operation, Integer.parseInt(s));
			} else {
				f.set(operation, s);
			}
		}
	}

	@Override
	public Boolean call() throws IOException, InterruptedException {
		HostDataReceiver r = new HostDataReceiver(portConnection,
				new CoreLocation(x, y, p), hostname, length, address,
				new ChipLocation(chipX, chipY), iptag);
		r.writeData(dataFile, missingFile);
		return r.isAlive();
	}
}
