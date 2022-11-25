/*
 * Copyright (c) 2018-2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.db;

import static jakarta.ws.rs.core.Response.status;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.slf4j.LoggerFactory.getLogger;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
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
