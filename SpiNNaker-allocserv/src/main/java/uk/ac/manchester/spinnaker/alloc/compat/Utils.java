/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.compat;

import static java.lang.Integer.parseInt;
import static java.lang.Thread.interrupted;
import static java.util.Objects.isNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.NSEC_PER_SEC;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.springframework.dao.DataAccessException;

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Job;
import uk.ac.manchester.spinnaker.alloc.model.BoardCoords;
import uk.ac.manchester.spinnaker.alloc.model.DownLink;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardLink;
import uk.ac.manchester.spinnaker.spalloc.messages.State;

/**
 * Utility functions to support {@link V1CompatService}.
 *
 * @author Donal Fellows
 */
abstract class Utils {
	private static final Logger log = getLogger(V1CompatService.class);

	private Utils() {
	}

	/**
	 * The notification handler task core implementation. This is
	 * <em>designed</em> to be interrupted; the task does not complete until it
	 * is interrupted or an exception is thrown!
	 *
	 * @author Donal Fellows
	 */
	@FunctionalInterface
	interface Notifier {
		/**
		 * How to wait for an event and send a notification about it.
		 *
		 * @throws InterruptedException
		 *             If the wait is interrupted.
		 * @throws DataAccessException
		 *             If database access fails.
		 * @throws IOException
		 *             If network access fails.
		 */
		void waitAndNotify()
				throws InterruptedException, DataAccessException, IOException;

		/**
		 * Wrap a notifier into a callable, doing some of the exception
		 * handling.
		 *
		 * @param notifier
		 *            The notifier to wrap.
		 * @return The wrapped notifier.
		 */
		static Callable<Void> toCallable(Notifier notifier) {
			return () -> {
				try {
					while (!interrupted()) {
						notifier.waitAndNotify();
					}
				} catch (UnknownIOException e) {
					// Nothing useful we can do here
				} catch (DataAccessException e) {
					log.error("SQL failure", e);
				} catch (IOException e) {
					log.warn("failed to notify", e);
				} catch (InterruptedException ignored) {
					// Nothing to do
				} catch (RuntimeException e) {
					log.error("unexpected exception", e);
				}
				return null;
			};
		}
	}

	/**
	 * Parse a value as non-negative decimal.
	 *
	 * @param value
	 *            The value to parse (must actually be a string or a number).
	 * @return The decimal value.
	 * @throws IllegalArgumentException
	 *             If the object can't be converted to a number.
	 */
	static Integer parseDec(Object value) {
		int n;
		if (isNull(value)) {
			return null;
		} else if (value instanceof Number num) {
			n = num.intValue();
		} else if (value instanceof String s) {
			n = parseInt(s);
		} else {
			throw new IllegalArgumentException(
					"needed a number, got a " + value.getClass().getName());
		}
		if (n < 0) {
			throw new IllegalArgumentException("negative values not supported");
		}
		return n;
	}

	/**
	 * Get an argument from an argument list.
	 *
	 * @param <T>
	 *            The type of values in the list.
	 * @param args
	 *            The list containing the value.
	 * @param index
	 *            The index into the list.
	 * @return The value.
	 * @throws Oops
	 *             If the list doesn't have a value at that index.
	 */
	static <T> T getArgument(List<T> args, int index) {
		if (isNull(args) || index < 0 || index >= args.size()) {
			throw new Oops("missing argument at index " + index);
		}
		return args.get(index);
	}

	/**
	 * Get an argument from an argument map.
	 *
	 * @param <T>
	 *            The type of values in the map.
	 * @param kwargs
	 *            The map containing the value.
	 * @param index
	 *            The key into the map.
	 * @return The value.
	 * @throws Oops
	 *             If the map doesn't have a value with that key.
	 */
	static <T> T getArgument(Map<String, T> kwargs, String index) {
		if (isNull(kwargs) || !kwargs.containsKey(index)) {
			throw new Oops("missing keyword argument: " + index);
		}
		return kwargs.get(index);
	}

	/**
	 * Parse a value as non-negative decimal.
	 *
	 * @param <T>
	 *            The type of the values in the list (must actually be a
	 *            string or a number or a supertype thereof).
	 * @param args
	 *            The list containing the value to parse.
	 * @param index
	 *            The index into the list.
	 * @return The decimal value.
	 */
	static <T> int parseDec(List<T> args, int index) {
		return parseDec(getArgument(args, index));
	}

	/**
	 * Parse a value as non-negative decimal.
	 *
	 * @param <T>
	 *            The type of the values in the map (must actually be a
	 *            string or a number or a supertype thereof).
	 * @param kwargs
	 *            The map containing the value to parse.
	 * @param index
	 *            The index into the map.
	 * @return The decimal value.
	 */
	static <T> int parseDec(Map<String, T> kwargs, String index) {
		return parseDec(getArgument(kwargs, index));
	}

	/**
	 * Convert an instant into an old-style timestamp.
	 *
	 * @param instant
	 *            The instant to convert
	 * @return The timestamp
	 */
	static Double timestamp(Instant instant) {
		if (isNull(instant)) {
			return null;
		}
		double ts = instant.getEpochSecond();
		ts += instant.getNano() / (double) NSEC_PER_SEC;
		return ts;
	}

	/**
	 * Convert the state of a job.
	 *
	 * @param job
	 *            The job.
	 * @return The converted state.
	 */
	static State state(Job job) {
		// So trivial...
		return switch (job.getState()) {
		case QUEUED -> State.QUEUED;
		case POWER -> State.POWER;
		case READY -> State.READY;
		case DESTROYED -> State.DESTROYED;
		default -> State.UNKNOWN;
		};
	}

	/**
	 * Convert a board coordinate into another form.
	 *
	 * @param coords
	 *            The coordinate to convert.
	 * @return The converted coordinate.
	 */
	static BoardCoordinates board(BoardCoords coords) {
		return new BoardCoordinates(coords.x(), coords.y(), coords.z());
	}

	/**
	 * Convert a down-link descriptor into another form.
	 *
	 * @param downLink
	 *            The link descriptor to convert.
	 * @return A stream of ends of the link.
	 */
	static Stream<BoardLink> boardLinks(DownLink downLink) {
		var bl1 = new BoardLink(downLink.end1().board().x(),
				downLink.end1().board().y(), downLink.end1().board().z(),
				downLink.end1().direction().ordinal());

		var bl2 = new BoardLink(downLink.end2().board().x(),
				downLink.end2().board().y(), downLink.end2().board().z(),
				downLink.end2().direction().ordinal());

		return List.of(bl1, bl2).stream();
	}
}
