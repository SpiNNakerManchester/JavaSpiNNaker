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

import static java.util.Objects.isNull;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

import uk.ac.manchester.spinnaker.alloc.security.Permit;
import uk.ac.manchester.spinnaker.alloc.web.RequestFailedException.NotFound;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * Enables a web service class to move processing a request into a background
 * task easily.
 *
 * @author Donal Fellows
 */
@UsedInJavadocOnly(Response.class)
public abstract class BackgroundSupport {
	@Autowired
	private Executor executor;

	/**
	 * An action that produces a response, usually handled asynchronously. Care
	 * should be taken as the action may be run on a thread other than the
	 * thread that creates it.
	 *
	 * @author Donal Fellows
	 */
	@FunctionalInterface
	protected interface BackgroundAction extends Callable<Object> {
		/**
		 * {@inheritDoc}
		 *
		 * @return The result of the action. A {@code null} is mapped as a
		 *         generic 404 with no special message. Custom messages should
		 *         be supported by throwing a suitable
		 *         {@link RequestFailedException}. If the result is not a
		 *         {@link Response} and not a {@link Throwable}, it will be
		 *         returned as the entity that populates a {@code 200 OK} (and
		 *         so must be convertible to JSON).
		 * @throws RequestFailedException
		 *             If anything goes wrong. This is the <em>expected</em>
		 *             exception type; it is <em>not</em> logged.
		 * @throws Exception
		 *             If anything goes wrong. This is reported as an
		 *             <em>unexpected</em> exception and logged.
		 */
		@Override
		Object call() throws RequestFailedException, Exception;
	}

	/**
	 * Run the action in the background and wrap it into the response when it
	 * completes.
	 *
	 * @param response
	 *            The asynchronous response.
	 * @param action
	 *            The action that generates a {@link Response}
	 */
	protected void bgAction(AsyncResponse response, BackgroundAction action) {
		executor.execute(() -> fgAction(response, action));
	}

	/**
	 * Run the action in the background and wrap it into the response when it
	 * completes.
	 *
	 * @param response
	 *            The asynchronous response.
	 * @param permit
	 *            The permissions granted to the asynchronous task from the
	 *            context that asked for it to happen.
	 * @param action
	 *            The action that generates a {@link Response}
	 */
	protected void bgAction(AsyncResponse response, Permit permit,
			BackgroundAction action) {
		executor.execute(() -> permit.authorize(() -> {
			fgAction(response, action);
			return this; // dummy value
		}));
	}

	/**
	 * Run the action immediately and wrap it into the response.
	 *
	 * @param response
	 *            The asynchronous response.
	 * @param action
	 *            The action that generates a {@link Response}
	 */
	protected static void fgAction(AsyncResponse response,
			BackgroundAction action) {
		try {
			var r = action.call();
			if (isNull(r)) {
				// If you want something else, don't return null
				response.resume(new NotFound("not found"));
			} else {
				response.resume(r);
			}
		} catch (RequestFailedException | DataAccessException e) {
			// Known exception mappers for these
			response.resume(e);
		} catch (Exception e) {
			response.resume(
					new RequestFailedException("unexpected server problem", e));
		}
	}
}
