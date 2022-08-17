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
 * SpiNNaker Allocation Server. This implements an allocation service for a
 * SpiNNaker machine (or multiple machines), and a web interface for monitoring
 * the behaviour of the service, and an administration interface for safely
 * doing tasks like controlling hardware blacklists.
 * <p>
 * It's implemented using CXF (for the service interface) and Spring MVC (for
 * the UI) sitting within a Spring Boot/Tomcat container. It is intended to sit
 * behind nginx and in a privileged location with respect to suitable firewalls
 * and the outside world. Security is handled by Spring Security; login with
 * OpenID Connect (especially from HBP/EBRAINS) is a key target.
 *
 * @author Donal Fellows
 */
module spinnaker.allocator.server {
	// External dependencies
	requires org.xerial.sqlitejdbc;
	requires com.fasterxml.jackson.annotation;
	requires com.fasterxml.jackson.databind;
	requires org.slf4j;
	requires org.apache.commons.io;
	requires spinnaker.comms;

	opens uk.ac.manchester.spinnaker.alloc
			to com.fasterxml.jackson.databind;
	opens uk.ac.manchester.spinnaker.alloc.model
			to com.fasterxml.jackson.databind;
	opens uk.ac.manchester.spinnaker.alloc.web
			to com.fasterxml.jackson.databind;
}
