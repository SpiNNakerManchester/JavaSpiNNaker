package uk.ac.manchester.spinnaker.front_end;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.front_end.download.DataOut;

public class CommandLineInterface {
	private static final Logger log = getLogger(CommandLineInterface.class);
	private static final String JARFILE;

	static {
		Properties prop = new Properties();
		try {
			prop.load(CommandLineInterface.class.getClassLoader()
					.getResourceAsStream("command-line.properties"));
		} catch (IOException e) {
			log.error("failed to read properties", e);
			System.exit(2);
		}
		JARFILE = prop.getProperty("jar");
	}

	public static void main(String... args) {
		if (args.length < 1) {
			System.err.printf(
					"wrong # args: must be \"java -jar %s <command> ...\"\n",
					JARFILE);
			System.exit(1);
		}
		try {
			switch (args[0]) {
			case "download":
				download(args);
				System.exit(0);
			default:
				System.err.printf("unknown command \"%s\": must be one of %s\n",
						args[0], "download");
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
