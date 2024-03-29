/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.py2json;

import static uk.ac.manchester.spinnaker.py2json.PythonUtils.getattr;
import static uk.ac.manchester.spinnaker.py2json.PythonUtils.toList;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import org.python.core.PyObject;

import uk.ac.manchester.spinnaker.utils.validation.IPAddress;
import uk.ac.manchester.spinnaker.utils.validation.TCPPort;

/** A configuration description. JSON-serializable. */
public final class Configuration {
	/** The machines to manage. */
	@NotEmpty(message = "there must be at least one machine described")
	public final List<@Valid Machine> machines;

	/** The port for the service to listen on. */
	@TCPPort
	public final int port;

	/**
	 * The host address for the service to listen on. Empty = all interfaces.
	 */
	@IPAddress(emptyOK = true)
	public final String ip;

	/** How often (in seconds) to check for timeouts. */
	@Positive
	public final double timeoutCheckInterval;

	/** How many retired jobs to retain. */
	@PositiveOrZero
	public final int maxRetiredJobs;

	/** Time to wait before freeing. */
	@PositiveOrZero
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
