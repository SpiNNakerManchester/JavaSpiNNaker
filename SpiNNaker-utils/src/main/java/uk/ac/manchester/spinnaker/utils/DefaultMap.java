package uk.ac.manchester.spinnaker.utils;

import static java.util.Objects.requireNonNull;

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
	private final Supplier<? extends V> defFactory;
	private final KeyAwareFactory<? super K, ? extends V> advFactory;

	/**
	 * Create a new map.
	 *
	 * @param <DV>
	 *            The type of the default value.
	 * @param defaultValue
	 *            The default value to use in the map. This should be an
	 *            immutable value as it can be potentially inserted for many
	 *            keys.
	 */
	public <DV extends V> DefaultMap(DV defaultValue) {
		direct = true;
		defValue = requireNonNull(defaultValue);
		defFactory = null;
		advFactory = null;
	}

	/**
	 * Create a new map.
	 *
	 * @param defaultFactory
	 *            A method to create a new value to insert in the map.
	 */
	public DefaultMap(Supplier<? extends V> defaultFactory) {
		direct = false;
		defValue = null;
		defFactory = requireNonNull(defaultFactory);
		advFactory = null;
	}

	/**
	 * Create a new map.
	 *
	 * @param dummy
	 *            Just something to make this constructor distinct.
	 * @param defaultFactory
	 *            A method to create a new value to insert in the map.
	 */
	private DefaultMap(Class<?> dummy,
			KeyAwareFactory<? super K, ? extends V> defaultFactory) {
		dummy.getSuperclass();
		direct = false;
		defValue = null;
		defFactory = null;
		advFactory = requireNonNull(defaultFactory);
	}

	/**
	 * Create a new map that manufactures new elements that are aware of their
	 * key from the beginning. This is done through this method because
	 * otherwise it clashes with the more common case of the unaware factory.
	 *
	 * @param <K>
	 *            The type of keys.
	 * @param <V>
	 *            The type of values.
	 * @param defaultFactory
	 *            A method to create a new value to insert in the map.
	 * @return The new default map.
	 */
	public static <K, V> DefaultMap<K, V> newAdvancedDefaultMap(
			KeyAwareFactory<? super K, ? extends V> defaultFactory) {
		return new DefaultMap<>(defaultFactory.getClass(), defaultFactory);
	}

	private V defaultFactory(K key) {
		if (direct) {
			return defValue;
		}
		if (defFactory != null) {
			return defFactory.get();
		}
		return advFactory.createValue(key);
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
			value = defaultFactory((K) key);
			put((K) key, value);
		}
		return value;
	}

	/**
	 * An advanced factory that has access to the key when it is creating the
	 * value to associate with it.
	 *
	 * @author Donal Fellows
	 * @param <K>
	 *            The type of keys.
	 * @param <V>
	 *            The type of values.
	 */
	@FunctionalInterface
	public interface KeyAwareFactory<K, V> {
		/**
		 * Create a new value for the {@linkplain DefaultMap default map}.
		 *
		 * @param key
		 *            The key that will be used to store the value in the map.
		 * @return the value to store
		 */
		V createValue(K key);
	}
}
