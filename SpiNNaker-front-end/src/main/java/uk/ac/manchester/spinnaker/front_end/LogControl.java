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

import static java.lang.System.getProperty;
import static org.apache.logging.log4j.Level.DEBUG;
import static org.apache.logging.log4j.Level.ERROR;
import static org.apache.logging.log4j.Level.INFO;
import static org.apache.logging.log4j.core.Filter.Result.ACCEPT;
import static org.apache.logging.log4j.core.Filter.Result.DENY;
import static org.apache.logging.log4j.core.appender.ConsoleAppender.Target.SYSTEM_ERR;
import static org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory.newConfigurationBuilder;

import java.io.File;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.FilterComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

/**
 * Utilities for working with the log. <em>This should be the only place in our
 * Java code that directly talks about log4j.</em>
 *
 * @author Donal Fellows
 */
public abstract class LogControl {
	private static final String LOG_FILE = "jspin.log";

	private LogControl() {
	}

	private static Level level(String propertyName) {
		return getProperty(propertyName) == null ? DEBUG : INFO;
	}

	interface Loggers {
		String MAIN = "tofile";
		String CON = "con";
		String PARALLEL = "parallog";
	}

	interface Props {
		String LOGGING_LEVEL_NAME = "logging.level";
		String UDP_LOGGING = "logging.udp";
		String EXECUTOR_LOGGING = "logging.executor";
	}

	interface Classes {
		String BASE = "uk.ac.manchester.spinnaker.";
		String DATA_GATHERER = BASE + "front_end.download.DataGatherer";
		String RR_DATA_GATHERER =
				BASE + "front_end.download.RecordingRegionDataGatherer";
		String FAST_EXEC = BASE + "front_end.dse.FastExecuteDataSpecification";
		String BASE_EXEC = BASE + "front_end.dse.HostExecuteDataSpecification";
		String UDP_CONN = BASE + "connections.UDPConnection";
		String EXEC_CORE = BASE + "data_spec.Executor";
	}

	interface Attrs {
		String ADDITIVITY = "additivity";
		String LEVEL = "level";
		String FILE = "file";
		String PATTERN = "pattern";
		String TARGET = "target";
	}

	interface Patterns {
		String CON = "%p: %m%n";
		String MAIN = "%d{HH:mm:ss.SSS} %-5p %c{1}:%L - %m%n";
		String PARALLEL = "%d{HH:mm:ss.SSS} %-5p %c{1}:%L %X{boardRoot} %m%n";
	}

	private static FilterComponentBuilder filter(
			ConfigurationBuilder<?> builder, Level level) {
		return builder.newFilter("ThresholdFilter", ACCEPT, DENY)
				.addAttribute(Attrs.LEVEL, level);
	}

	private static FilterComponentBuilder filter(
			ConfigurationBuilder<?> builder, String level) {
		return builder.newFilter("ThresholdFilter", ACCEPT, DENY)
				.addAttribute(Attrs.LEVEL, level);
	}

	private static LayoutComponentBuilder layout(
			ConfigurationBuilder<?> builder, String pattern) {
		return builder.newLayout("PatternLayout").addAttribute(Attrs.PATTERN,
				pattern);
	}

	private static RootLoggerComponentBuilder rootLog(
			ConfigurationBuilder<?> builder) {
		return builder.newRootLogger(level(Props.LOGGING_LEVEL_NAME))
				.add(builder.newAppenderRef(Loggers.MAIN))
				.add(builder.newAppenderRef(Loggers.CON));
	}

	private static LoggerComponentBuilder parallelLog(
			ConfigurationBuilder<?> builder, String name) {
		return builder.newLogger(name, DEBUG)
				.add(builder.newAppenderRef(Loggers.PARALLEL))
				.add(builder.newAppenderRef(Loggers.CON))
				.addAttribute(Attrs.ADDITIVITY, false);
	}

	private static LoggerComponentBuilder basicLog(
			ConfigurationBuilder<?> builder, String name, String prop) {
		return builder.newLogger(name, level(prop));
	}

	/**
	 * Initialise the logging subsystem to log to the correct directory.
	 *
	 * @param directoryName
	 *            The directory where the log should written inside.
	 */
	public static void setLoggerDir(String directoryName) {
		File logfile = new File(new File(directoryName), LOG_FILE);
		ConfigurationBuilder<BuiltConfiguration> builder =
				newConfigurationBuilder();
		builder.add(rootLog(builder))
				.add(parallelLog(builder, Classes.DATA_GATHERER))
				.add(parallelLog(builder, Classes.RR_DATA_GATHERER))
				.add(parallelLog(builder, Classes.FAST_EXEC))
				.add(parallelLog(builder, Classes.BASE_EXEC))
				.add(basicLog(builder, Classes.UDP_CONN, Props.UDP_LOGGING))
				.add(basicLog(builder, Classes.EXEC_CORE,
						Props.EXECUTOR_LOGGING))
				.add(builder
						.newAppender(Loggers.CON, ConsoleAppender.PLUGIN_NAME)
						.addAttribute(Attrs.TARGET, SYSTEM_ERR)
						.add(filter(builder, ERROR))
						.add(layout(builder, Patterns.CON)))
				.add(builder.newAppender(Loggers.MAIN, FileAppender.PLUGIN_NAME)
						.addAttribute(Attrs.FILE, logfile.getAbsolutePath())
						.add(filter(builder,
								getProperty(Props.LOGGING_LEVEL_NAME)))
						.add(layout(builder, Patterns.MAIN)))
				.add(builder
						.newAppender(Loggers.PARALLEL, FileAppender.PLUGIN_NAME)
						.addAttribute(Attrs.FILE, logfile.getAbsolutePath())
						.add(filter(builder,
								getProperty(Props.LOGGING_LEVEL_NAME)))
						.add(layout(builder, Patterns.PARALLEL)));
		Configurator.initialize(builder.build());
	}
}
