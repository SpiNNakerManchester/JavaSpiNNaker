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
package uk.ac.manchester.spinnaker.allocator;

import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;

/**
 * Helper that manages fetching lists of items from a sequence of URLs. Used to
 * handle paging.
 *
 * @param <T>
 *            The type of elements of the lists retrieved.
 * @author Donal Fellows
 */
abstract class ListFetchingIter<T> implements Iterator<List<T>> {
	private static final Logger log = getLogger(ListFetchingIter.class);

	private boolean done = false;

	private List<T> handles = null;

	@Override
	public final boolean hasNext() {
		if (!done) {
			if (handles == null && canFetchMore()) {
				try {
					handles = fetchNext();
				} catch (IOException e) {
					log.warn("problem when fetching list section", e);
					handles = null;
				}
			}
			if (handles == null || handles.isEmpty()) {
				done = true;
			}
		}
		return !done;
	}

	@Override
	public final List<T> next() {
		if (handles == null) {
			throw new NoSuchElementException("no more");
		}
		try {
			return handles;
		} finally {
			handles = null;
			if (!canFetchMore()) {
				done = true;
			}
		}
	}

	/**
	 * Get the next chunk. Advance the internal state of what chunk to fetch.
	 *
	 * @return The list of items from the chunk. This list may be empty or
	 *         {@code null}, but either will indicate that the sequence of items
	 *         is exhausted.
	 * @throws IOException
	 *             If the fetch fails.
	 */
	abstract List<T> fetchNext() throws IOException;

	/**
	 * Whether there is the ability to fetch another chunk.
	 *
	 * @return {@code true} if {@link #fetchNext()} will do something sensible.
	 */
	abstract boolean canFetchMore();

	/**
	 * Get the elements of this iterator as a stream.
	 *
	 * @return a sequential stream.
	 */
	public Stream<List<T>> stream() {
		return StreamSupport.stream(
				spliteratorUnknownSize(this, ORDERED | IMMUTABLE), false);
	}
}
