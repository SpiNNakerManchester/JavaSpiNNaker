/*
 * Copyright (c) 2018-2020 The University of Manchester
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

import static java.lang.System.getProperty;
import static org.apache.logging.log4j.Level.DEBUG;
import static org.apache.logging.log4j.Level.ERROR;
import static org.apache.logging.log4j.Level.INFO;
import static org.apache.logging.log4j.Level.WARN;
import static org.apache.logging.log4j.core.Filter.Result.ACCEPT;
import static org.apache.logging.log4j.core.Filter.Result.DENY;
import static org.apache.logging.log4j.core.appender.ConsoleAppender.Target.SYSTEM_ERR;
import static org.apache.logging.log4j.core.config.Configurator.reconfigure;
import static org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory.newConfigurationBuilder;

import java.io.File;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.FilterComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;

/**
 * Utilities for working with the log. <em>This should be the only place in our
 * Java code that directly talks about log4j.</em>
 *
 * @author Donal Fellows
 */
public final class LogControl {
	private static final String LOG_FILE = "jspin.log";

	/** The names of appenders. */
	private interface Loggers {
		/** Main log appender. */
		String MAIN = "tofile";

		/** Console log appender. */
		String CON = "con";

		/** Parallel (board-localised) operation log appender. */
		String PARALLEL = "parallog";
	}

	/** The names of relevant system properties. */
	private interface Props {
		/** Logging level property. */
		String LOGGING_LEVEL_NAME = "logging.level";

		/** UDP low-level logging level property. */
		String UDP_LOGGING = "logging.udp";

		/** Executor low-level logging level property. */
		String EXECUTOR_LOGGING = "logging.executor";
	}

	/** The names of actual loggers (class names). */
	private interface Classes {
		/** Base for logging domains (package name). */
		String BASE = "uk.ac.manchester.spinnaker.";

		/** Gatherer logging domain. */
		String DATA_GATHERER = BASE + "front_end.download.DataGatherer";

		/** Gatherer logging domain. */
		String RR_DATA_GATHERER =
				BASE + "front_end.download.RecordingRegionDataGatherer";

		/** Executor logging domain. */
		String FAST_EXEC = BASE + "front_end.dse.FastExecuteDataSpecification";

		/** Executor logging domain. */
		String BASE_EXEC = BASE + "front_end.dse.HostExecuteDataSpecification";

		/** Low-level logging domain. */
		String UDP_CONN = BASE + "connections.UDPConnection";

		/** Low-level logging domain. */
		String EXEC_CORE = BASE + "data_spec.Executor";
	}

	/** The names of various attributes of logger components. */
	private interface Attrs {
		/** Control attribute. */
		String ADDITIVITY = "additivity";

		/** Control attribute. */
		String LEVEL = "level";

		/** Control attribute. */
		String FILE = "fileName";

		/** Control attribute. */
		String PATTERN = "pattern";

		/** Control attribute. */
		String TARGET = "target";
	}

	/** The patterns used by loggers. */
	private interface Patterns {
		/** Console log message pattern. */
		String CON = "%p: %m%n";

		/** Main log message pattern. */
		String MAIN = "%d{HH:mm:ss.SSS} %-5p %c{1}:%L - %m%n";

		/** Parallel (board-localised) log message pattern. */
		String PARALLEL = "%d{HH:mm:ss.SSS} %-5p %c{1}:%L %X{boardRoot} %m%n";
	}

	private final ConfigurationBuilder<? extends Configuration> builder;

	private LogControl() {
		builder = newConfigurationBuilder();
	}

	private FilterComponentBuilder filter(Level level) {
		return builder.newFilter("ThresholdFilter", ACCEPT, DENY)
				.addAttribute(Attrs.LEVEL, level);
	}

	private FilterComponentBuilder filter(String level) {
		return builder.newFilter("ThresholdFilter", ACCEPT, DENY)
				.addAttribute(Attrs.LEVEL, level);
	}

	private LayoutComponentBuilder layout(String pattern) {
		return builder.newLayout("PatternLayout").addAttribute(Attrs.PATTERN,
				pattern);
	}

	private static Level debugIfDefined(String propertyName) {
		return getProperty(propertyName) == null ? INFO : DEBUG;
	}

	private RootLoggerComponentBuilder rootLog(String level) {
		return builder.newRootLogger(level)
				.add(builder.newAppenderRef(Loggers.MAIN))
				.add(builder.newAppenderRef(Loggers.CON));
	}

	private LoggerComponentBuilder parallelLog(String name) {
		return builder.newLogger(name, DEBUG)
				.add(builder.newAppenderRef(Loggers.PARALLEL))
				.add(builder.newAppenderRef(Loggers.CON))
				.addAttribute(Attrs.ADDITIVITY, false);
	}

	private LoggerComponentBuilder basicLog(String name, String prop) {
		return builder.newLogger(name, debugIfDefined(prop));
	}

	private Configuration configuration(File logfile, String level) {
		builder.setStatusLevel(ERROR).add(rootLog(level))
				.add(parallelLog(Classes.DATA_GATHERER))
				.add(parallelLog(Classes.RR_DATA_GATHERER))
				.add(parallelLog(Classes.FAST_EXEC))
				.add(parallelLog(Classes.BASE_EXEC))
				.add(basicLog(Classes.UDP_CONN, Props.UDP_LOGGING))
				.add(basicLog(Classes.EXEC_CORE, Props.EXECUTOR_LOGGING))
				.add(builder
						.newAppender(Loggers.CON, ConsoleAppender.PLUGIN_NAME)
						.addAttribute(Attrs.TARGET, SYSTEM_ERR)
						.add(filter(WARN)).add(layout(Patterns.CON)))
				.add(builder.newAppender(Loggers.MAIN, FileAppender.PLUGIN_NAME)
						.addAttribute(Attrs.FILE, logfile.getAbsolutePath())
						.add(filter(level)).add(layout(Patterns.MAIN)))
				.add(builder
						.newAppender(Loggers.PARALLEL, FileAppender.PLUGIN_NAME)
						.addAttribute(Attrs.FILE, logfile.getAbsolutePath())
						.add(filter(level)).add(layout(Patterns.PARALLEL)));
		return builder.build();
	}

	/**
	 * Initialise the logging subsystem to log to the correct directory.
	 * <p>
	 * Note that this reads system properties.
	 *
	 * @param directoryName
	 *            The directory where the log should written inside.
	 */
	protected static void setLoggerDir(String directoryName) {
		var logfile = new File(new File(directoryName), LOG_FILE);
		var level = getProperty(Props.LOGGING_LEVEL_NAME, "info");
		var lc = new LogControl();
		reconfigure(lc.configuration(logfile, level));
	}
}
