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
	// Base dependencies
	requires java.annotation;
	requires java.desktop;
	requires java.mail;
	requires java.validation;
	requires java.ws.rs;

	// Internal dependencies
	requires spinnaker.utils;
	requires spinnaker.machine;
	requires spinnaker.comms;
	requires spinnaker.storage;

	// External dependencies
	requires com.fasterxml.jackson.annotation;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.jaxrs.json;
	requires org.slf4j;

	requires org.apache.commons.io;
	requires org.apache.commons.lang3;
	requires commons.math3;

	requires spring.aop;
	requires spring.core;
	requires spring.beans;
	requires spring.context;
	requires spring.context.support;
	requires spring.expression;
	requires spring.boot;
	requires spring.boot.autoconfigure;

	requires spring.jdbc;
	requires spring.tx;
	requires org.xerial.sqlitejdbc;

	requires spring.web;
	requires spring.webmvc;
	requires spring.websocket;
	requires org.apache.cxf.core;
	requires org.apache.cxf.frontend.jaxrs;
	requires org.apache.cxf.rs.wadl;
	requires org.apache.cxf.rs.common.openapi;
	requires org.apache.cxf.rs.openapi.v3;
	requires org.apache.cxf.rs.swagger.ui;
	requires org.apache.cxf.transport.http;
	requires spring.boot.starter.websocket;
	requires org.apache.tomcat.embed.core;
	requires io.swagger.v3.oas.annotations;

	requires spring.security.core;
	requires spring.security.config;
	requires spring.security.crypto;
	requires spring.security.web;
	requires spring.security.oauth2.core;
	requires spring.security.oauth2.client;
	requires spring.security.oauth2.jose;
	requires spring.security.oauth2.resource.server;

	opens uk.ac.manchester.spinnaker.alloc
			to com.fasterxml.jackson.databind,
				spring.core, spring.beans, spring.context;
	opens uk.ac.manchester.spinnaker.alloc.db
			to spring.core, spring.beans, spring.context;
	opens uk.ac.manchester.spinnaker.alloc.model
			to com.fasterxml.jackson.databind;
	opens uk.ac.manchester.spinnaker.alloc.web
			to com.fasterxml.jackson.databind;
}
