/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.compat;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The encoded form of a command to the server. This is basically a Python
 * call encoded (except that no argument is a live object).
 */
@JsonAutoDetect(setterVisibility = NON_PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class Command {
	private static final int MAX_SIZE = 10;

	@NotBlank
	private String command;

	@Size(max = MAX_SIZE, message = "crazy number of arguments")
	private List<@SaneParameter Object> args = List.of();

	@Size(max = MAX_SIZE, message = "crazy number of keyword arguments")
	private Map<@NotBlank String, @SaneParameter Object> kwargs = Map.of();

	/** @return The name of the command. */
	public String getCommand() {
		return command;
	}

	void setCommand(String command) {
		this.command = command;
	}

	/** @return The positional arguments to the command. */
	public List<Object> getArgs() {
		return args;
	}

	void setArgs(List<Object> args) {
		this.args = new ArrayList<Object>(args);
	}

	/** @return The keyword arguments to the command. */
	public Map<String, Object> getKwargs() {
		return kwargs;
	}

	void setKwargs(Map<String, Object> kwargs) {
		this.kwargs = new HashMap<String, Object>(kwargs);
	}
}
