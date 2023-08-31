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
	 * @param appId
	 *            The ID to set
	 */
	public void setAppId(final String appId) {
		this.appId = appId;
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
	 * @param collab
	 *            The collab to set
	 */
	public void setCollab(final Collab collab) {
		this.collab = collab;
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
	 * @param context
	 *            The context to set
	 */
	public void setContext(final String context) {
		this.context = context;
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
	 * @param id
	 *            The ID
	 */
	public void setId(final int id) {
		this.id = id;
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
	 * @param name
	 *            The name to set
	 */
	public void setName(final String name) {
		this.name = name;
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
	 * @param orderIndex
	 *            The index to set
	 */
	public void setOrderIndex(final int orderIndex) {
		this.orderIndex = orderIndex;
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
	 * @param parent
	 *            The parent to set
	 */
	public void setParent(final int parent) {
		this.parent = parent;
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
	 * @param type
	 *            The type to set
	 */
	public void setType(final String type) {
		this.type = type;
	}
}
