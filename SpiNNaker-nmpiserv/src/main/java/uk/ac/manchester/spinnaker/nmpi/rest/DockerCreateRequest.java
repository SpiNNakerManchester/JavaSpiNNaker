/*
 * Copyright (c) 2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.nmpi.rest;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A request to docker create.
 */
public class DockerCreateRequest {

	private String image;

	private List<String> cmd;

	public DockerCreateRequest(String image, List<String> cmd) {
		this.image = image;
		this.cmd = cmd;
	}

	/**
	 * @return the image
	 */
	@JsonProperty("Image")
	public String getImage() {
		return image;
	}

	/**
	 * @param image the image to set
	 */
	public void setImage(String image) {
		this.image = image;
	}

	/**
	 * @return the cmd
	 */
	@JsonProperty("Cmd")
	public List<String> getCmd() {
		return cmd;
	}

	/**
	 * @param cmd the cmd to set
	 */
	public void setCmd(List<String> cmd) {
		this.cmd = cmd;
	}
}
