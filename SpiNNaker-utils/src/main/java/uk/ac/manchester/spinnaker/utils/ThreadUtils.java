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
package uk.ac.manchester.spinnaker.utils;

import static java.lang.management.ManagementFactory.getThreadMXBean;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import java.lang.management.ThreadInfo;

/**
 * Utilities for working with threads.
 */
public abstract class ThreadUtils {
	/**
	 * Produce a dump of what all threads are doing. Useful for debugging as it
	 * means you can get a dump of threads programmatically at the time when the
	 * problem is likely to be on some thread's stack.
	 *
	 * @return The dump, as a multi-line string.
	 */
	public static String threadDump() {
		return stream(getThreadMXBean().dumpAllThreads(true, true))
				.map(ThreadInfo::toString).collect(joining());
	}
}
