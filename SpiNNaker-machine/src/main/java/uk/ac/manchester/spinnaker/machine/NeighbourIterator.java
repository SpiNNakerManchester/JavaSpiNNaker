/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.machine;

import java.util.Iterator;

/**
 * Iterates over the coordinates of the neighbours of a chip.
 *
 * @author Christian-B
 */
class NeighbourIterator implements Iterator<ChipLocation> {
	private Iterator<Link> linksIter;

	/**
	 * @param linksIter
	 *            The links from the chip.
	 */
	NeighbourIterator(Iterator<Link> linksIter) {
		this.linksIter = linksIter;
	}

	/**
	 * @param linksIterable
	 *            The links from the chip.
	 */
	NeighbourIterator(Iterable<Link> linksIterable) {
		this.linksIter = linksIterable.iterator();
	}

	@Override
	public boolean hasNext() {
		return linksIter.hasNext();
	}

	@Override
	public ChipLocation next() {
		return linksIter.next().destination();
	}
}
