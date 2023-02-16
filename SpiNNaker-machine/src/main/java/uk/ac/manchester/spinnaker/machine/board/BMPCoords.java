/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.machine.board;

import static java.lang.Integer.compare;
import static java.lang.Integer.parseInt;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.errorprone.annotations.Immutable;

/**
 * A simple description of a BMP to talk to. Supports equality and being used as
 * a hash key.
 * <p>
 * Although every board technically has a BMP, they are managed at the level of
 * a frame (when a sufficient quantity of boards is used, typically but not
 * necessarily 24). Cabinets contain frames.
 *
 * <h2>Serialisation Formats</h2>
 * Defaults to being serialised as a JSON object:
 * <pre>{"cabinet": 3, "frame": 2}</pre>
 * and can be deserialized from that, or:
 * <pre>{"c": 3, "f": 2}</pre>
 * It can also accept being deserialised from a JSON array, for a more compact
 * notation:
 * <pre>[3, 2]</pre>
 * Finally, it can also be deserialised from the string form created by the
 * {@link #toString()} method:
 * <pre>[c:3,f:2]</pre>
 *
 * @author Donal Fellows
 */
@Immutable
@JsonDeserialize(using = BMPCoords.Deserializer.class)
public final class BMPCoords implements Comparable<BMPCoords> {
	/** Parses the result of {@link #toString()}. */
	private static final Pattern PATTERN =
			Pattern.compile("^\\[c:(\\d+),f:(\\d+)\\]$");

	/** The ID of the cabinet that contains the frame that contains the BMPs. */
	@ValidCabinetNumber
	public final int cabinet;

	/**
	 * The ID of the frame that contains the master BMP. Frames are contained
	 * within a cabinet.
	 */
	@ValidFrameNumber
	public final int frame;

	/**
	 * Create an instance.
	 *
	 * @param cabinet
	 *            Cabinet number.
	 * @param frame
	 *            Frame number.
	 */
	public BMPCoords(int cabinet, int frame) {
		this.cabinet = cabinet;
		this.frame = frame;
	}

	/**
	 * Create an instance from its serial form. This is the form produced by
	 * {@link #toString()}. The serial form (where the numbers may vary) is:
	 *
	 * <pre>
	 * [c:34,f:12]
	 * </pre>
	 *
	 * @param serialForm
	 *            The form to deserialise.
	 * @throws IllegalArgumentException
	 *             If the string is not in the right form.
	 */
	@JsonCreator
	public BMPCoords(String serialForm) {
		var m = PATTERN.matcher(serialForm);
		if (!m.matches()) {
			throw new IllegalArgumentException("bad argument: " + serialForm);
		}
		int idx = 0;
		cabinet = parseInt(m.group(++idx));
		frame = parseInt(m.group(++idx));
	}

	/**
	 * @return The ID of the cabinet that contains the frame that contains the
	 *         BMPs.
	 */
	public int getCabinet() {
		return cabinet;
	}

	/**
	 * @return The ID of the frame that contains the master BMP. Frames are
	 *         contained within a cabinet.
	 */
	public int getFrame() {
		return frame;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BMPCoords) {
			var other = (BMPCoords) obj;
			return cabinet == other.cabinet && frame == other.frame;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return cabinet * 5 + frame;
	}

	@Override
	public String toString() {
		return "[c:" + cabinet + ",f:" + frame + "]";
	}

	@Override
	public int compareTo(BMPCoords other) {
		int cmp = compare(cabinet, other.cabinet);
		if (cmp != 0) {
			return cmp;
		}
		return compare(frame, other.frame);
	}

	/** JSON deserializer for {@link BMPCoords}. */
	static final class Deserializer extends DeserializerHelper<BMPCoords> {
		private static final long serialVersionUID = 1L;

		Deserializer() {
			super(BMPCoords.class);
		}

		@Override
		BMPCoords deserializeArray() throws IOException {
			int c = getNextIntOfArray();
			int f = getNextIntOfArray();
			requireEndOfArray();
			return new BMPCoords(c, f);
		}

		@Override
		BMPCoords deserializeObject() throws IOException {
			Integer c = null, f = null;
			String name;
			while ((name = getNextFieldName()) != null) {
				switch (name) {
				case "cabinet":
				case "c":
					c = requireSetOnceInt(name, c);
					break;
				case "frame":
				case "f":
					f = requireSetOnceInt(name, f);
					break;
				default:
					unknownProperty(name);
				}
			}
			if (c == null || f == null) {
				missingProperty("c", c, "f", f);
			}
			return new BMPCoords(c, f);
		}

		@Override
		BMPCoords deserializeString(String string) {
			return new BMPCoords(string);
		}

		@Override
		public List<Object> getKnownPropertyNames() {
			return List.of("c", "f");
		}
	}
}
