/*
 * Copyright (c) 2018-2023 The University of Manchester
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
		private int from;

		private int to;

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
