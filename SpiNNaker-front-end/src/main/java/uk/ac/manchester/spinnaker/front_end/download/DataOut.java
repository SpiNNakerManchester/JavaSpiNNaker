package uk.ac.manchester.spinnaker.front_end.download;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
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
	public @interface Field {
		/**
		 * Which position this field is bound to.
		 *
		 * @return The position indicator.
		 */
		Arg value();
	}

	private static final boolean USE_NAMED_OPTIONS = false;

	/**
	 * Used to describe the binding of positions in arguments to fields of the
	 * {@link DataOut} class.
	 *
	 * @author Donal Fellows
	 */
	public enum Arg {
		/** The hostname */
		HOSTNAME(0),
		/** The port number */
		PORT_NUMBER(1),
		/** The X coord of the CPU to read from */
		PLACEMENT_X(2),
		/** The Y coord of the CPU to read from */
		PLACEMENT_Y(3),
		/** The P coord of the CPU to read from */
		PLACEMENT_P(4),
		/** Where to write data to */
		DATA_FILE(5),
		/** Where to report missing sequence numbers */
		MISSING_SEQS_FILE(6),
		/** How many bytes to read */
		LENGTH_IN_BYTES(7),
		/** Where to read from */
		MEMORY_ADDRESS(8),
		/** X coord for IPtag setting */
		CHIP_X(9),
		/** Y coord for IPtag setting */
		CHIP_Y(10),
		/** The ID of the IPtag */
		IPTAG(11);
		private final int value;

		Arg(int value) {
			this.value = value;
		}

		/**
		 * Get the position of the argument in the positional arguments.
		 *
		 * @return The position.
		 */
		public int position() {
			return this.value;
		}
	}

	@Field(Arg.PLACEMENT_X)
	int placement_x;
	@Field(Arg.PLACEMENT_Y)
	int placement_y;
	@Field(Arg.PLACEMENT_P)
	int placement_p;
	@Field(Arg.PORT_NUMBER)
	int port_connection;
	@Field(Arg.LENGTH_IN_BYTES)
	int length_in_bytes;
	@Field(Arg.MEMORY_ADDRESS)
	int memory_address;
	@Field(Arg.HOSTNAME)
	String hostname;
	@Field(Arg.DATA_FILE)
	String file_pathr;
	@Field(Arg.MISSING_SEQS_FILE)
	String file_pathm;
	@Field(Arg.CHIP_X)
	int chip_x;
	@Field(Arg.CHIP_Y)
	int chip_y;
	@Field(Arg.IPTAG)
	int iptag;

	private static CommandLine parse(String[] args) throws ParseException {
		Options options = new Options();
		if (USE_NAMED_OPTIONS) {
			options.addOption(Option.builder("H").longOpt("hostname")
					.desc("the SpiNNaker host name/IP address").hasArg()
					.build());
			options.addOption(Option.builder("P").longOpt("port")
					.desc("the SpiNNaker port number").hasArg()
					.type(Number.class).build());

			options.addOption(Option.builder("p").longOpt("placement")
					.desc("the coordinates (X, Y, P) of the placement")
					.numberOfArgs(3).build());

			options.addOption(Option.builder("D").longOpt("datafile")
					.desc("the file to write the downloaded data into").hasArg()
					.type(File.class).build());
			options.addOption(Option.builder("S").longOpt("seqsfile").desc(
					"the file to write the sequence number information into")
					.hasArg().type(File.class).build());

			options.addOption(Option.builder("l").longOpt("length")
					.desc("the number of bytes to download").hasArg()
					.type(Number.class).build());
			options.addOption(Option.builder("a").longOpt("address")
					.desc("the address to download from").hasArg()
					.type(Number.class).build());

			options.addOption(Option.builder("C").longOpt("chip").desc(
					"the coordinates (X, Y) of the chip for IPtag setting")
					.numberOfArgs(2).build());
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
		if (positionals.size() != Arg.values().length) {
			System.err.println("wrong # args: should be \""
					+ System.getProperty("java.home") + "/bin/java "
					+ DataOut.class + " " + join(Arg.values()) + "\"");
			System.exit(1);
		}
		DataOut operation = new DataOut();
		setFieldsFromArguments(positionals, operation);
		operation.call();
	}

	private static void setFieldsFromArguments(List<String> positionals,
			DataOut operation) throws IllegalAccessException {
		for (Arg a : Arg.values()) {
			String s = positionals.get(a.position());
			for (java.lang.reflect.Field f : operation.getClass()
					.getDeclaredFields()) {
				if (!f.isAnnotationPresent(Field.class)) {
					continue;
				}
				if (f.getAnnotation(Field.class).value() != a) {
					continue;
				}
				f.setAccessible(true);
				if (f.getType() == Integer.TYPE) {
					f.set(operation, Integer.parseInt(s));
				} else {
					f.set(operation, s);
				}
				break;
			}
		}
	}

	@Override
	public Boolean call() throws IOException, InterruptedException {
		HostDataReceiver r = new HostDataReceiver(port_connection,
				new CoreLocation(placement_x, placement_y, placement_p),
				hostname, length_in_bytes, memory_address,
				new ChipLocation(chip_x, chip_y), iptag);
		r.writeData(file_pathr, file_pathm);
		return r.isAlive();
	}
}
