package testconfig;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
		StringBuilder sb = new StringBuilder(name).append(" ");
		for (Enum<?> enumValue : enumList) {
			sb.append(enumValue.name()).append("; ");
		}
		System.out.println(sb.toString());
	}

	public static void printWordAsBinary(String name, int word,
			Field[] fields) {
		int start = 0;
		int end = 32;

		Set<Integer> startFields = new HashSet<>();
		Set<Integer> endFields = new HashSet<>();
		for (Field field : fields) {
			startFields.add(field.from);
			endFields.add(field.to);
		}

		StringBuilder prefix = new StringBuilder(" ");
		name.chars().forEachOrdered(c -> prefix.append(" "));
		StringBuilder header1 = new StringBuilder(prefix);
		StringBuilder header2 = new StringBuilder(prefix);
		StringBuilder header3 = new StringBuilder(prefix);
		StringBuilder mainline = new StringBuilder(name).append(" ");
		boolean sep = false;
		for (int i = start; i < end; i++) {
			if (!sep && startFields.contains(i)) {
				header1.append("|");
				header2.append("|");
				header3.append("|");
				mainline.append("|");
			}
			header1.append(i % 10 == 0 ? i / 10 : " ");
			header2.append(i % 10);
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
			from = to = value;
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
