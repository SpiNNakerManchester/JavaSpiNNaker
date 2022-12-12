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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.errorprone.annotations.Immutable;

/**
 * Physical board coordinates. The {@code cabinet} and {@code frame} (with
 * multiple frames per cabinet) describe where a board is located within the
 * physical layout of the machine (and also which BMP is managing it, as there
 * is one managing BMP per frame). The {@code board} number says which board
 * within the frame is being referred to.
 *
 * <h2>Serialisation Formats</h2>
 * Defaults to being serialised as a JSON object:
 * <pre>{"cabinet": 3, "frame": 2, "board": 1}</pre>
 * and can be deserialized from that, or:
 * <pre>{"c": 3, "f": 2, "b": 1}</pre>
 * It can also accept being deserialised from a JSON array, for a more compact
 * notation:
 * <pre>[3, 2, 1]</pre>
 * Finally, it can also be deserialised from the string form created by the
 * {@link #toString()} method:
 * <pre>[c:3,f:2,b:1]</pre>
 *
 * @author Donal Fellows
 * @param c
 *            Cabinet number.
 * @param f
 *            Frame number.
 * @param b
 *            Board number.
 */
@Immutable
@JsonDeserialize(using = PhysicalCoords.Deserializer.class)
public record PhysicalCoords(//
		@JsonProperty("c") @ValidCabinetNumber int c,
		@JsonProperty("f") @ValidFrameNumber int f,
		@JsonProperty("b") @ValidBoardNumber int b)
		implements Comparable<PhysicalCoords> {
	private static final Pattern PATTERN =
			Pattern.compile("^\\[c:(\\d+),f:(\\d+),b:(\\d+)\\]$");

	/**
	 * Create an instance from its serial form. The serial form (where the
	 * numbers may vary) is:
	 *
	 * <pre>
	 * [c:34,f:12,b:23]
	 * </pre>
	 *
	 * @param serialForm
	 *            The form to deserialise.
	 * @throws IllegalArgumentException
	 *             If the string is not in the right form.
	 */
	@JsonCreator
	public static PhysicalCoords parse(String serialForm) {
		var m = PATTERN.matcher(serialForm);
		if (!m.matches()) {
			throw new IllegalArgumentException("bad argument: " + serialForm);
		}
		int idx = 0;
		int c = parseInt(m.group(++idx));
		int f = parseInt(m.group(++idx));
		int b = parseInt(m.group(++idx));
		return new PhysicalCoords(c, f, b);
	}

	@Override
	public String toString() {
		return "[c:" + c + ",f:" + f + ",b:" + b + "]";
	}

	/**
	 * @return The coordinates of the BMP that manages this board.
	 */
	@JsonIgnore
	public BMPCoords getBmpCoords() {
		return new BMPCoords(c, f);
	}

	@Override
	public int compareTo(PhysicalCoords other) {
		int cmp = compare(c, other.c);
		if (cmp != 0) {
			return cmp;
		}
		cmp = compare(f, other.f);
		if (cmp != 0) {
			return cmp;
		}
		return compare(b, other.b);
	}

	/** JSON deserializer for {@link PhysicalCoords}. */
	static final class Deserializer extends DeserializerHelper<PhysicalCoords> {
		private static final long serialVersionUID = 1L;

		Deserializer() {
			super(PhysicalCoords.class);
		}

		@Override
		PhysicalCoords deserializeArray() throws IOException {
			int c = getNextIntOfArray();
			int f = getNextIntOfArray();
			int b = getNextIntOfArray();
			requireEndOfArray();
			return new PhysicalCoords(c, f, b);
		}

		@Override
		PhysicalCoords deserializeObject() throws IOException {
			Integer c = null, f = null, b = null;
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
				case "board":
				case "b":
					b = requireSetOnceInt(name, b);
					break;
				default:
					unknownProperty(name);
				}
			}
			if (c == null || f == null || b == null) {
				missingProperty("c", c, "f", f, "b", b);
			}
			return new PhysicalCoords(c, f, b);
		}

		@Override
		PhysicalCoords deserializeString(String string) {
			return PhysicalCoords.parse(string);
		}

		@Override
		public List<Object> getKnownPropertyNames() {
			return List.of("c", "f", "b");
		}
	}
}
