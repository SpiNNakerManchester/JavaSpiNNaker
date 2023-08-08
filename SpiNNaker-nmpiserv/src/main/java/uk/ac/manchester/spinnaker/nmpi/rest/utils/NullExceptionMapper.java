/*
 * Copyright (c) 2014 The University of Manchester
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
package uk.ac.manchester.spinnaker.nmpi.rest.utils;

import static java.util.Objects.isNull;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.slf4j.LoggerFactory.getLogger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;

/**
 * Convert NPEs into complaints about their most likely causes, bad inputs.
 *
 * @author Donal Fellows
 */
@Provider
public class NullExceptionMapper
		implements ExceptionMapper<NullPointerException> {
	/** Log. */
	private static final Logger logger = getLogger(NullExceptionMapper.class);

	@Override
	public Response toResponse(NullPointerException exception) {
		var msg = exception.getMessage();
		if (isNull(msg) || msg.isEmpty()) {
			msg = "bad parameter";
		}
		logger.info("trapped exception in service method", exception);
		return status(BAD_REQUEST).entity(msg).build();
	}
}
