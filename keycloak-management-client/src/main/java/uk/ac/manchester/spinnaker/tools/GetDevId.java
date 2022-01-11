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

public class GetDevId extends CredentialDB {
	private static final Logger log = getLogger(GetDevId.class);

	public static final String HBP_OPENID_BASE =
			"https://iam.ebrains.eu/auth/realms/hbp/";

	private static final String DEFAULT_DB = "ebrains-keycloak.sqlite3";

	private static ObjectMapper mapper =
			new ObjectMapper().enable(INDENT_OUTPUT);

	static final class ConsoleCredentials implements EBRAINSDevCredentials {
		private final Console console = System.console();

		@Override
		public String getUser() {
			return console.readLine("Enter your username: ");
		}

		@Override
		public String getPass() {
			return new String(console.readPassword("Enter your password: "));
		}
	}

	private Auth nextAuth;

	public GetDevId(File databaseFile) throws SQLException, IOException {
		super(databaseFile);
	}

	private ClientRegistration getRegistrationClient(String clientId) {
		ClientRegistration cr = ClientRegistration.create()
				.url(HBP_OPENID_BASE + "clients-registrations/").build();
		if (nextAuth != null) {
			cr.auth(nextAuth);
		} else if (clientId != null) {
			String token = getToken(clientId);
			if (token != null) {
				cr.auth(Auth.token(token));
			}
		}
		return cr;
	}

	private ClientRepresentation saveAuth(ClientRepresentation client) {
		String clientId = requireNonNull(client.getClientId());
		if (client.getRegistrationAccessToken() != null) {
			String registrationAccessToken =
					client.getRegistrationAccessToken();
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

	public ClientRepresentation makeClient(ClientRepresentation client,
			EBRAINSDevCredentials credentials)
			throws IOException, ClientRegistrationException {
		ClientRegistration reg = getRegistrationClient(null);
		reg.auth(Auth.token(credentials.getToken()));
		client = reg.create(client);
		return saveAuth(client);
	}

	public ClientRepresentation updateClient(ClientRepresentation client)
			throws ClientRegistrationException {
		ClientRegistration reg = getRegistrationClient(client.getClientId());
		client = reg.update(client);
		return saveAuth(client);
	}

	public ClientRepresentation getClient(String clientId)
			throws ClientRegistrationException {
		ClientRegistration reg = getRegistrationClient(clientId);
		ClientRepresentation client = reg.get(clientId);
		return saveAuth(client);
	}

	static ClientRepresentation makeUpdateSpallocDescriptor(String clientId) {
		ClientRepresentation client = new ClientRepresentation();
		client.setClientId(clientId);
		return client;
	}

	private EBRAINSDevCredentials obtainCredentials() throws SQLException {
		try {
			EBRAINSDevCredentials c = new DBContainedCredentials();
			log.info("using existing user credentials for {}", c.getUser());
			return c;
		} catch (IllegalStateException e) {
			return new ConsoleCredentials();
		}
	}

	public static void main(String... strings)
			throws IOException, SQLException {
		String id = SPALLOC_ID;
		File db = getDBFilename(strings);
		try (GetDevId gdi = new GetDevId(db)) {
			ClientRepresentation cr;
			if (gdi.getToken(id) != null) {
				cr = gdi.getClient(id);
				// cr = gdi.updateClient(makeSpallocDescriptor(id));
			} else {
				EBRAINSDevCredentials creds = gdi.obtainCredentials();
				cr = gdi.makeClient(makeSpallocDescriptor(id), creds);
				if (!(creds instanceof DBContainedCredentials)) {
					gdi.saveCredentials(creds);
				}
			}
			log.info("client-id = {}", cr.getClientId());
			log.info("secret = {}", cr.getSecret());
			log.debug("response:\n{}", mapper.writeValueAsString(cr));
		} catch (ClientRegistrationException e) {
			log.error("failed", e);
			if (e.getCause() instanceof HttpErrorException) {
				HttpErrorException ex = (HttpErrorException) e.getCause();
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
