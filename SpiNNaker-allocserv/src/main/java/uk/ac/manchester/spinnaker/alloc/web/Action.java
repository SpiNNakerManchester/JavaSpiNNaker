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
package uk.ac.manchester.spinnaker.alloc.web;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import uk.ac.manchester.spinnaker.alloc.admin.AdminControllerImpl;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * Describes what action a method is to take. Used to enhance logging in
 * {@link SystemControllerImpl} and {@link AdminControllerImpl}.
 */
@Target(METHOD)
@Retention(RUNTIME)
@UsedInJavadocOnly(AdminControllerImpl.class)
public @interface Action {
	/** @return The action we do in the annotated method. */
	String value();
}
