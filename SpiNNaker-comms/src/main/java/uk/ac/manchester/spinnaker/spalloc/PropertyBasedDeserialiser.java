package uk.ac.manchester.spinnaker.spalloc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A deserialiser which deserialises classes based on unique properties that
 * they have. The classes to be deserialised need to be registered with a unique
 * property using the "register" function.
 *
 * @param <T>
 *            The type of values being deserialised.
 */
public class PropertyBasedDeserialiser<T> extends StdDeserializer<T> {
	private static final long serialVersionUID = 1L;

	private final Map<String, Class<? extends T>> registry = new HashMap<>();

	/**
	 * Creates a new deserialiser.
	 *
	 * @param type
	 *            The (super)class of the values that will be produced.
	 */
	public PropertyBasedDeserialiser(Class<T> type) {
		super(type);
	}

	/**
	 * Registers a type against a property in the deserialiser.
	 *
	 * @param propertyName
	 *            The name of the unique property that identifies the class.
	 *            This is the JSON name.
	 * @param type
	 *            The class to register against the property.
	 */
	protected void register(String propertyName, Class<? extends T> type) {
		if (propertyName == null) {
			throw new IllegalArgumentException("propertyName must be non-null");
		}
		if (type == null) {
			throw new IllegalArgumentException("type must be non-null");
		}

		registry.put(propertyName, type);
	}

	/**
	 * Look up what class we might deserialize as.
	 *
	 * @param elementNames
	 *            The element names available to us.
	 * @return The class, or <tt>null</tt> if nothing matches (i.e., if we've
	 *         gone through the iterator and had no matches).
	 */
	private Class<? extends T> getTargetClass(Iterator<String> elementNames) {
		while (elementNames.hasNext()) {
			Class<? extends T> cls = registry.get(elementNames.next());
			if (cls != null) {
				return cls;
			}
		}
		return null;
	}

	/**
	 * Deserialize a JSON object as a Java object, using the names of the
	 * properties of the JSON object to work out what sort of Java object to
	 * return.
	 *
	 * @param parser
	 *            The parser to get the JSON object from.
	 * @param context
	 *            ignored
	 * @return A Java object (probably a POJO), or <tt>null</tt> if the
	 *         properties of the JSON object don't identify which Java class we
	 *         should use for deserialization.
	 * @throws IOException
	 *             If the deserialization fails.
	 */
	@Override
	public T deserialize(JsonParser parser, DeserializationContext context)
			throws IOException {
		ObjectNode root = parser.readValueAsTree();
		Class<? extends T> cls = getTargetClass(root.fieldNames());
		if (cls == null) {
			return null;
		}
		return parser.getCodec().treeToValue(root, cls);
	}
}
