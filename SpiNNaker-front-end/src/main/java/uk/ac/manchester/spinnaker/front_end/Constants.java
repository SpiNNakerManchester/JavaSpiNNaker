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
package uk.ac.manchester.spinnaker.front_end;

import static java.lang.Integer.getInteger;
import static java.lang.Math.max;

/** Miscellaneous constants. */
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
	/**
	 * Default value of {@link #PARALLEL_SIZE}.
	 */
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
	/**
	 * Default value of {@link #PARALLEL_SIZE}.
	 */
	private static final int NEXT_MSGS_DEFAULT = 7;
	/**
	 * The number of <em>next messages</em> that should be used in the data
	 * speed up gatherer protocol's retransmission mode.
	 */
	public static final int NEXT_MESSAGES_COUNT =
			// Zero or less make no sense at all
			max(0, getInteger(NEXT_MSGS_PROPERTY, NEXT_MSGS_DEFAULT));
}
