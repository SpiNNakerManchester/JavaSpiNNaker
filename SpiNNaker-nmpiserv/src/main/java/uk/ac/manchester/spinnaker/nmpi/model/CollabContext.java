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
package uk.ac.manchester.spinnaker.nmpi.model;

/**
 * POJO holding the description of a HBP Collaboratory context.
 */
public class CollabContext {
	/** The ID of the APP in the collab. */
	private String appId;

	/** The collab contained within. */
	private Collab collab;

	/** The context string. */
	private String context;

	/** The ID of the item. */
	private int id;

	/** The name of the item. */
	private String name;

	/** The order of the item within its group. */
	private int orderIndex;

	/** The parent of the item. */
	private int parent;

	/** The type of the item. */
	private String type;

	/**
	 * Get the ID of the application.
	 *
	 * @return The ID
	 */
	public String getAppId() {
		return appId;
	}

	/**
	 * Set the ID of the application.
	 *
	 * @param appIdParam
	 *            The ID to set
	 */
	void setAppId(String appIdParam) {
		this.appId = appIdParam;
	}

	/**
	 * Get the collab of the application.
	 *
	 * @return The collab.
	 */
	public Collab getCollab() {
		return collab;
	}

	/**
	 * Set the collab of the application.
	 *
	 * @param collabParam
	 *            The collab to set
	 */
	void setCollab(Collab collabParam) {
		this.collab = collabParam;
	}

	/**
	 * Get the context of the application.
	 *
	 * @return The context
	 */
	public String getContext() {
		return context;
	}

	/**
	 * Set the context of the application.
	 *
	 * @param contextParam
	 *            The context to set
	 */
	void setContext(String contextParam) {
		this.context = contextParam;
	}

	/**
	 * Get the ID of the context.
	 *
	 * @return The ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Set the ID of the context.
	 *
	 * @param idParam
	 *            The ID
	 */
	void setId(int idParam) {
		this.id = idParam;
	}

	/**
	 * Get the name of the context.
	 *
	 * @return The name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the context.
	 *
	 * @param nameParam
	 *            The name to set
	 */
	void setName(String nameParam) {
		this.name = nameParam;
	}

	/**
	 * Get the index of this item to order.
	 *
	 * @return The index
	 */
	public int getOrderIndex() {
		return orderIndex;
	}

	/**
	 * Set the index of this item in order.
	 *
	 * @param orderIndexParam
	 *            The index to set
	 */
	void setOrderIndex(int orderIndexParam) {
		this.orderIndex = orderIndexParam;
	}

	/**
	 * Get the parent of this item.
	 *
	 * @return The parent
	 */
	public int getParent() {
		return parent;
	}

	/**
	 * Set the parent of this item.
	 *
	 * @param parentParam
	 *            The parent to set
	 */
	void setParent(int parentParam) {
		this.parent = parentParam;
	}

	/**
	 * Get the type of this item.
	 *
	 * @return The type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Set the type of this item.
	 *
	 * @param typeParam
	 *            The type to set
	 */
	void setType(String typeParam) {
		this.type = typeParam;
	}
}
