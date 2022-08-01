/*
 * Copyright (c) 2018-2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.utils;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
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
public class DefaultMap<K, V> extends HashMap<K, V> {
	private static final long serialVersionUID = -3805864660424802906L;

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
	 *            keys. <em>Must not be {@code null}.</em>
	 */
	private <DV extends V> DefaultMap(DV defaultValue) {
		direct = true;
		defValue = requireNonNull(defaultValue);
		defFactory = null;
		advFactory = null;
	}

	/**
	 * Create a new map.
	 * <p>
	 * The {@code defaultFactory} is a method to generate an object. If the
	 * default value is mutable it is <em>highly</em> recommended to pass in a
	 * method like this, and not an object. For example use
	 * {@code ArrayList::new} and not {@code new ArrayList()} otherwise the
	 * single value will be used every time and values added after one get will
	 * be in the default for the next get.
	 *
	 * @param defaultFactory
	 *            A method to create a new value/object to insert in the map.
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
	 *            Can be an Object of a Class that implement KeyAwareFactory.
	 *            Can be a method expressed as a lambda.
	 */
	private DefaultMap(Marker dummy,
			KeyAwareFactory<? super K, ? extends V> defaultFactory) {
		direct = false;
		defValue = null;
		defFactory = null;
		advFactory = requireNonNull(defaultFactory);
	}

	/**
	 * A marker used only to differentiate a constructor.
	 */
	private static final class Marker {
		// Nothing interesting
		static final Marker INSTANCE = new Marker();
	}

	/**
	 * Create a new map.
	 *
	 * @param <K>
	 *            The type of keys.
	 * @param <DV>
	 *            The type of the default value.
	 * @param defaultValue
	 *            The default value to use in the map. This should be an
	 *            immutable value as it can be potentially inserted for many
	 *            keys. <em>Must not be {@code null}.</em>
	 * @return The new default map.
	 */
	public static <K, DV> DefaultMap<K, DV> newMapWithDefault(DV defaultValue) {
		return new DefaultMap<>(defaultValue);
	}

	/**
	 * Create a new map that manufactures new elements that are aware of their
	 * key from the beginning. This is done through this method because
	 * otherwise it clashes with the more common case of the unaware factory.
	 * <p>
	 * The Factory can be a lambda method to create a me value based on the key.
	 * <br>
	 * For example:
	 * <p>
	 * {@code DefaultMap.newAdvancedDefaultMap(i -> i*2);}
	 * <p>
	 * The Factory can also be a Object of a class that implements the
	 * KeyAwareFactory interface.
	 *
	 * @param <K>
	 *            The type of keys.
	 * @param <V>
	 *            The type of values.
	 * @param keyAwareFactory
	 *            Method or Object to create the default values.
	 * @return The new default map.
	 */
	public static <K, V> DefaultMap<K, V> newAdvancedDefaultMap(
			KeyAwareFactory<? super K, ? extends V> keyAwareFactory) {
		return new DefaultMap<>(Marker.INSTANCE, keyAwareFactory);
	}

	private V makeDefault(K key) {
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
	@Override
	public V get(Object key) {
		@SuppressWarnings("unchecked")
		var k = (K) key;
		var value = super.get(key);
		if (value == null) {
			value = makeDefault(k);
			put(k, value);
		}
		return value;
	}

	// Versions of ops that aren't done quite right by the superclass

	/**
	 * {@inheritDoc}
	 * <p>
	 * <strong>NB:</strong> This converts {@code null}s into the correct default
	 * value.
	 */
	@Override
	public V compute(K key,
			BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		return super.compute(key, (k, v) -> {
			// Not very efficient, but can't see internals needed to do better
			if (v == null) {
				v = makeDefault(k);
			}
			var result = remappingFunction.apply(k, v);
			if (result == null) {
				result = makeDefault(k);
			}
			return result;
		});
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <strong>NB:</strong> This converts {@code null}s into the correct default
	 * value.
	 */
	@Override
	public V computeIfAbsent(K key,
			Function<? super K, ? extends V> mappingFunction) {
		return super.computeIfAbsent(key, k -> {
			var result = mappingFunction.apply(k);
			if (result == null) {
				result = makeDefault(k);
			}
			return result;
		});
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <strong>NB:</strong> This converts {@code null}s into the correct default
	 * value.
	 */
	@Override
	public V computeIfPresent(K key,
			BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		return super.computeIfPresent(key, (k, v) -> {
			var result = remappingFunction.apply(k, v);
			if (result == null) {
				result = makeDefault(k);
			}
			return result;
		});
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <strong>NB:</strong> This converts {@code null}s into the correct default
	 * value.
	 */
	@Override
	public V merge(K key, V value,
			BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		if (value == null) {
			value = makeDefault(key);
		}
		return super.merge(key, value, (v1, v2) -> {
			var result = remappingFunction.apply(v1, v2);
			if (result == null) {
				result = makeDefault(key);
			}
			return result;
		});
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
