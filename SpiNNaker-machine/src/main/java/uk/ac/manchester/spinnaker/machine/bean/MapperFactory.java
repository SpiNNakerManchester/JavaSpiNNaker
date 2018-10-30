/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine.bean;

import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import com.fasterxml.jackson.databind.ObjectMapper;
import static com.fasterxml.jackson.databind.PropertyNamingStrategy.LOWER_CAMEL_CASE;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 *
 * @author Christian-B
 */
public final class MapperFactory {
    /**
     * No Reason to instantiate.
     */
    private MapperFactory() {
    }

    /**
     * Static method to create the object mapper.
     *<p>
     * This method makes sure that all JSON unmarshallers use the same mapper
     * set up the exact same way.
     *
     * @return The Object Mapper used by the Spalloc client,
     */
    public static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        mapper.registerModule(module);
        mapper.setPropertyNamingStrategy(LOWER_CAMEL_CASE);
        mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, true);
        mapper.configure(ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        return mapper;
    }
}
