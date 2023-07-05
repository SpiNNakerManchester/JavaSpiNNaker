/*
 * Copyright (c) 2014 The University of Manchester
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
package uk.ac.manchester.spinnaker.nmpiexec.jobprocess;

import static jakarta.xml.bind.annotation.XmlAccessType.FIELD;

import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;

/**
 * The POJO representation of a provenance data item.
 */
@XmlAccessorType(FIELD)
public class ProvenanceDataItem {

	/**
	 * The name of the item.
	 */
	@XmlAttribute
	private String name;

	/**
	 * The value if the item.
	 */
	@XmlValue
	private String value;

	/**
	 * Get the name of the item.
	 *
	 * @return The name of the item
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the value of the item.
	 *
	 * @return The value of the item
	 */
	public String getValue() {
		return value;
	}
}
