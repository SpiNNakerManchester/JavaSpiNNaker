/*
 * Copyright (c) 2018 The University of Manchester
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
