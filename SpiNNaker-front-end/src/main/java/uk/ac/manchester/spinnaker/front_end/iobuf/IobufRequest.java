/*
 * Copyright (c) 2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end.iobuf;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING;
import static com.fasterxml.jackson.annotation.JsonFormat.Shape.OBJECT;
import static java.util.Collections.unmodifiableMap;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import uk.ac.manchester.spinnaker.machine.CoreSubsets;

/**
 * The request to read some IOBUFs off a SpiNNaker system.
 * Parses a JSON request in this format:
 * <pre>
 *   {
 *      "/some/path/abc.aplx": [ [0,0,1], [0,0,2], [0,0,3] ],
 *      "/some/path/def.aplx": [ [0,1,1], [0,1,2], [0,1,3] ],
 *      "/some/path/ghi.aplx": [ [1,0,1], [1,0,2], [0,0,4] ]
 *   }
 * </pre>
 *
 * @author Donal Fellows
 */
@JsonFormat(shape = OBJECT)
@JsonAutoDetect(fieldVisibility = NONE)
public class IobufRequest {
	private Map<@NotNull File, @Valid CoreSubsets> requestMap;

	/**
	 * @param map
	 *            A mapping from fully qualified filenames to an array of
	 *            sub-arrays of integers, where each sub-array is three elements
	 *            long.
	 * @throws IllegalArgumentException
	 *             If any core is used more than once, whether for the same
	 *             executable or a different one.
	 */
	@JsonCreator(mode = DELEGATING)
	public IobufRequest(Map<String, List<List<Integer>>> map) {
		var requestMap = new HashMap<File, CoreSubsets>();
		for (var name : map.keySet()) {
			var cores = requestMap.computeIfAbsent(
					new File(name).getAbsoluteFile(), __ -> new CoreSubsets());
			map.get(name).forEach(node -> parseCore(cores, node));
		}

		if (!isSaneCores()) {
			throw new IllegalArgumentException("overlapping uses of core");
		}

		this.requestMap = unmodifiableMap(requestMap);
	}

	@AssertTrue(message = "core must not have two binaries mapped to it")
	private boolean isSaneCores() {
		int expectedSize = 0;
		var validator = new CoreSubsets();
		for (var s : requestMap.values()) {
			validator.addCores(s);
			expectedSize += s.size();
		}
		return validator.size() == expectedSize;
	}

	private static void parseCore(CoreSubsets cores, List<Integer> a) {
		int x = a.get(0);
		int y = a.get(1);
		int p = a.get(2);
		cores.addCore(x, y, p);
	}

	/**
	 * @return The mapping from executables to the cores on which they are
	 *         running.
	 */
	@JsonIgnore
	public Map<File, CoreSubsets> getRequestDetails() {
		return requestMap;
	}
}
