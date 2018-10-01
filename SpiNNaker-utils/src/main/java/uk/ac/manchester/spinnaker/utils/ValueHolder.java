package uk.ac.manchester.spinnaker.utils;

/**
 * A simple class that can optionally hold a single value. <i>This class is
 * modifiable.</i>
 *
 * @author Donal Fellows
 * @param <T>
 *            The type of value to hold.
 */
public class ValueHolder<T> {
	/**
	 * The value held.
	 */
	public T value;

	/**
	 * Create an instance. The initial value held is <tt>null</tt>.
	 */
	public ValueHolder() {
	}

	/**
	 * Create an instance.
	 *
	 * @param value
	 *            The initial value to hold.
	 */
	public ValueHolder(T value) {
		this.value = value;
	}
}
