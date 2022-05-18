/*
 * Copyright (c) 2018 The University of Manchester
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
package testconfig;

import java.util.Collection;
import java.util.HashSet;

/**
 * Some of the printing utilities used by testing code.
 *
 * @author Donal Fellows
 */
public abstract class Utils {
	private Utils() {
	}

	public static void printEnumCollection(String name,
			Collection<? extends Enum<?>> enumList) {
		var sb = new StringBuilder(name).append(" ");
		for (var enumValue : enumList) {
			sb.append(enumValue.name()).append("; ");
		}
		System.out.println(sb.toString());
	}

	private static final int BITS_IN_WORD = 32;
	private static final int IN_TENS = 10;

	public static void printWordAsBinary(String name, int word,
			Field[] fields) {
		int start = 0;
		int end = BITS_IN_WORD;

		var startFields = new HashSet<>();
		var endFields = new HashSet<>();
		for (var field : fields) {
			startFields.add(field.from);
			endFields.add(field.to);
		}

		var prefix = new StringBuilder(" ");
		name.chars().forEachOrdered(c -> prefix.append(" "));
		var header1 = new StringBuilder(prefix);
		var header2 = new StringBuilder(prefix);
		var header3 = new StringBuilder(prefix);
		var mainline = new StringBuilder(name).append(" ");
		boolean sep = false;
		for (int i = start; i < end; i++) {
			if (!sep && startFields.contains(i)) {
				header1.append("|");
				header2.append("|");
				header3.append("|");
				mainline.append("|");
			}
			header1.append(i % IN_TENS == 0 ? i / IN_TENS : " ");
			header2.append(i % IN_TENS);
			header3.append("=");
			mainline.append((word >>> i) & 0x1);
			sep = endFields.contains(i);
			if (sep) {
				header1.append("|");
				header2.append("|");
				header3.append("|");
				mainline.append("|");
			}
		}
		System.out.println(header1);
		System.out.println(header2);
		System.out.println(header3);
		System.out.println(mainline);
	}

	/**
	 * Describes a binary field in an encoded word.
	 *
	 * @see #printWordAsBinary(String,int,Field[])
	 * @author Donal Fellows
	 */
	public static class Field {
		private int from, to;

		/**
		 * Make a field one bit wide.
		 *
		 * @param value
		 *            The field bit index.
		 */
		public Field(int value) {
			from = value;
			to = value;
		}

		/**
		 * Make a field multiple bits wide.
		 *
		 * @param from
		 *            The LSB of the field.
		 * @param to
		 *            The MSB of the field.
		 */
		public Field(int from, int to) {
			this.from = from;
			this.to = to;
		}
	}
}
