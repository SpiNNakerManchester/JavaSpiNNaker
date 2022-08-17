/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.py2json;

import static uk.ac.manchester.spinnaker.py2json.PythonUtils.getattr;
import static uk.ac.manchester.spinnaker.py2json.PythonUtils.toList;

import java.util.List;

import org.python.core.PyObject;

/** A configuration description. JSON-serializable. */
public final class Configuration {
	/** The machines to manage. */
	public final List<Machine> machines;

	/** The port for the service to listen on. */
	public final int port;

	/**
	 * The host address for the service to listen on. Empty = all interfaces.
	 */
	public final String ip;

	/** How often (in seconds) to check for timeouts. */
	public final double timeoutCheckInterval;

	/** How many retired jobs to retain. */
	public final int maxRetiredJobs;

	/** Time to wait before freeing. */
	public final int secondsBeforeFree;

	Configuration(PyObject configuration) {
		machines = toList(getattr(configuration, "machines"), Machine::new);
		port = getattr(configuration, "port").asInt();
		ip = getattr(configuration, "ip").asString();
		timeoutCheckInterval =
				getattr(configuration, "timeout_check_interval").asDouble();
		maxRetiredJobs = getattr(configuration, "max_retired_jobs").asInt();
		secondsBeforeFree =
				getattr(configuration, "seconds_before_free").asInt();
	}

	@Override
	public String toString() {
		return new StringBuilder("Configuration(").append("machines=")
				.append(machines).append(",").append("port=").append(port)
				.append(",").append("ip=").append(ip).append(",")
				.append("timeoutCheckInterval=").append(timeoutCheckInterval)
				.append(",").append("maxRetiredJobs=").append(maxRetiredJobs)
				.append(",").append("secondsBeforeFree=")
				.append(secondsBeforeFree).append(")").toString();
	}
}
