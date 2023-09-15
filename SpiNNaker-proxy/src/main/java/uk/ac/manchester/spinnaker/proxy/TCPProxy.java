/*
 * Copyright (c) 2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.proxy;

import static java.lang.String.format;
import static java.util.Objects.requireNonNullElse;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;

import org.slf4j.Logger;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * A TCP Proxy server that can re-connect if the target goes down.
 * <p>
 * Typically called via:
 *
 * <pre>java -jar spinnaker-proxy.jar LOCAL_PORT REMOTE_HOST REMOTE_PORT</pre>
 *
 * @author Andrew Rowley
 */
public class TCPProxy {
	/** The maximum size of a read from a TCP socket. */
	private static final int BUFFER_SIZE = 4096;

	/** The logger. */
	static final Logger log = getLogger(TCPProxy.class);

	private Socket client;

	private final Remote remote;

	TCPProxy(Socket client, String remoteHost, int remotePort) {
		log.info("New connection from {}", client.getRemoteSocketAddress());
		this.client = client;
		this.remote = new Remote(remoteHost, remotePort);

		var clientToRemote = new Thread(this::clientToRemote);
		var remoteToClient = new Thread(this::remoteToClient);
		clientToRemote.start();
		remoteToClient.start();
	}

	/**
	 * Read the client and send to the remote. If remote write fails, reconnect
	 * to remote and resend. If client read fails, stop.
	 */
	private void clientToRemote() {
		try {
			remote.connect();
			var buffer = new byte[BUFFER_SIZE];
			try (var input = client.getInputStream()) {
				while (client.isConnected()) {
					int bytesRead = input.read(buffer);
					if (bytesRead == -1) {
						throw new IOException();
					}
					remote.write(buffer, 0, bytesRead);
				}
			} catch (IOException e) {
				// This is likely caused by the client disconnecting.
			}
		} catch (InterruptedException e) {
			// Exiting
		}
		forceCloseClient();
		remote.close();
	}

	/**
	 * Read from the remote and send to the client. If the remote read fails,
	 * retry until working. If the client write fails, stop.
	 */
	private void remoteToClient() {
		try {
			remote.connect();
			var buffer = new byte[BUFFER_SIZE];
			try (var output = client.getOutputStream()) {
				while (client.isConnected()) {
					int bytesRead = remote.read(buffer);
					if (bytesRead == -1) {
						throw new IOException();
					}
					output.write(buffer, 0, bytesRead);
				}
			} catch (IOException e) {
				// This is likely caused by the client disconnecting.
			}
		} catch (InterruptedException e) {
			// Exiting
		}
		forceCloseClient();
		remote.close();
	}

	/**
	 * Close the client and ignore any errors.
	 */
	private synchronized void forceCloseClient() {
		if (client == null) {
			return;
		}
		log.info("Client {} left", client.getRemoteSocketAddress());
		try {
			client.close();
		} catch (IOException e) {
			// Ignore
		}
		client = null;
	}

	static int mainLoop(int localPort, String remoteHost, int remotePort) {
		try (var server = new ServerSocket(localPort)) {
			log.info("listening on {}", server.getLocalSocketAddress());
			while (true) {
				var client = server.accept();
				new TCPProxy(client, remoteHost, remotePort);
			}
		} catch (InterruptedIOException e) {
			return CommandLine.ExitCode.OK;
		} catch (IOException e) {
			log.error("failure in listener", e);
			return CommandLine.ExitCode.SOFTWARE;
		}
	}

	/**
	 * Implementation of the command line handler.
	 */
	@Command(name = "spinnaker-proxy", mixinStandardHelpOptions = true, //
			versionProvider = Version.class)
	static class CLI implements Callable<Integer> {
		@Parameters(index = "0", paramLabel = "localPort",
				description = "The local port to listen on.")
		private int localPort;

		@Parameters(index = "1", paramLabel = "remoteHost",
				description = "The remote host to proxy.")
		private String remoteHost;

		@Parameters(index = "2", paramLabel = "remotePort",
				description = "The remote port to proxy.")
		private int remotePort;

		@Spec
		private CommandSpec spec;

		private void validate(int port, String name) {
			// TCP port numbers are really unsigned shorts
			if (port != Short.toUnsignedInt((short) port)) {
				throw new ParameterException(spec.commandLine(),
						format("value '%s' for parameter '%s' is out of "
								+ "range (0..65535)", port, name));
			}
		}

		@Override
		public Integer call() {
			validate(localPort, "localPort");
			validate(remotePort, "remotePort");
			return mainLoop(localPort, remoteHost, remotePort);
		}
	}

	/** How to get the version number baked in by Maven. */
	static class Version implements IVersionProvider {
		private static final String DEFAULT = "0.1 (unpackaged)";

		@Override
		public String[] getVersion() throws Exception {
			return new String[] {
				requireNonNullElse(
						getClass().getPackage().getImplementationVersion(),
						DEFAULT)
			};
		}
	}

	/**
	 * The main method.
	 *
	 * @param args
	 *            {@code args[0]}: The local port to listen on <br>
	 *            {@code args[1]}: The remote host to proxy <br>
	 *            {@code args[2]}: The remote port to proxy
	 * @throws IOException
	 *             If we can't start the server.
	 * @throws IllegalArgumentException
	 *             If the wrong number of arguments are given.
	 */
	public static void main(String[] args) throws IOException {
		System.exit(new CommandLine(new CLI()).execute(args));
	}
}
