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
package uk.ac.manchester.spinnaker.data_spec.impl;

import uk.ac.manchester.spinnaker.data_spec.DataSpecificationException;

/**
 * How to actually call a DSE operation.
 *
 * @author Donal Fellows
 */
@FunctionalInterface
public interface Callable {
	/**
	 * The outer interface of a DSE operation. Note that this is subject to
	 * coercion to make the actual operations have a wider range of supported
	 * types.
	 *
	 * @param cmd
	 *            The encoded command word.
	 * @return Usually {@code 0}. Sometimes a marker to indicate special
	 *         states (currently just for end-of-specification).
	 * @throws DataSpecificationException
	 *             If anything goes wrong in the data specification.
	 */
	int execute(int cmd) throws DataSpecificationException;
}
