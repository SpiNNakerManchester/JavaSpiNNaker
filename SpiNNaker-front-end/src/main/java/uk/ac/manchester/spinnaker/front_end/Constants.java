/*
 * Copyright (c) 2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end;

import static java.lang.Integer.getInteger;
import static java.lang.Math.max;

/** Miscellaneous constants that can be overridden by system properties. */
public abstract class Constants {
	/**
	 * The name of the system property defining the number of parallel tasks
	 * that should be executed at once. The fundamental unit of parallelisation
	 * is the SpiNNaker board, as there's a strict limit on just how much
	 * trickery can be pushed through a SCAMP process.
	 * <p>
	 * This is used to set the scaling size of various thread pool executors,
	 * and should be set so as to keep the network reasonably busy with a large
	 * job.
	 * <p>
	 * If a property with this name is absent, a default is used ({@code 4}).
	 */
	public static final String PARALLEL_PROPERTY = "spinnaker.parallel_tasks";

	/** Default value of {@link #PARALLEL_SIZE}. */
	private static final int PARALLEL_DEFAULT = 4;

	/**
	 * The number of parallel tasks that should be executed at once. The
	 * fundamental unit of parallelisation is the SpiNNaker board, as there's a
	 * strict limit on just how much trickery can be pushed through a SCAMP
	 * process.
	 * <p>
	 * This is used to set the scaling size of various thread pool executors,
	 * and should be set so as to keep the network reasonably busy with a large
	 * job.
	 */
	public static final int PARALLEL_SIZE =
			// Zero or less make no sense at all
			max(1, getInteger(PARALLEL_PROPERTY, PARALLEL_DEFAULT));

	/**
	 * The name of the system property defining the number of <em>next
	 * messages</em> that should be used in the data speed up gatherer
	 * protocol's retransmission mode.
	 * <p>
	 * If a property with this name is absent, a default is used ({@code 7}).
	 */
	public static final String NEXT_MSGS_PROPERTY = "spinnaker.next_messages";

	/** Default value of {@link #PARALLEL_SIZE}. */
	private static final int NEXT_MSGS_DEFAULT = 7;

	/**
	 * The number of <em>next messages</em> that should be used in the data
	 * speed up gatherer protocol's retransmission mode.
	 */
	public static final int NEXT_MESSAGES_COUNT =
			// Zero or less make no sense at all
			max(0, getInteger(NEXT_MSGS_PROPERTY, NEXT_MSGS_DEFAULT));

	/**
	 * The name of the system property defining the threshold at which retrieves
	 * are switched over to the fast (data-speed-up-packet-gatherer based)
	 * retrieve protocol. Below this threshold, the retrieves of data are done
	 * via a normal SCAMP memory read.
	 * <p>
	 * If a property with this name is absent, a default is used
	 * ({@code 40000}).
	 */
	public static final String SMALL_RETRIEVE_PROPERTY =
			"spinnaker.small_retrieve";

	/** Default value of {@link #SMALL_RETRIEVE_THRESHOLD}. */
	private static final int SMALL_RETRIEVE_DEFAULT = 40000;

	/**
	 * Retrieves of data that is less than this many bytes are done via a normal
	 * SCAMP memory read.
	 */
	public static final int SMALL_RETRIEVE_THRESHOLD =
			max(1, getInteger(SMALL_RETRIEVE_PROPERTY, SMALL_RETRIEVE_DEFAULT));

	/**
	 * Base SDRAM tag to use for core data.  This matches the constant in
	 * SpiNNFrontEndCommon.
	 */
	public static final int CORE_DATA_SDRAM_BASE_TAG = 200;
}
