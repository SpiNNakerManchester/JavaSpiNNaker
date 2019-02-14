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

import static org.apache.log4j.Logger.getRootLogger;

import java.io.File;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import uk.ac.manchester.spinnaker.connections.UDPConnection;
import uk.ac.manchester.spinnaker.front_end.download.DataGatherer;

/**
 * Utilities for working with the log. <em>This should be the only place in our
 * Java code that directly talks about log4j.</em>
 *
 * @author Donal Fellows
 */
public abstract class LogControl {
	private static final String LOG_FILE = "jspin.log";
	private static final String LOGGER_NAME = "tofile";
	private static final String GATHER_LOGGER_NAME = "gatherlog";
	private static final String LOGGING_LEVEL_NAME = "logging.level";
	private static final String UDP_LOGGING = "logging.udp";

	private LogControl() {
	}

	/**
	 * Initialise the logging subsystem to log to the correct directory.
	 *
	 * @param directoryName
	 *            The directory where the log should written inside.
	 */
	public static void setLoggerDir(String directoryName) {
		File dir = new File(directoryName);
		File logfile = new File(dir, LOG_FILE);
		FileAppender a =
				(FileAppender) getRootLogger().getAppender(LOGGER_NAME);
		if (System.getProperty(LOGGING_LEVEL_NAME) == null) {
			a.setThreshold(Level.INFO);
		}
		a.setFile(logfile.getAbsolutePath());
		a.activateOptions();

		a = (FileAppender) Logger.getLogger(DataGatherer.class)
				.getAppender(GATHER_LOGGER_NAME);
		if (System.getProperty(LOGGING_LEVEL_NAME) == null) {
			a.setThreshold(Level.INFO);
		}
		a.setFile(logfile.getAbsolutePath());
		a.activateOptions();

		if (System.getProperty(UDP_LOGGING) != null) {
			Logger.getLogger(UDPConnection.class).setLevel(Level.DEBUG);
		}
	}
}
