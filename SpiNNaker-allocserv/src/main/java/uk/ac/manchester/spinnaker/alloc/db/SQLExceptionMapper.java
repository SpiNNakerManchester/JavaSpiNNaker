/*
 * Copyright (c) 2018-2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.db;

import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.slf4j.LoggerFactory.getLogger;

import java.sql.SQLException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.alloc.SpallocProperties;

/**
 * Handler for {@link SQLException}.
 *
 * @author Donal Fellows
 */
@Component
@Provider
public class SQLExceptionMapper
		implements ExceptionMapper<SQLException> {
	static final Logger log = getLogger(SQLExceptionMapper.class);

	@Autowired
	private SpallocProperties properties;

	@Override
	public Response toResponse(SQLException exception) {
		log.warn("uncaught SQL exception", exception);
		if (properties.getSqlite().isDebugFailures()) {
			return status(INTERNAL_SERVER_ERROR)
					.entity("failed: " + exception.getMessage()).build();
		}
		return status(INTERNAL_SERVER_ERROR).entity("failed").build();
	}
}
