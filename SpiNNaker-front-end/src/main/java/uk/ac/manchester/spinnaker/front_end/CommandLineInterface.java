package uk.ac.manchester.spinnaker.front_end;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.front_end.download.DataOut;

/**
 * The main command line interface.
 *
 * @author Donal Fellows
 */
public class CommandLineInterface {
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
