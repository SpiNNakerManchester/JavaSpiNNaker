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
package uk.ac.manchester.spinnaker.machine.board;

import static java.lang.Integer.compare;
import static java.lang.Integer.parseUnsignedInt;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.validation.constraints.PositiveOrZero;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

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
 *
 * @author Donal Fellows
 */
@JsonDeserialize(using = BoardPhysicalCoords.Deserializer.class)
public final class BoardPhysicalCoords
		implements Comparable<BoardPhysicalCoords> {
	/** Cabinet number. */
	@PositiveOrZero(message = "cabinet number must not be negative")
	public final int cabinet;

	/** Frame number. */
	@PositiveOrZero(message = "frame number must not be negative")
	public final int frame;

	/** Board number. */
	@PositiveOrZero(message = "board number must not be negative")
	public final int board;

	/**
	 * Create an instance.
	 *
	 * @param c
	 *            Cabinet number.
	 * @param f
	 *            Frame number.
	 * @param b
	 *            Board number.
	 */
	@JsonCreator
	public BoardPhysicalCoords(@JsonProperty("cabinet") @JsonAlias("c") int c,
			@JsonProperty("frame") @JsonAlias("f") int f,
			@JsonProperty("board") @JsonAlias("b") int b) {
		this.cabinet = c;
		this.frame = f;
		this.board = b;
	}

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
	public BoardPhysicalCoords(String serialForm) {
		var m = PATTERN.matcher(serialForm);
		if (!m.matches()) {
			throw new IllegalArgumentException(
					"bad argument: " + serialForm);
		}
		int idx = 0;
		cabinet = parseUnsignedInt(m.group(++idx));
		frame = parseUnsignedInt(m.group(++idx));
		board = parseUnsignedInt(m.group(++idx));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BoardPhysicalCoords) {
			var other = (BoardPhysicalCoords) obj;
			return (cabinet == other.cabinet) && (frame == other.frame)
					&& (board == other.board);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (((cabinet << 2 + cabinet) ^ frame) << 2 + frame) ^ board;
	}

	@Override
	public String toString() {
		return "[c:" + cabinet + ",f:" + frame + ",b:" + board + "]";
	}

	/** @return The managing BMP address. */
	@JsonIgnore
	public BMPCoords getBmp() {
		return new BMPCoords(cabinet, frame);
	}

	/** @return The board number handle. */
	@JsonIgnore
	public BMPBoard getBoardNumber() {
		return new BMPBoard(board);
	}

	@Override
	public int compareTo(BoardPhysicalCoords other) {
		int cmp = compare(cabinet, other.cabinet);
		if (cmp != 0) {
			return cmp;
		}
		cmp = compare(frame, other.frame);
		if (cmp != 0) {
			return cmp;
		}
		return compare(board, other.board);
	}

	/** JSON deserializer for {@link BoardPhysicalCoords}. */
	static class Deserializer extends StdDeserializer<BoardPhysicalCoords> {
		private static final long serialVersionUID = 1L;

		protected Deserializer() {
			super(BoardPhysicalCoords.class);
		}

		@Override
		public BoardPhysicalCoords deserialize(JsonParser p,
				DeserializationContext ctxt)
				throws IOException, JacksonException {
			switch (p.currentToken()) {
			case START_ARRAY:
				return deserializeArray(p, ctxt);
			case START_OBJECT:
				return deserializeObject(p, ctxt);
			default:
				ctxt.handleUnexpectedToken(BoardPhysicalCoords.class, p);
				return null;
			}
		}

		private BoardPhysicalCoords deserializeArray(JsonParser p,
				DeserializationContext ctxt) throws IOException {
			if (!p.nextToken().isNumeric()) {
				ctxt.handleUnexpectedToken(int.class, p);
			}
			int c = p.getIntValue();
			if (!p.nextToken().isNumeric()) {
				ctxt.handleUnexpectedToken(int.class, p);
			}
			int f = p.getIntValue();
			if (!p.nextToken().isNumeric()) {
				ctxt.handleUnexpectedToken(int.class, p);
			}
			int b = p.getIntValue();
			if (!p.nextToken().isStructEnd()) {
				ctxt.handleUnexpectedToken(BoardPhysicalCoords.class, p);
			}
			return new BoardPhysicalCoords(c, f, b);
		}

		private BoardPhysicalCoords deserializeObject(JsonParser p,
				DeserializationContext ctxt) throws IOException {
			Integer c = null, f = null, b = null;
			while (true) {
				String name = p.nextFieldName();
				if (name == null) {
					if (p.currentToken() != JsonToken.END_OBJECT) {
						ctxt.handleUnexpectedToken(BoardPhysicalCoords.class,
								p);
					}
					break;
				}
				switch (name) {
				case "cabinet":
				case "c":
					if (c != null) {
						ctxt.handleUnknownProperty(p, this,
								BoardPhysicalCoords.class, name);
					}
					c = p.nextIntValue(0);
					break;
				case "frame":
				case "f":
					if (f != null) {
						ctxt.handleUnknownProperty(p, this,
								BoardPhysicalCoords.class, name);
					}
					f = p.nextIntValue(0);
					break;
				case "board":
				case "b":
					if (b != null) {
						ctxt.handleUnknownProperty(p, this,
								BoardPhysicalCoords.class, name);
					}
					b = p.nextIntValue(0);
					break;
				default:
					ctxt.handleUnknownProperty(p, this,
							BoardPhysicalCoords.class, name);
				}
			}
			if (c == null || f == null || b == null) {
				ctxt.handleUnexpectedToken(BoardPhysicalCoords.class, p);
			}
			return new BoardPhysicalCoords(c, f, b);
		}
	}
}
