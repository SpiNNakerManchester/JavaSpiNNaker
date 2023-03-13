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

	/** The status code. */
	private final Status code;

	/** The response message contents. */
	private final String message;

	/**
	 * Create an instance.
	 *
	 * @param code
	 *            The status code.
	 * @param message
	 *            The response message contents.
	 * @param cause
	 *            The cause of the exception.
	 */
	private RequestFailedException(Status code, String message,
			Throwable cause) {
		super(message, cause);
		this.code = code;
		this.message = message;
	}

	/**
	 * Create an instance.
	 *
	 * @param code
	 *            The status code.
	 * @param message
	 *            The response message contents.
	 */
	public RequestFailedException(Status code, String message) {
		this(code, message, null);
	}

	/**
	 * Create an instance that indicates an internal server error.
	 *
	 * @param cause
	 *            The cause of the server error. <em>This will be used as part
	 *            of the description of the failure.</em>
	 */
	public RequestFailedException(Throwable cause) {
		this(INTERNAL_SERVER_ERROR, "unexpected server problem", cause);
	}

	/**
	 * Convert this exception to a response.
	 *
	 * @return The response that this exception implies.
	 */
	Response toResponse() {
		var cause = getCause();
		if (cause instanceof WebApplicationException) {
			return ((WebApplicationException) cause).getResponse();
		} else if (cause != null) {
			// Be careful about what bits are extracted from message
			var cls = cause.getClass().getName().replaceFirst("^.*[.]", "")
					.replaceAll("Exception", "");
			var msg =
					cause.getMessage() != null ? ": " + cause.getMessage() : "";
			return status(code).type(TEXT_PLAIN)
					.entity(message + ": " + cls + msg).build();
		} else {
			return status(code).type(TEXT_PLAIN).entity(message).build();
		}
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

	/** A resource is no longer believed to exist. */
	public static class ItsGone extends RequestFailedException {
		private static final long serialVersionUID = 3774531853141947270L;

		/**
		 * @param message What message to use to say "its gone away".
		 */
		public ItsGone(String message) {
			super(GONE, message);
		}
	}

	/** A resource cannot be located. */
	public static class NotFound extends RequestFailedException {
		private static final long serialVersionUID = 5991697173204757030L;

		/**
		 * @param message What message to use to say "not found".
		 */
		public NotFound(String message) {
			super(NOT_FOUND, message);
		}

		/**
		 * @param message What message to use to say "not found".
		 * @param cause Why we are sending the message.
		 */
		public NotFound(String message, Throwable cause) {
			super(NOT_FOUND, message, cause);
		}
	}

	/** The client provided bad arguments in a request. */
	public static class BadArgs extends RequestFailedException {
		private static final long serialVersionUID = 7916573155067333350L;

		/**
		 * @param message What message to use to say "bad arguments".
		 */
		public BadArgs(String message) {
			super(BAD_REQUEST, message, null);
		}
	}

	/** The response is empty. */
	public static class EmptyResponse extends RequestFailedException {
		private static final long serialVersionUID = -2944836034264700912L;

		/** Create an instance. */
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
	 * @hidden
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
