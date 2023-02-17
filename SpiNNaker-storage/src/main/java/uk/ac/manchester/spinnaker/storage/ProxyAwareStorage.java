/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.storage;

/**
 * Storage interface that knows how to get information about the proxy system
 * from the DB.
 *
 * @author Donal Fellows
 */
public non-sealed interface ProxyAwareStorage extends DatabaseAPI {
	/**
	 * Get the proxy information from the database.
	 *
	 * @return The proxy information, or {@code null} if none defined. When
	 *         there is no proxy, only direct connections to SpiNNaker are
	 *         possible.
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	ProxyInformation getProxyInformation() throws StorageException;
}
