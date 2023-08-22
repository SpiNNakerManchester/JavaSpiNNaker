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
import java.io.PipedReader;
import java.io.PipedWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.errorprone.annotations.RestrictedApi;

import uk.ac.manchester.spinnaker.alloc.ForTestingOnly;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.CompatibilityProperties;
import uk.ac.manchester.spinnaker.alloc.model.Prototype;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;
import uk.ac.manchester.spinnaker.utils.ValueHolder;

/**
 * Implementation of the old style Spalloc interface. Delegates handling a
 * single connection to a {@link V1CompatTask} instance (manufactured by Spring
 * as a {@linkplain Prototype prototype} bean).
 *
 * @author Donal Fellows
 */
@Service("spalloc-v1-compatibility-service")
@UsedInJavadocOnly(Prototype.class)
public class V1CompatService {
	private static final Logger log = getLogger(V1CompatService.class);

	/** The overall service properties. */
	@Autowired
	private SpallocProperties mainProps;

	/**
	 * Factory for {@linkplain V1CompatTask tasks}. Only use via
	 * {@link #getTask(Socket) getTask(...)} or the test API.
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

	V1CompatService() {
		mapper = JsonMapper.builder().propertyNamingStrategy(SNAKE_CASE)
				.addModule(new Jdk8Module())
				.addModule(new JavaTimeModule()).build();
		var group = new ThreadGroup("spalloc-legacy-service");
		var counter = new ValueHolder<>(1);
		threadFactory = r -> {
			var t = new Thread(group, r,
					"spalloc-legacy-" + counter.update(i -> i + 1));
			t.setUncaughtExceptionHandler((thread, ex) -> log
					.error("uncaught exception in {}", thread, ex));
			return t;
		};
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
		var props = mainProps.getCompat();
		if (props.getThreadPoolSize() > 0) {
			log.info("setting thread pool size to {}",
					props.getThreadPoolSize());
			executor = newFixedThreadPool(props.getThreadPoolSize(),
					threadFactory);
		} else {
			log.info("using unbounded thread pool");
			executor = newCachedThreadPool(threadFactory);
		}

		if (props.isEnable()) {
			var addr = new InetSocketAddress(props.getHost(), props.getPort());
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
		var remainingTasks = executor.shutdownNow();
		if (!remainingTasks.isEmpty()) {
			log.warn("there are {} compat tasks outstanding",
					remainingTasks.size());
		}
		if (executor.awaitTermination(shutdownTimeout.toMillis(),
				MILLISECONDS)) {
			log.info("compat service stopped");
		} else {
			log.warn("compat service executor ({}) still running!", executor);
		}
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
			var service = getTask(serv.accept());
			executor.execute(service::handleConnection);
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

	/**
	 * Not a public API! Operations for testing only.
	 *
	 * @hidden
	 */
	@ForTestingOnly
	public interface TestAPI {
		/**
		 * Make an instance of {@link V1CompatTask} that we can talk to.
		 *
		 * @param in
		 *            How to send a message to the task. Should be
		 *            <em>unconnected</em>.
		 * @param out
		 *            How to receive a message from the task. Should be
		 *            <em>unconnected</em>.
		 * @return A future that can be cancelled to shut things down.
		 * @throws Exception
		 *             If various things go wrong.
		 */
		Future<?> launchInstance(PipedWriter in, PipedReader out)
				throws Exception;
	}

	/**
	 * Not a public API!
	 *
	 * @return Test interface.
	 * @deprecated Only for testing.
	 * @hidden
	 */
	@ForTestingOnly
	@RestrictedApi(explanation = "just for testing", link = "index.html",
			allowedOnPath = ".*/src/test/java/.*")
	@Deprecated
	public TestAPI getTestApi() {
		ForTestingOnly.Utils.checkForTestClassOnStack();
		return new TestAPI() {
			@Override
			public Future<?> launchInstance(PipedWriter in, PipedReader out)
					throws Exception {
				var service = taskFactory.getObject(V1CompatService.this,
						new PipedReader(in), new PipedWriter(out));
				return executor.submit(service::handleConnection);
			}
		};
	}
}
