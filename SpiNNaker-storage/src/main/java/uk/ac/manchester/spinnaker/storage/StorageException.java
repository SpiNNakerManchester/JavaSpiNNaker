/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.storage;

/**
 * Exceptions caused by the storage system.
 *
 * @author Donal Fellows
 */
public class StorageException extends Exception {
	private static final long serialVersionUID = 3553555491656536568L;

	/**
	 * Create a storage exception.
	 *
	 * @param message
	 *            What overall operation was being done
	 * @param cause
	 *            What caused the problem
	 */
	public StorageException(String message, Throwable cause) {
		super(message + ": " + cause.getMessage(), cause);
	}
}
