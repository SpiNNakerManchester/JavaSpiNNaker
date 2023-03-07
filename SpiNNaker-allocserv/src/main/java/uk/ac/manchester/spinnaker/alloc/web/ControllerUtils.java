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

import static java.util.Objects.isNull;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;

import java.net.URI;

import org.springframework.ui.ModelMap;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import com.google.errorprone.annotations.CompileTimeConstant;
import com.google.errorprone.annotations.Immutable;

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
		var b = fromMethodCall(selfCall);
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
		var msg = error.getDefaultMessage();
		return isNull(msg) ? error.toString() : msg;
	}

	/**
	 * Creates a {@link ModelAndView} on demand.
	 *
	 * @author Donal Fellows
	 */
	@Immutable
	public static final class ViewFactory {
		private final String view;

		/**
		 * @param viewName
		 *            The name of the view that this class will make.
		 */
		public ViewFactory(@CompileTimeConstant String viewName) {
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
