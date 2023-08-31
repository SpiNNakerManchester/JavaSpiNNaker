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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO describing the permissions to a collaboratory object.
 */
public class CollabPermissions {
	/** True if delete permission. */
	private boolean delete;

	/** True if update permission. */
	private boolean update;

	/**
	 * Determine if delete permission is given.
	 *
	 * @return Whether permitted to delete
	 */
	@JsonProperty("DELETE")
	public boolean isDelete() {
		return delete;
	}

	/**
	 * Set delete permission.
	 *
	 * @param delete
	 *            Whether to give permission
	 */
	public void setDelete(final boolean delete) {
		this.delete = delete;
	}

	/**
	 * Determine if update permission is given.
	 *
	 * @return Whether permitted to update
	 */
	@JsonProperty("UPDATE")
	public boolean isUpdate() {
		return update;
	}

	/**
	 * Set update permission.
	 *
	 * @param update
	 *            Whether permission is given
	 */
	public void setUpdate(final boolean update) {
		this.update = update;
	}
}
