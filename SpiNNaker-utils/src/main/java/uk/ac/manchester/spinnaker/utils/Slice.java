package uk.ac.manchester.spinnaker.utils;

/**
 * A description of a slice (range) of an object. Modelled after the concept
 * with the same name in Python. Note that this does not create the actual
 * range; it merely <i>describes</i> it.
 *
 * @author Donal Fellows
 */
public final class Slice {
	/** The index where the slice starts. */
	public final Integer start;
	/**
	 * The index where the slice stops. (One after the last accessible byte.)
	 */
	public final Integer stop;

	private Slice(Integer start, Integer stop) {
		this.start = start;
		this.stop = stop;
	}

	/**
	 * Create a new slice from the start position to the end of the IO object.
	 *
	 * @param start
	 *            Where to start.
	 * @return The slice
	 */
	public static Slice from(int start) {
		return new Slice(start, null);
	}

	/**
	 * Create a new slice from the beginning to the stop position of the IO
	 * object.
	 *
	 * @param end
	 *            Where to finish.
	 * @return The slice
	 */
	public static Slice to(int end) {
		return new Slice(null, end);
	}

	/**
	 * Create a new slice, from the the start position to the stop position, of
	 * the IO object.
	 *
	 * @param start
	 *            Where to start.
	 * @param end
	 *            Where to finish.
	 * @return The slice
	 */
	public static Slice over(int start, int end) {
		return new Slice(start, end);
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder("Slice(");
		if (start != null) {
			str.append(start);
		}
		str.append(";");
		if (stop != null) {
			str.append(stop);
		}
		return str.append(")").toString();
	}
}
