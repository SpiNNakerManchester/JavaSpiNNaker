package uk.ac.manchester.spinnaker.spalloc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A deserialiser which deserialises classes based on unique properties that
 * they have. The classes to be deserialised need to be registered with a unique
 * property using the "register" function.
 *
 * @param <T>
 *      The type of values being deserialised.
 */
public class PropertyBasedDeserialiser<T> extends StdDeserializer<T> {
    private static final long serialVersionUID = 1L;

    private final Map<String, Class<? extends T>> registry = new HashMap<>();

    /**
     * Creates a new deserialiser.
     *
     * @param type
     *          The (super)class of the values that will be produced.
     */
    public PropertyBasedDeserialiser(final Class<T> type) {
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
    public void register(final String propertyName,
            final Class<? extends T> type) {
        if (propertyName == null) {
            throw new IllegalArgumentException("propertyName must be non-null");
        }
        if (type == null) {
            throw new IllegalArgumentException("type must be non-null");
        }

        registry.put(propertyName, type);
    }

    @Override
    public T deserialize(final JsonParser parser,
            final DeserializationContext context)
            throws IOException, JsonProcessingException {
        final ObjectNode root = parser.readValueAsTree();
        final Iterator<String> elementsIterator = root.fieldNames();
        while (elementsIterator.hasNext()) {
            final String name = elementsIterator.next();
            if (registry.containsKey(name)) {
                return parser.getCodec().treeToValue(root, registry.get(name));
            }
        }
        return null;
    }
}
