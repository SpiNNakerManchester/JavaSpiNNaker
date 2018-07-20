package uk.ac.manchester.spinnaker.utils;

import java.util.HashMap;
import java.util.function.Supplier;

/**
 * A map that will extend itself with new values (on get) when the key is
 * otherwise absent from the map.
 *
 * @author Donal Fellows
 *
 * @param <K>
 *            The type of keys.
 * @param <V>
 *            The type of values.
 */
@SuppressWarnings("serial")
public class DefaultMap<K, V> extends HashMap<K, V> {
	private final boolean direct;
	private final V defValue;
	private final Supplier<V> defFactory;

	/**
	 * Create a new map.
	 *
	 * @param defaultValue
	 *            The default value to use in the map. This should be an
	 *            immutable value as it can be potentially inserted for many
	 *            keys.
	 */
	public DefaultMap(V defaultValue) {
		direct = true;
		defValue = defaultValue;
		defFactory = null;
	}

	/**
	 * Create a new map.
	 *
	 * @param defaultFactory
	 *            A method to create a new value to insert in the map.
	 */
	public DefaultMap(Supplier<V> defaultFactory) {
		direct = false;
		defValue = null;
		defFactory = defaultFactory;
	}

	V defaultFactory() {
		if (direct) {
			return defValue;
		}
		return defFactory.get();
	}

	/**
	 * Gets a value from the dictionary, inserting a newly manufactured value if
	 * the key has no mapping.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key) {
		V value = super.get(key);
		if (value == null) {
			value = defaultFactory();
			put((K) key, value);
		}
		return value;
	}
}
