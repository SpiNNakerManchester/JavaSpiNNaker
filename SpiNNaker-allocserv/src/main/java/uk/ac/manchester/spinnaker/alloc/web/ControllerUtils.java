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
package uk.ac.manchester.spinnaker.alloc.web;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;

import java.net.URI;

import org.slf4j.Logger;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

public abstract class ControllerUtils {
	private static final Logger log = getLogger(ControllerUtils.class);

	private ControllerUtils() {
	}

	/**
	 * Convert the result of calling a
	 * {@linkplain MvcUriComponentsBuilder#on(Class) component builder delegate}
	 * into a URL. Fixes the scheme to {@code https} and always ignores the
	 * query part.
	 * <p>
	 * <strong>Do not</strong> define a {@code static} variable holding the
	 * result of {@link MvcUriComponentsBuilder#on(Class)}; it tries to be
	 * request-aware, and that defeats it. Rely on the Spring MVC core to do the
	 * relevant object caching.
	 *
	 * @param selfCall
	 *            component builder delegate result
	 * @param objects
	 *            Values to substitute into the template; can be none at all
	 * @return URL that will invoke the method
	 * @see MvcUriComponentsBuilder
	 */
	public static URI uri(Object selfCall, Object...objects) {
		// No template variables in the overall controller, so can factor out
		UriComponentsBuilder b = fromMethodCall(selfCall).query(null);
		if (!b.build().getScheme().equalsIgnoreCase("https")) {
			log.warn("forcing scheme of generated URL to https: {}", b.build());
			b.scheme("https");
		}
		return b.buildAndExpand(objects).toUri();
	}

	/** The name of the Spring MVC error view. */
	public static final String MVC_ERROR = "erroroccurred";

	/**
	 * Create a view that shows an error to the user.
	 *
	 * @param message
	 *            The error message.
	 * @return The view.
	 */
	public static ModelAndView error(String message) {
		return new ModelAndView(MVC_ERROR, "error", message);
	}
}
