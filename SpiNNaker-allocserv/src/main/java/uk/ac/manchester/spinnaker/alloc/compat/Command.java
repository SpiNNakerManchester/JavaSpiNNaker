/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.compat;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * The encoded form of a command to the server. This is basically a Python
 * call encoded (except that no argument is a live object).
 */
@JsonAutoDetect(setterVisibility = NON_PRIVATE)
public final class Command {
	private String command;

	private List<Object> args = new ArrayList<>();

	private Map<String, Object> kwargs = new HashMap<>();

	/** @return The name of the command. */
	public String getCommand() {
		return command;
	}

	void setCommand(String command) {
		this.command = command;
	}

	/** @return The positional arguments to the command. */
	public List<Object> getArgs() {
		return isNull(args) ? emptyList() : unmodifiableList(args);
	}

	void setArgs(List<Object> args) {
		this.args = args;
	}

	/** @return The keyword arguments to the command. */
	public Map<String, Object> getKwargs() {
		return isNull(kwargs) ? emptyMap() : unmodifiableMap(kwargs);
	}

	void setKwargs(Map<String, Object> kwargs) {
		this.kwargs = kwargs;
	}
}
