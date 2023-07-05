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

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * The POJO representation of a collection of data items.
 */
@XmlAccessorType(FIELD)
@XmlRootElement(name = "provenance_data_items")
public class ProvenanceDataItems {

	/**
	 * The name of this level of the hierarchy.
	 */
	@XmlAttribute
	private String name;

	/**
	 * The sub hierarchies.
	 */
	@XmlElement(name = "provenance_data_items")
	private final List<ProvenanceDataItems> provenanceDataItems =
			new ArrayList<>();

	/**
	 * Items at this level of the hierarchy.
	 */
	@XmlElement(name = "provenance_data_item")
	private final List<ProvenanceDataItem> provenanceDataItem =
			new ArrayList<>();

	/**
	 * Get the name of this level of the hierarchy.
	 *
	 * @return The name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Levels below this level of the hierarchy.
	 *
	 * @return The child levels
	 */
	public List<ProvenanceDataItems> getProvenanceDataItems() {
		return provenanceDataItems;
	}

	/**
	 * Items at this level of the hierarchy.
	 *
	 * @return The items at this level
	 */
	public List<ProvenanceDataItem> getProvenanceDataItem() {
		return provenanceDataItem;
	}
}
