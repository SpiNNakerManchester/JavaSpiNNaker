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

import static java.lang.Integer.parseInt;
import static java.lang.Thread.interrupted;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.lang.reflect.Array;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.slf4j.Logger;

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
	private static final int BASE_TEN = 10;

	private static final double NANOFACTOR = 1e9;

	private static final Logger log = getLogger(V1CompatService.class);

	private Utils() {
	}

	/**
	 * A function that converts between values of two types.
	 *
	 * @param <T>
	 *            The type of the value to map from.
	 * @param <U>
	 *            The type of the value to map to. Must be instantiatable by
	 *            calling {@link Class#newInstance()}.
	 */
	@FunctionalInterface
	interface CheckedFunction<T, U> {
		/**
		 * Convert a value from one type to another.
		 *
		 * @param value
		 *            The value to convert from.
		 * @param receiver
		 *            The object to write the value into. This will have been
		 *            instantiated using the no-argument constructor.
		 * @throws SQLException
		 *             If database access fails.
		 */
		void call(T value, U receiver) throws SQLException;
	}

	/**
	 * The notification handler task core implementation. This is
	 * <em>designed</em> to be interrupted; the task does not complete until it
	 * is interrupted or an exception is thrown!
	 *
	 * @author Donal Fellows
	 */
	@FunctionalInterface
	interface Notifier extends Callable<Void> {
		@Override
		default Void call() {
			try {
				while (!interrupted()) {
					waitAndNotify();
				}
			} catch (SQLException e) {
				log.error("SQL failure", e);
			} catch (IOException e) {
				log.warn("failed to notify", e);
			} catch (InterruptedException ignored) {
				// Nothing to do
			} catch (RuntimeException e) {
				log.error("unexpected exception", e);
			}
			return null;
		}

		/**
		 * How to wait for an event and send a notification about it.
		 *
		 * @throws InterruptedException
		 *             If the wait is interrupted.
		 * @throws SQLException
		 *             If database access fails.
		 * @throws IOException
		 *             If network access fails.
		 */
		void waitAndNotify()
				throws InterruptedException, SQLException, IOException;
	}

	/**
	 * Parse a value as decimal.
	 *
	 * @param value
	 *            The value to parse (must actually be a string or a number).
	 * @return The decimal value.
	 */
	static int parseDec(Object value) {
		if (value instanceof Integer) {
			return (Integer) value;
		} else if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		return parseInt((String) value, BASE_TEN);
	}

	/**
	 * Parse a value as decimal.
	 *
	 * @param args
	 *            The list containing the value to parse (must actually be a
	 *            string).
	 * @param index
	 *            The index into the list.
	 * @return The decimal value.
	 */
	static int parseDec(List<Object> args, int index) {
		return parseDec((String) args.get(index));
	}

	/**
	 * Parse a value as decimal.
	 *
	 * @param kwargs
	 *            The map containing the value to parse (must actually be a
	 *            string).
	 * @param index
	 *            The index into the map.
	 * @return The decimal value.
	 */
	static int parseDec(Map<String, Object> kwargs, String index) {
		return parseDec((String) kwargs.get(index));
	}

	/**
	 * Convert an instant into an old-style timestamp.
	 *
	 * @param instant
	 *            The instant to convert
	 * @return The timestamp
	 */
	static double timestamp(Instant instant) {
		double ts = instant.getEpochSecond();
		ts += instant.getNano() / NANOFACTOR;
		return ts;
	}

	/**
	 * Convert the state of a job.
	 *
	 * @param job
	 *            The job.
	 * @return The converted state.
	 * @throws SQLException
	 *             If database access fails.
	 */
	static State state(Job job) throws SQLException {
		switch (job.getState()) {
		case QUEUED:
			return State.QUEUED;
		case POWER:
			return State.POWER;
		case READY:
			return State.READY;
		case DESTROYED:
			return State.DESTROYED;
		default:
			return State.UNKNOWN;
		}
	}

	/**
	 * Convert a board coordinate into another form.
	 *
	 * @param coords
	 *            The coordinate to convert.
	 * @return The converted coordinate.
	 */
	static BoardCoordinates board(BoardCoords coords) {
		BoardCoordinates bc = new BoardCoordinates();
		bc.setX(coords.getX());
		bc.setY(coords.getY());
		bc.setZ(coords.getZ());
		return bc;
	}

	/**
	 * Convert a down-link descriptor into another form.
	 *
	 * @param downLink
	 *            The link descriptor to convert.
	 * @return A stream of ends of the link.
	 */
	static Stream<BoardLink> boardLinks(DownLink downLink) {
		BoardLink bl1 = new BoardLink();
		bl1.setX(downLink.end1.board.getX());
		bl1.setY(downLink.end1.board.getY());
		bl1.setZ(downLink.end1.board.getZ());
		bl1.setLink(downLink.end1.direction.ordinal());

		BoardLink bl2 = new BoardLink();
		bl2.setX(downLink.end2.board.getX());
		bl2.setY(downLink.end2.board.getY());
		bl2.setZ(downLink.end2.board.getZ());
		bl2.setLink(downLink.end2.direction.ordinal());

		return Arrays.asList(bl1, bl2).stream();
	}

	/**
	 * Convert a collection into an array of items of a mapped type.
	 *
	 * @param <T>
	 *            The type of elements in the collection.
	 * @param <U>
	 *            The type of elements in the array.
	 * @param src
	 *            The source collection.
	 * @param cls
	 *            How to make instances of the array and the array itself.
	 * @param fun
	 *            The element conversion function.
	 * @return The array of converted elements.
	 * @throws SQLException
	 *             If database access fails.
	 */
	@SuppressWarnings("unchecked")
	static <T, U> U[] mapToArray(Collection<T> src, Class<U> cls,
			CheckedFunction<T, U> fun) throws SQLException {
		List<U> dst = new ArrayList<>();
		for (T val : src) {
			U target;
			try {
				target = cls.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				log.error("unexpected failure", e);
				break;
			}
			fun.call(val, target);
			dst.add(target);
		}
		return dst.toArray((U[]) Array.newInstance(cls, 0));
	}
}
