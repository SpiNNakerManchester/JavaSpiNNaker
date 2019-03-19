/*
 * Copyright (c) 2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.transceiver;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Documents a transceiver operation that should not be used from multiple
 * threads in parallel at all. Such operations need to be called only from
 * single-threaded code as they can interfere with each other (whether inside
 * the {@link uk.ac.manchester.spinnaker.connections.SCPConnection
 * SCPConnection}, inside SCAMP, or on the hardware itself).
 *
 * @see Transceiver
 * @see TransceiverInterface
 * @author Donal Fellows
 */
@Documented
@Inherited
@Retention(SOURCE)
@Target(METHOD)
public @interface ParallelUnsafe {
}
