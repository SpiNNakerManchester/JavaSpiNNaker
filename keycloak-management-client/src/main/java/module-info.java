/*
 * Copyright (c) 2022 The University of Manchester
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

/**
 * Keycloak Client for talking to the HBP/EBRAINS authentication and
 * authorization service for the purpose of managing Spalloc's registration
 * there. You are not expected to ever need this, and even less expected to need
 * it without editing the source code!
 *
 * @author Donal Fellows
 */
module spinnaker.keycloak_management_tool {
	// External dependencies
	requires keycloak.client.registration.api;
	requires org.xerial.sqlitejdbc;
	requires com.fasterxml.jackson.annotation;
	requires com.fasterxml.jackson.databind;
	requires org.slf4j;

	opens uk.ac.manchester.spinnaker.tools
			to com.fasterxml.jackson.databind;
}
