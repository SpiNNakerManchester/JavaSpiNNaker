/*
 * Copyright (c) 2021-2022 The University of Manchester
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
 * @param cabinet
 *            The ID of the cabinet that contains the frame that contains the
 *            BMPs.
 * @param frame
 *            The ID of the frame that contains the master BMP. Frames are
 *            contained within a cabinet.
 */
@Immutable
@JsonDeserialize(using = BMPCoords.Deserializer.class)
public record BMPCoords(@ValidCabinetNumber int cabinet,
		@ValidFrameNumber int frame) implements Comparable<BMPCoords> {
	/** Parses the result of {@link #toString()}. */
	private static final Pattern PATTERN =
			Pattern.compile("^\\[c:(\\d+),f:(\\d+)\\]$");

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
	public static BMPCoords parse(String serialForm) {
		var m = PATTERN.matcher(serialForm);
		if (!m.matches()) {
			throw new IllegalArgumentException("bad argument: " + serialForm);
		}
		int idx = 0;
		int cabinet = parseInt(m.group(++idx));
		int frame = parseInt(m.group(++idx));
		return new BMPCoords(cabinet, frame);
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
				case "cabinet", "c" -> {
					c = requireSetOnceInt(name, c);
				}
				case "frame", "f" -> {
					f = requireSetOnceInt(name, f);
				}
				default -> unknownProperty(name);
				}
			}
			if (c == null || f == null) {
				missingProperty("c", c, "f", f);
			}
			return new BMPCoords(c, f);
		}

		@Override
		BMPCoords deserializeString(String string) {
			return BMPCoords.parse(string);
		}

		@Override
		public List<Object> getKnownPropertyNames() {
			return List.of("c", "f");
		}
	}
}
