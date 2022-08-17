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

import static java.util.Objects.isNull;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;

import java.net.URI;

import org.springframework.ui.ModelMap;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/** Utilities to support MVC controllers. */
public abstract class ControllerUtils {
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
	@UsedInJavadocOnly(MvcUriComponentsBuilder.class)
	public static URI uri(Object selfCall, Object... objects) {
		UriComponentsBuilder b = fromMethodCall(selfCall);
		// Force some dumb stuff to be right
		b.query(null).scheme("https");
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

	/**
	 * Convert a problem with validation into a user-suitable error message.
	 *
	 * @param error
	 *            The error detail.
	 * @return The message.
	 */
	public static String errorMessage(ObjectError error) {
		String msg = error.getDefaultMessage();
		return isNull(msg) ? error.toString() : msg;
	}

	/**
	 * Creates a {@link ModelAndView} on demand.
	 *
	 * @author Donal Fellows
	 */
	public static class ViewFactory {
		private final String view;

		/**
		 * @param viewName
		 *            The name of the view that this class will make.
		 */
		public ViewFactory(String viewName) {
			view = viewName;
		}

		/**
		 * Make an instance.
		 *
		 * @return The model-and-view, ready for decorating with the model.
		 */
		public ModelAndView view() {
			return new ModelAndView(view);
		}

		/**
		 * Make an instance.
		 *
		 * @param model
		 *            The model we want to start with.
		 * @return The model-and-view, ready for decorating with the model.
		 */
		public ModelAndView view(ModelMap model) {
			return new ModelAndView(view, model);
		}

		/**
		 * Make an instance.
		 *
		 * @param key
		 *            Name of item to initially insert in the model.
		 * @param value
		 *            Value of item to initially insert in the model.
		 * @return The model-and-view, ready for decorating with the model.
		 */
		public ModelAndView view(String key, Object value) {
			return new ModelAndView(view, key, value);
		}
	}
}
