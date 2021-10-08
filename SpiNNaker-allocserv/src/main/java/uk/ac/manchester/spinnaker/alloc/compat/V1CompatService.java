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
package uk.ac.manchester.spinnaker.alloc.compat;

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;
import static java.lang.Thread.interrupted;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.compat.Utils.parseDec;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import uk.ac.manchester.spinnaker.alloc.SpallocProperties;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.CompatibilityProperties;

/**
 * Implementation of the old style Spalloc interface.
 *
 * @author Donal Fellows
 */
@Component("spalloc-v1-compatibility-service")
public class V1CompatService {
	/** In seconds. */
	private static final int SHUTDOWN_TIMEOUT = 3;

	private static final ThreadGroup GROUP =
			new ThreadGroup("spalloc-legacy-service");

	private static final Logger log = getLogger(V1CompatService.class);

	/** The overall service properties. */
	@Autowired
	private SpallocProperties mainProps;

	/**
	 * Factory for {@linkplain V1CompatTask tasks}.
	 */
	@Autowired
	private ObjectProvider<V1CompatTask> taskFactory;

	/** The service socket. */
	private ServerSocket serv;

	/** The main network service listener thread. */
	private Thread servThread;

	/**
	 * How to serialize and deserialize JSON. Would be a bean except then we'd
	 * be wiring by name.
	 */
	final ObjectMapper mapper;

	/** How the majority of threads are launched by the service. */
	ExecutorService executor;

	public V1CompatService() {
		mapper = JsonMapper.builder().propertyNamingStrategy(SNAKE_CASE)
				.build();
	}

	@PostConstruct
	private void open() throws IOException {
		CompatibilityProperties props = mainProps.getCompat();
		if (props.getThreadPoolSize() > 0) {
			executor = newFixedThreadPool(props.getThreadPoolSize(),
					r -> new Thread(GROUP, r));
		} else {
			executor = newCachedThreadPool(r -> new Thread(GROUP, r));
		}

		if (props.isEnable()) {
			InetSocketAddress addr =
					new InetSocketAddress(props.getHost(), props.getPort());
			serv = new ServerSocket();
			serv.bind(addr);
			servThread = new Thread(GROUP, this::acceptConnections);
			servThread.setName("service-master");
			log.info("launching listener thread {} on address {}", servThread,
					addr);
			servThread.start();
		}
	}

	@PreDestroy
	private void close() throws IOException, InterruptedException {
		if (nonNull(serv)) {
			log.info("shutting down listener thread {}", servThread);
			// Shut down the server socket first; no new clients
			servThread.interrupt();
			serv.close();
			servThread.join();

			// Shut down the clients
			executor.shutdown();
			executor.shutdownNow();
			executor.awaitTermination(SHUTDOWN_TIMEOUT, SECONDS);
		}
	}

	private void acceptConnections() {
		try {
			while (!interrupted()) {
				try {
					V1CompatTask service =
							taskFactory.getObject(this, serv.accept());
					executor.execute(() -> service.handleConnection());
				} catch (SocketException e) {
					if (interrupted()) {
						return;
					}
					if (serv.isClosed()) {
						return;
					}
					log.warn("IO error", e);
				} catch (IOException e) {
					log.warn("IO error", e);
				}
			}
		} finally {
			try {
				serv.close();
			} catch (IOException e) {
				log.warn("IO error", e);
			}
		}
	}

	static Integer optInt(List<Object> args) {
		return args.isEmpty() ? null : parseDec(args, 0);
	}

	static String optStr(List<Object> args) {
		return args.isEmpty() ? null : args.get(0).toString();
	}

	/** Indicates a failure to parse a command. */
	static final class Oops extends RuntimeException {
		private static final long serialVersionUID = 1L;

		Oops(String msg) {
			super(msg);
		}
	}

	static final class ReturnResponse {
		private Object returnValue;

		@JsonProperty("return")
		public Object getReturnValue() {
			return returnValue;
		}

		public void setReturnValue(Object returnValue) {
			this.returnValue = returnValue;
		}
	}

	static final class ExceptionResponse {
		private String exception;

		@JsonProperty("exception")
		public String getException() {
			return exception;
		}

		public void setException(String exception) {
			this.exception = isNull(exception) ? "" : exception.toString();
		}
	}

	static final class JobNotifyMessage {
		private List<Integer> jobsChanged;

		/**
		 * @return the jobs changed
		 */
		@JsonProperty("jobs_changed")
		public List<Integer> getJobsChanged() {
			return jobsChanged;
		}

		public void setJobsChanged(List<Integer> jobsChanged) {
			this.jobsChanged = jobsChanged;
		}
	}

	static final class MachineNotifyMessage {
		private List<String> machinesChanged;

		/**
		 * @return the machines changed
		 */
		@JsonProperty("machines_changed")
		public List<String> getMachinesChanged() {
			return machinesChanged;
		}

		public void setMachinesChanged(List<String> machinesChanged) {
			this.machinesChanged = machinesChanged;
		}
	}
}
