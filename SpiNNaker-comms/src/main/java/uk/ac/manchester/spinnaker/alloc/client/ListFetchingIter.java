/*
 * Copyright (c) 2021-2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.client;

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
