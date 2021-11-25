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
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

	private static final ThreadFactory THREAD_FACTORY;

	static {
		ThreadGroup group = new ThreadGroup("spalloc-legacy-service");
		THREAD_FACTORY = r -> new Thread(group, r);
	}

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
					THREAD_FACTORY);
		} else {
			executor = newCachedThreadPool(THREAD_FACTORY);
		}

		if (props.isEnable()) {
			InetSocketAddress addr =
					new InetSocketAddress(props.getHost(), props.getPort());
			serv = new ServerSocket();
			serv.bind(addr);
			servThread = THREAD_FACTORY.newThread(this::acceptConnections);
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
		}

		// Shut down the clients
		executor.shutdown();
		executor.shutdownNow();
		executor.awaitTermination(SHUTDOWN_TIMEOUT, SECONDS);
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
}
