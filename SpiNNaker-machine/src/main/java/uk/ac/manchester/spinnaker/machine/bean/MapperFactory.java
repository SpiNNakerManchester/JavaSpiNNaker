/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.machine.bean;

import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import com.fasterxml.jackson.databind.ObjectMapper;
import static com.fasterxml.jackson.databind.PropertyNamingStrategies.LOWER_CAMEL_CASE;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Creates a mapper that can handle the sort of JSON mappings used in SpiNNaker
 * code.
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
	 * <p>
	 * This method makes sure that all JSON unmarshallers use the same mapper
	 * set up the exact same way.
	 *
	 * @return The Object Mapper used by the Spalloc client,
	 */
	public static ObjectMapper createMapper() {
		return new ObjectMapper().registerModule(new SimpleModule())
				.setPropertyNamingStrategy(LOWER_CAMEL_CASE)
				.configure(FAIL_ON_UNKNOWN_PROPERTIES, true)
				.configure(ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
	}
}
