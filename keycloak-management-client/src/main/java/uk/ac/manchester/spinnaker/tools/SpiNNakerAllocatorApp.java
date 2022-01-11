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

import static java.util.Arrays.asList;

import java.util.Map;

import org.keycloak.representations.idm.ClientRepresentation;

public interface SpiNNakerAllocatorApp {
	String SPALLOC_ID = "spinnaker-spalloc";

	String SPALLOC_NAME = "SpiNNaker Board Resource Manager";

	String SPALLOC_DESCRIPTION = "The SpiNNaker Board Resource Manager "
			+ "('spalloc') handles allocation of resources within the "
			+ "SpiNNaker1M machine at the University of Manchester.";

	String SPALLOC_APP_BASE = "https://spinnaker.cs.man.ac.uk/spalloc/";

	String SPALLOC_CONTACTS = "donal.k.fellows@manchester.ac.uk";

	/**
	 * From the wiki.
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
	 *             "contacts": "first.contact@example.com; second.contact@example.com"
	 *         }
	 *     }' |
	 *
	 * # Pretty print the JSON response
	 * json_pp;
	 * </pre>
	 *
	 * @param clientId
	 *            The client ID
	 * @see <a href=
	 *      "https://wiki.ebrains.eu/bin/view/Collabs/the-collaboratory/Documentation%20IAM/FAQ/OIDC%20Clients%20explained/1.%20Registering%20an%20OIDC%20client/">wiki
	 *      page</a>
	 * @see <a href=
	 *      "https://www.keycloak.org/docs-api/11.0/javadocs/org/keycloak/representations/idm/ClientRepresentation.html">Keycloak
	 *      documentation</a>
	 */
	static ClientRepresentation makeSpallocDescriptor(String clientId) {
		ClientRepresentation client = new ClientRepresentation();
		client.setClientId(clientId);
		client.setName(SPALLOC_NAME);
		client.setDescription(SPALLOC_DESCRIPTION);
		client.setAttributes(Map.of("contacts", SPALLOC_CONTACTS));

		client.setRootUrl(SPALLOC_APP_BASE);
		client.setBaseUrl(SPALLOC_APP_BASE);
		client.setRedirectUris(asList(SPALLOC_APP_BASE + "*"));
		client.setWebOrigins(asList(SPALLOC_APP_BASE, "+"));

		client.setBearerOnly(false);
		client.setConsentRequired(true);
		client.setStandardFlowEnabled(true);
		client.setImplicitFlowEnabled(false);
		client.setDirectAccessGrantsEnabled(false);

		client.setDefaultClientScopes(asList("openid", "profile", "email"));
		client.setOptionalClientScopes(asList("team", "group"));

		return client;
	}
}
