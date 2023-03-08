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
package uk.ac.manchester.spinnaker.tools;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.keycloak.representations.idm.ClientRepresentation;

/**
 * Information about the {@code SpiNNaker-allocserv} application. Note that this
 * information is only really usable by the administrator of the deployment at
 * Manchester.
 *
 * @author Donal Fellows
 */
public interface SpiNNakerAllocatorApp {
	/** The ID of the service. */
	String SPALLOC_ID = "spalloc";

	/** The name of the service. */
	String SPALLOC_NAME = "SpiNNaker Board Resource Manager";

	/** The description of the service. */
	String SPALLOC_DESCRIPTION = "The SpiNNaker Board Resource Manager "
			+ "('spalloc') handles allocation of resources within the "
			+ "SpiNNaker1M machine at the University of Manchester.";

	/** The location of the service. */
	String SPALLOC_APP_BASE = "https://spinnaker.cs.man.ac.uk/spalloc/";

	/** Contact info for the service. Semicolon-separated list. */
	String SPALLOC_CONTACTS = "donal.k.fellows@manchester.ac.uk";

	/** Scopes we require: "openid", "profile", "roles". */
	List<String> SPALLOC_DEFAULT_SCOPES = List.of("openid", "profile", "roles");

	/** Scopes we can ask for: "team", "group", "email". */
	List<String> SPALLOC_OPTIONAL_SCOPES = List.of("team", "group", "email");

	// Some unavoidable long lines here, so we do:
	// CHECKSTYLE:OFF
	/**
	 * Create a proposed description of the service. From the wiki:
	 *
	 * <pre>
	 * # Send the creation request
	 * curl -X POST OIDC_CLIENT_CREATE_URL \
	 *  -H "Authorization: Bearer ${clb_dev_token}" \
	 *  -H 'Content-Type: application/json' \
	 *  -d '{
	 *         "clientId": "my-awesome-client",
	 *         "name": "My Awesome App",
	 *         "description": "This describes what my app is for end users",
	 *         "rootUrl": "https://root.url.of.my.app",
	 *         "baseUrl": "/relative/path/to/its/frontpage.html",
	 *         "redirectUris": [
	 *             "/relative/redirect/path",
	 *             "/these/can/use/wildcards/*"
	 *         ],
	 *         "webOrigins": ["+"],
	 *         "bearerOnly": false,
	 *         "consentRequired": true,
	 *         "standardFlowEnabled": true,
	 *         "implicitFlowEnabled": true,
	 *         "directAccessGrantsEnabled": false,
	 *         "attributes": {
	 *             "contacts":
	 *                 "first.contact@example.com; second.contact@example.com"
	 *         }
	 *     }' |
	 *
	 * # Pretty print the JSON response
	 * json_pp;
	 * </pre>
	 *
	 * @param clientId
	 *            The client ID
	 * @return The client description.
	 * @see <a href="https://wiki.ebrains.eu/bin/view/Collabs/the-collaboratory/Documentation%20IAM/FAQ/OIDC%20Clients%20explained/1.%20Registering%20an%20OIDC%20client/"
	 *      >wiki page</a>
	 * @see <a href="https://www.keycloak.org/docs-api/11.0/javadocs/org/keycloak/representations/idm/ClientRepresentation.html"
	 *      >Keycloak documentation</a>
	 */
	// CHECKSTYLE:ON
	static ClientRepresentation makeSpallocDescriptor(String clientId) {
		var client = new ClientRepresentation();
		client.setClientId(clientId);
		client.setName(SPALLOC_NAME);
		client.setDescription(SPALLOC_DESCRIPTION);
		client.setAttributes(Map.of("contacts", SPALLOC_CONTACTS));

		client.setRootUrl(SPALLOC_APP_BASE);
		client.setBaseUrl(SPALLOC_APP_BASE);
		client.setRedirectUris(List.of(SPALLOC_APP_BASE + "*"));
		client.setWebOrigins(List.of(SPALLOC_APP_BASE, "+"));

		client.setBearerOnly(false);
		client.setConsentRequired(true);
		client.setStandardFlowEnabled(true);
		client.setImplicitFlowEnabled(false);
		client.setDirectAccessGrantsEnabled(false);

		client.setDefaultClientScopes(SPALLOC_DEFAULT_SCOPES);
		client.setOptionalClientScopes(SPALLOC_OPTIONAL_SCOPES);

		return client;
	}

	/**
	 * Create a new client registration that is a delta from an old client
	 * registration.
	 *
	 * @param oldClient
	 *            The old client registration.
	 * @return The delta client registration document.
	 */
	static ClientRepresentation
			makeSpallocDescriptor(ClientRepresentation oldClient) {
		var newClient = makeSpallocDescriptor(oldClient.getClientId());
		var compressedClient = new ClientRepresentation();
		// Client ID must always be present
		compressedClient.setClientId(oldClient.getClientId());
		compressedClient.setId(oldClient.getId());
		if (!Objects.equals(oldClient.getName(), newClient.getName())) {
			compressedClient.setName(newClient.getName());
		}
		if (!Objects.equals(oldClient.getDescription(),
				newClient.getDescription())) {
			compressedClient.setDescription(newClient.getDescription());
		}
		if (!Objects.equals(oldClient.getAttributes(),
				newClient.getAttributes())) {
			compressedClient.setAttributes(newClient.getAttributes());
		}
		if (!Objects.equals(oldClient.getRootUrl(), newClient.getRootUrl())) {
			compressedClient.setRootUrl(newClient.getRootUrl());
		}
		if (!Objects.equals(oldClient.getBaseUrl(), newClient.getBaseUrl())) {
			compressedClient.setBaseUrl(newClient.getBaseUrl());
		}
		if (!Objects.equals(oldClient.getRedirectUris(),
				newClient.getRedirectUris())) {
			compressedClient.setRedirectUris(newClient.getRedirectUris());
		}
		if (!Objects.equals(oldClient.getWebOrigins(),
				newClient.getWebOrigins())) {
			compressedClient.setWebOrigins(newClient.getWebOrigins());
		}
		if (!Objects.equals(oldClient.isBearerOnly(),
				newClient.isBearerOnly())) {
			compressedClient.setBearerOnly(newClient.isBearerOnly());
		}
		if (!Objects.equals(oldClient.isConsentRequired(),
				newClient.isConsentRequired())) {
			compressedClient.setConsentRequired(newClient.isConsentRequired());
		}
		if (!Objects.equals(oldClient.isStandardFlowEnabled(),
				newClient.isStandardFlowEnabled())) {
			compressedClient
					.setStandardFlowEnabled(newClient.isStandardFlowEnabled());
		}
		if (!Objects.equals(oldClient.isImplicitFlowEnabled(),
				newClient.isImplicitFlowEnabled())) {
			compressedClient
					.setImplicitFlowEnabled(newClient.isImplicitFlowEnabled());
		}
		if (!Objects.equals(oldClient.isDirectAccessGrantsEnabled(),
				newClient.isDirectAccessGrantsEnabled())) {
			compressedClient.setDirectAccessGrantsEnabled(
					newClient.isDirectAccessGrantsEnabled());
		}
		if (!Objects.equals(oldClient.getDefaultClientScopes(),
				newClient.getDefaultClientScopes())) {
			compressedClient
					.setDefaultClientScopes(newClient.getDefaultClientScopes());
		}
		if (!Objects.equals(oldClient.getOptionalClientScopes(),
				newClient.getOptionalClientScopes())) {
			compressedClient.setOptionalClientScopes(
					newClient.getOptionalClientScopes());
		}
		return compressedClient;
	}
}
