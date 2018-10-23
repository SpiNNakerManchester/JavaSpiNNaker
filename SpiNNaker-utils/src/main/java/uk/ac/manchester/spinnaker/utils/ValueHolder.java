package uk.ac.manchester.spinnaker.utils;

/**
 * A simple class that can optionally hold a single value. <i>This class is
 * modifiable.</i>
 *
 * @author Donal Fellows
 * @param <T>
 *            The type of value to hold.
 */
public final class ValueHolder<T> {
	private T value;

	/**
	 * Create an instance. The initial value held is {@code null}.
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

	/**
	 * Get the value held.
	 *
	 * @return The value held.
	 */
	public T getValue() {
		return value;
	}

	/**
	 * Set the value to hold.
	 *
	 * @param value
	 *            The new value to hold.
	 */
	public void setValue(T value) {
		this.value = value;
	}
}
