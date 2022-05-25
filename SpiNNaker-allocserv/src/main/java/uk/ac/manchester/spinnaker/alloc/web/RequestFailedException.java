/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.web;

import static java.util.Objects.nonNull;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.Family.SERVER_ERROR;
import static org.slf4j.LoggerFactory.getLogger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Thrown to indicate various sorts of problems with the service. Very much like
 * a {@link WebApplicationException} except with a different handling strategy.
 *
 * @author Donal Fellows
 */
public class RequestFailedException extends RuntimeException {
	private static final long serialVersionUID = -7522760691720854101L;

	private final Status code;

	private final String message;

	public RequestFailedException(Status code, String message,
			Throwable cause) {
		super(message, cause);
		this.code = code;
		this.message = message;
	}

	public RequestFailedException(WebApplicationException exn) {
		// code will not be used
		this(NO_CONTENT, exn.getMessage(), exn);
	}

	public RequestFailedException(String message) {
		this(INTERNAL_SERVER_ERROR, message, null);
	}

	public RequestFailedException(Status code, String message) {
		this(code, message, null);
	}

	public RequestFailedException(String message, Throwable cause) {
		this(INTERNAL_SERVER_ERROR, message, cause);
	}

	public RequestFailedException(Throwable cause) {
		this(INTERNAL_SERVER_ERROR, cause.getMessage(), cause);
	}

	public RequestFailedException(Status code, Throwable cause) {
		this(code, cause.getMessage(), cause);
	}

	Response toResponse() {
		if (getCause() instanceof WebApplicationException) {
			return ((WebApplicationException) getCause()).getResponse();
		}
		return status(code).type(TEXT_PLAIN).entity(message).build();
	}

	private void log(Logger log) {
		var cause = getCause();

		if (code.getFamily().equals(SERVER_ERROR)) {
			if (nonNull(cause)) {
				log.error(message, cause);
			} else {
				log.error(message);
			}
		} else {
			if (nonNull(cause)) {
				log.debug(message, cause);
			} else {
				log.debug(message);
			}
		}
	}

	public static class ItsGone extends RequestFailedException {
		private static final long serialVersionUID = 3774531853141947270L;

		public ItsGone(String message) {
			super(GONE, message);
		}
	}

	public static class NotFound extends RequestFailedException {
		private static final long serialVersionUID = 5991697173204757030L;

		public NotFound(String message) {
			super(NOT_FOUND, message);
		}

		public NotFound(String message, Throwable cause) {
			super(NOT_FOUND, message, cause);
		}
	}

	public static class BadArgs extends RequestFailedException {
		private static final long serialVersionUID = 7916573155067333350L;

		public BadArgs(String message) {
			super(BAD_REQUEST, message, null);
		}
	}

	public static class EmptyResponse extends RequestFailedException {
		private static final long serialVersionUID = -2944836034264700912L;

		public EmptyResponse() {
			super(NO_CONTENT, "");
		}

		@Override
		Response toResponse() {
			return noContent().build();
		}
	}

	/**
	 * Handler for {@link RequestFailedException}.
	 *
	 * @author Donal Fellows
	 */
	@Component
	@Provider
	public static class RequestFailedExceptionMapper
			implements ExceptionMapper<RequestFailedException> {
		private static final Logger log = getLogger(SpallocServiceImpl.class);

		@Override
		public Response toResponse(RequestFailedException exception) {
			exception.log(log);
			return exception.toResponse();
		}
	}
}
