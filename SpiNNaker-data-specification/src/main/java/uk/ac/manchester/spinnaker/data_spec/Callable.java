/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.data_spec;

/**
 * How to actually call a DSE operation.
 * <p>
 * Classes outside of this package do not need to directly make instances of
 * this class.
 *
 * @author Donal Fellows
 */
@FunctionalInterface
public interface Callable {
	/**
	 * The outer interface of a DSE operation. Note that this is subject to
	 * coercion to make the actual operations have a wider range of supported
	 * types.
	 *
	 * @param cmd
	 *            The encoded command word.
	 * @return Usually {@code 0}. Sometimes a marker to indicate special states
	 *         (currently just used for {@linkplain Commands#END_SPEC
	 *         end-of-specification}).
	 * @throws DataSpecificationException
	 *             If anything goes wrong in the data specification.
	 */
	int execute(int cmd) throws DataSpecificationException;
}
