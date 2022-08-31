/*
 * Copyright (c) 2021-2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.tools;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static java.lang.System.getProperty;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.tools.SpiNNakerAllocatorApp.SPALLOC_ID;
import static uk.ac.manchester.spinnaker.tools.SpiNNakerAllocatorApp.makeSpallocDescriptor;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.representations.idm.ClientRepresentation;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The keycloak registration client.
 *
 * @author Donal Fellows
 */
public class GetDevId extends CredentialDB {
	private static final Logger log = getLogger(GetDevId.class);

	/** The OIDC realm name. */
	public static final String REALM = "hbp";

	/** Where the keycloak service is. */
	public static final String HBP_OPENID_BASE =
			"https://iam.ebrains.eu/auth/";

	private static final String DEFAULT_DB = "ebrains-keycloak.sqlite3";

	private static ObjectMapper mapper =
			new ObjectMapper().enable(INDENT_OUTPUT);

	/**
	 * A username and password that are prompted for on the console.
	 *
	 * @author Donal Fellows
	 */
	static final class ConsoleCredentials implements EBRAINSDevCredentials {
		private final Console console = System.console();

		private String user;

		private String pass;

		@Override
		public String getUser() {
			if (user == null) {
				user = console.readLine("Enter your username: ");
			}
			return user;
		}

		@Override
		public String getPass() {
			if (pass == null) {
				pass = new String(
						console.readPassword("Enter your password: "));
			}
			return pass;
		}
	}

	private Auth nextAuth;

	public GetDevId(File databaseFile) throws SQLException, IOException {
		super(databaseFile);
	}

	private ClientRegistration getRegistrationClient(String clientId) {
		var cr = ClientRegistration.create().url(HBP_OPENID_BASE, REALM)
				.build();
		if (nextAuth != null) {
			cr.auth(nextAuth);
		} else if (clientId != null) {
			var token = getToken(clientId);
			if (token != null) {
				cr.auth(Auth.token(token));
			}
		}
		return cr;
	}

	private ClientRepresentation saveAuth(ClientRepresentation client) {
		var clientId = requireNonNull(client.getClientId());
		if (client.getRegistrationAccessToken() != null) {
			var registrationAccessToken = client.getRegistrationAccessToken();
			log.debug("RAT: {}", registrationAccessToken);
			nextAuth = Auth.token(client);
			try {
				saveToken(clientId, registrationAccessToken);
			} catch (SQLException e) {
				log.error(
						"failed to save client {} registration access token {}",
						clientId, registrationAccessToken, e);
			}
		}
		return client;
	}

	/**
	 * Create a client service instance. Will remember the registration access
	 * token for the client for you.
	 *
	 * @param client
	 *            The proposed description of client.
	 * @param credentials
	 *            The registering developer's credentials.
	 * @return The <em>actual</em> description of the client.
	 * @throws IOException
	 *             If we can't get the developer token.
	 * @throws ClientRegistrationException
	 *             If registration fails.
	 */
	public ClientRepresentation makeClient(ClientRepresentation client,
			EBRAINSDevCredentials credentials)
			throws IOException, ClientRegistrationException {
		var reg = getRegistrationClient(null);
		reg.auth(Auth.token(credentials.getToken()));
		client = reg.create(client);
		return saveAuth(client);
	}

	/**
	 * Update a client service registration. Will remember the registration
	 * access token for the client for you.
	 *
	 * @param client
	 *            The proposed new client service description. Must include the
	 *            client ID.
	 * @return The full updated client service description
	 * @throws ClientRegistrationException
	 *             If the update fails
	 */
	public ClientRepresentation updateClient(ClientRepresentation client)
			throws ClientRegistrationException {
		var reg = getRegistrationClient(client.getClientId());
		client = reg.update(client);
		return saveAuth(client);
	}

	/**
	 * Get the current client service registration for a client. Will remember
	 * the registration access token for the client for you (if one is
	 * provided).
	 *
	 * @param clientId
	 *            The ID of the client service registration.
	 * @return The full client service description.
	 * @throws ClientRegistrationException
	 *             If the read fails.
	 */
	public ClientRepresentation getClient(String clientId)
			throws ClientRegistrationException {
		var reg = getRegistrationClient(clientId);
		var client = reg.get(clientId);
		return saveAuth(client);
	}

	/**
	 * Make a partial client descriptor, suitable for preparing an update.
	 *
	 * @param clientId
	 *            The ID of the client to update.
	 * @return The partial client description. Should be filled out by the
	 *         caller with the parts to update.
	 */
	static ClientRepresentation makeUpdateSpallocDescriptor(String clientId) {
		var client = new ClientRepresentation();
		client.setClientId(clientId);
		return client;
	}

	/**
	 * Get the developer credentials, either from the DB or by querying the
	 * user.
	 *
	 * @return The developer credentials.
	 * @throws SQLException
	 *             If database access fails.
	 */
	private EBRAINSDevCredentials obtainCredentials() throws SQLException {
		try {
			var c = new DBContainedCredentials();
			log.info("using existing user credentials for {}", c.getUser());
			return c;
		} catch (IllegalStateException e) {
			return new ConsoleCredentials();
		}
	}

	/**
	 * Create the client or read its current values. Write the client ID and
	 * secret to the screen so that they can be inserted into a configuration
	 * file.
	 *
	 * @param arguments
	 *            Command line arguments
	 * @throws IOException
	 *             If I/O or filesystem access fails
	 * @throws SQLException
	 *             If database access fails
	 */
	public static void main(String... arguments)
			throws IOException, SQLException {
		var id = SPALLOC_ID;
		var db = getDBFilename(arguments);
		try (var gdi = new GetDevId(db)) {
			ClientRepresentation cr;
			if (gdi.getToken(id) != null) {
				cr = gdi.getClient(id);
				// cr = gdi.updateClient(makeSpallocDescriptor(id));
			} else {
				var creds = gdi.obtainCredentials();
				cr = gdi.makeClient(makeSpallocDescriptor(id), creds);
				if (!(creds instanceof DBContainedCredentials)) {
					gdi.saveCredentials(creds);
				}
			}
			log.info("ID = {}", cr.getId());
			log.info("client-id = {}", cr.getClientId());
			log.info("secret = {}", cr.getSecret());
			log.debug("response:\n{}", mapper.writeValueAsString(cr));
		} catch (ClientRegistrationException e) {
			log.error("failed", e);
			if (e.getCause() instanceof HttpErrorException) {
				var ex = (HttpErrorException) e.getCause();
				log.info("status: {}", ex.getStatusLine().getStatusCode());
				log.info("response: {}", ex.getErrorResponse());
			}
		}
	}

	private static File getDBFilename(String... strings) {
		if (strings.length > 0) {
			return new File(strings[0]);
		} else {
			return new File(getProperty("user.home"), DEFAULT_DB);
		}
	}
}
