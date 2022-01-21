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
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import uk.ac.manchester.spinnaker.alloc.SpallocProperties;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.CompatibilityProperties;
import uk.ac.manchester.spinnaker.utils.ValueHolder;

/**
 * Implementation of the old style Spalloc interface.
 *
 * @author Donal Fellows
 */
@Service("spalloc-v1-compatibility-service")
public class V1CompatService {
	private static final Logger log = getLogger(V1CompatService.class);

	/** The overall service properties. */
	@Autowired
	private SpallocProperties mainProps;

	/**
	 * Factory for {@linkplain V1CompatTask tasks}. Only use via
	 * {@link #getTask(Socket) getTask(...)}.
	 */
	@Autowired
	private ObjectProvider<V1CompatTask> taskFactory;

	/** How we make threads. */
	private final ThreadFactory threadFactory;

	/** The service socket. */
	private ServerSocket serv;

	/** The main network service listener thread. */
	private Thread servThread;

	/**
	 * How to serialize and deserialize JSON. Would be a bean except then we'd
	 * be wiring by name.
	 */
	private final ObjectMapper mapper;

	/** How the majority of threads are launched by the service. */
	private ExecutorService executor;

	private Duration shutdownTimeout;

	public V1CompatService() {
		mapper = JsonMapper.builder().propertyNamingStrategy(SNAKE_CASE)
				.build();
		ThreadGroup group = new ThreadGroup("spalloc-legacy-service");
		ValueHolder<Integer> counter = new ValueHolder<>(1);
		threadFactory = r -> new Thread(group, r,
				"spalloc-legacy-" + counter.update(i -> i + 1));
	}

	/** A class that can reach into a compat service. */
	abstract static class Aware {
		private final V1CompatService srv;

		Aware(V1CompatService service) {
			srv = requireNonNull(service);
		}

		/**
		 * @return The executor to use.
		 */
		protected final ExecutorService getExecutor() {
			return requireNonNull(srv.executor);
		}

		/**
		 * @return The JSON mapper to use if necessary.
		 */
		protected final ObjectMapper getJsonMapper() {
			return requireNonNull(srv.mapper);
		}

		/** @return The relevant properties. */
		protected final CompatibilityProperties getProperties() {
			return srv.mainProps.getCompat();
		}
	}

	@PostConstruct
	private void open() throws IOException {
		CompatibilityProperties props = mainProps.getCompat();
		if (props.getThreadPoolSize() > 0) {
			executor = newFixedThreadPool(props.getThreadPoolSize(),
					threadFactory);
		} else {
			executor = newCachedThreadPool(threadFactory);
		}

		if (props.isEnable()) {
			InetSocketAddress addr =
					new InetSocketAddress(props.getHost(), props.getPort());
			serv = new ServerSocket();
			serv.bind(addr);
			servThread = threadFactory.newThread(this::acceptConnections);
			servThread.setName("spalloc-legacy-service");
			log.info("launching listener thread {} on address {}", servThread,
					addr);
			servThread.start();
		}

		this.shutdownTimeout = props.getShutdownTimeout();
	}

	@PreDestroy
	private void close() throws IOException, InterruptedException {
		if (nonNull(serv)) {
			log.info("shutting down listener thread {}", servThread);
			// Shut down the server socket first; no new clients
			servThread.interrupt();
			serv.close();
			servThread.join();
		}

		// Shut down the clients
		executor.shutdown();
		executor.shutdownNow();
		executor.awaitTermination(shutdownTimeout.toMillis(), MILLISECONDS);
	}

	/**
	 * Make a task.
	 *
	 * @param socket
	 *            The connected socket that the task will be handling.
	 * @return The task instance.
	 */
	private V1CompatTask getTask(Socket socket) {
		return taskFactory.getObject(this, socket);
	}

	/**
	 * Main service loop. Accepts connections and dispatches them to workers.
	 */
	private void acceptConnections() {
		try {
			while (acceptConnection()) {
				continue;
			}
		} finally {
			try {
				serv.close();
			} catch (IOException e) {
				log.warn("IO error", e);
			}
		}
	}

	/**
	 * Accept a single connection and dispatch it to a worker task in a thread.
	 *
	 * @return If {@code false}, we want to stop accepting connections.
	 */
	private boolean acceptConnection() {
		try {
			V1CompatTask service = getTask(serv.accept());
			executor.execute(() -> service.handleConnection());
		} catch (SocketException e) {
			// Check here; interrupt = shutting down = no errors, please
			if (interrupted()) {
				return false;
			}
			if (serv.isClosed()) {
				return false;
			}
			log.warn("IO error", e);
		} catch (IOException e) {
			log.warn("IO error", e);
		}
		// If we've been interrupted here, we want the main loop to stop
		return !interrupted();
	}
}
