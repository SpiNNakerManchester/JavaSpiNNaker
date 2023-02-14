/*
 * Copyright (c) 2019-2022 The University of Manchester
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

import static difflib.DiffUtils.diff;
import static uk.ac.manchester.spinnaker.utils.ByteBufferUtils.sliceUp;
import static java.util.stream.Collectors.toList;

import java.nio.ByteBuffer;
import java.util.List;

import org.slf4j.Logger;

import difflib.Chunk;
import uk.ac.manchester.spinnaker.utils.MathUtils;

/**
 * Utilities for debugging.
 */
public abstract class DebuggingUtils {
	private DebuggingUtils() {
	}

	/**
	 * Compare two buffers and write a description of how they differ to the
	 * given log (if they actually differ).
	 *
	 * @param original
	 *            The data to compare against. The ground truth.
	 * @param downloaded
	 *            The data that is being checked for differences.
	 * @param log
	 *            Where to write messages on differences being found.
	 */
	public static void compareBuffers(ByteBuffer original,
			ByteBuffer downloaded, Logger log) {
		for (int i = 0; i < original.remaining(); i++) {
			if (original.get(i) != downloaded.get(i)) {
				log.error("downloaded buffer contents different");
				for (var delta : diff(list(original), list(downloaded))
						.getDeltas()) {
					switch (delta.getType()) {
					case CHANGE -> {
						var changeFrom = delta.getOriginal();
						var changeTo = delta.getRevised();
						log.warn(
								"swapped {} bytes (SCP) for {} (gather) "
										+ "at {}->{}",
								changeFrom.getLines().size(),
								changeTo.getLines().size(),
								changeFrom.getPosition(),
								changeTo.getPosition());
						log.info("change {} -> {}", describeChunk(changeFrom),
								describeChunk(changeTo));
					}
					case DELETE -> {
						var delete = delta.getOriginal();
						log.warn("gather deleted {} bytes at {}",
								delete.getLines().size(), delete.getPosition());
						log.info("delete {}", describeChunk(delete));
					}
					default /* INSERT */ -> {
						var insert = delta.getRevised();
						log.warn("gather inserted {} bytes at {}",
								insert.getLines().size(), insert.getPosition());
						log.info("insert {}", describeChunk(insert));
					}
					}
				}
				break;
			}
		}
	}

	private static List<Byte> list(ByteBuffer buffer) {
		return sliceUp(buffer, 1).map(ByteBuffer::get).toList();
	}

	private static List<String> describeChunk(Chunk<Byte> chunk) {
		return chunk.getLines().stream().map(MathUtils::hexbyte)
				.collect(toList());
	}
}
