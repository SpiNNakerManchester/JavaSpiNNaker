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
package uk.ac.manchester.spinnaker.nmpi.jobmanager;

import java.io.IOException;
import java.net.URL;

/**
 * A factory for creating job executers.
 *
 * @see DockerExecutorFactory
 * @see LocalJobExecuterFactory
 * @see XenVMExecuterFactory
 */
public sealed interface JobExecuterFactory permits DockerExecutorFactory,
		LocalJobExecuterFactory, XenVMExecuterFactory {
	/**
	 * Creates a new {@link JobExecuter}.
	 *
	 * @param manager
	 *            The manager requesting the creation
	 * @param baseUrl
	 *            The URL of the manager
	 * @return The new executer
	 * @throws IOException
	 *             If there is an error creating the executer
	 */
	JobExecuter createJobExecuter(JobManager manager, URL baseUrl)
			throws IOException;
}
