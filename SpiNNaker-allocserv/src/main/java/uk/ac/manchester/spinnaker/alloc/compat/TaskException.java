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
package uk.ac.manchester.spinnaker.alloc.compat;

/**
 * An exception that a task operation may throw. Such exceptions are converted
 * into suitable classic spalloc error response messages by the service message
 * handling layer.
 *
 * @author Donal Fellows
 * @see ExceptionResponse
 */
public final class TaskException extends Exception {
	private static final long serialVersionUID = 1L;

	TaskException(String msg) {
		super(msg);
	}
}
