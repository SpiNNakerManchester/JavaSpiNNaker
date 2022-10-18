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
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.IOException;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
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
	static class Deserializer extends StdDeserializer<BMPCoords> {
		private static final long serialVersionUID = 1L;

		protected Deserializer() {
			super(BMPCoords.class);
		}

		@Override
		public BMPCoords deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JacksonException {
			switch (p.currentToken()) {
			case START_ARRAY:
				return deserializeArray(p, ctxt);
			case START_OBJECT:
				return deserializeObject(p, ctxt);
			case VALUE_STRING:
				return new BMPCoords(p.getValueAsString());
			default:
				ctxt.handleUnexpectedToken(_valueClass, p);
				return null;
			}
		}

		private BMPCoords deserializeArray(JsonParser p,
				DeserializationContext ctxt) throws IOException {
			if (!p.nextToken().isNumeric()) {
				ctxt.handleUnexpectedToken(int.class, p);
			}
			int c = p.getIntValue();
			if (!p.nextToken().isNumeric()) {
				ctxt.handleUnexpectedToken(int.class, p);
			}
			int f = p.getIntValue();
			if (!p.nextToken().isStructEnd()) {
				ctxt.handleUnexpectedToken(_valueClass, p);
			}
			return new BMPCoords(c, f);
		}

		private BMPCoords deserializeObject(JsonParser p,
				DeserializationContext ctxt) throws IOException {
			Integer c = null, f = null;
			while (true) {
				String name = p.nextFieldName();
				if (isNull(name)) {
					if (p.currentToken() != JsonToken.END_OBJECT) {
						ctxt.handleUnexpectedToken(_valueClass, p);
					}
					break;
				}
				switch (name) {
				case "cabinet":
				case "c":
					if (nonNull(c)) {
						ctxt.handleUnknownProperty(p, this, _valueClass, name);
					}
					c = p.nextIntValue(0);
					break;
				case "frame":
				case "f":
					if (nonNull(f)) {
						ctxt.handleUnknownProperty(p, this, _valueClass, name);
					}
					f = p.nextIntValue(0);
					break;
				default:
					ctxt.handleUnknownProperty(p, this, _valueClass, name);
				}
			}
			if (isNull(c) || isNull(f)) {
				ctxt.handleUnexpectedToken(_valueClass, p);
			}
			return new BMPCoords(c, f);
		}
	}
}
