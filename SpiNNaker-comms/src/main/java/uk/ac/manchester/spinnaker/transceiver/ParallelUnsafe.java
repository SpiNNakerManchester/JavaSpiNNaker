/*
 * Copyright (c) 2019 The University of Manchester
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
