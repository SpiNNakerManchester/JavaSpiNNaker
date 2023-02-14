/*
 * Copyright (c) 2018-2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.model;

import com.google.errorprone.annotations.Immutable;

/** Information about a tag pool on an Ethernet-connected chip. */
@Immutable
public final class TagInfo {
	/**
	 * The timeout for transient IP tags (i.e., responses to SCP commands).
	 */
	public final IPTagTimeOutWaitTime transientTimeout;

	/** The count of the IP tag pool size. */
	public final int poolSize;

	/** The count of the number of fixed IP tag entries. */
	public final int fixedSize;

	/**
	 * @param transientTimeout
	 *            The timeout for transient IP tags (i.e., responses to SCP
	 *            commands).
	 * @param poolSize
	 *            The count of the IP tag pool size.
	 * @param fixedSize
	 *            The count of the number of fixed IP tag entries.
	 */
	public TagInfo(IPTagTimeOutWaitTime transientTimeout, int poolSize,
			int fixedSize) {
		this.transientTimeout = transientTimeout;
		this.poolSize = poolSize;
		this.fixedSize = fixedSize;
	}
}
