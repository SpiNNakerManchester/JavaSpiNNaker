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
/**
 * The NMPI service classes.
 * <p>
 * The actual key service APIs are:
 * <ul>
 * <li>{@link NMPIQueueManager} &mdash; Handles the queue of {@linkplain Job
 * NMPI jobs}.
 * <li>{@link JobExecuter} &mdash; Handles the running of a single NMPI job.
 * Note that job executer implementations ask the queue manager for the job that
 * they should run.
 * <li>{@link JobExecuterFactory} &mdash; Creates job executers.
 * <li>{@link MachineManager} &mdash; Handles the resources on which NMPI jobs
 * run.
 * </ul>
 */
@UsedInJavadocOnly({ NMPIQueueManager.class, Job.class, JobExecuter.class,
		JobExecuterFactory.class, MachineManager.class })
package uk.ac.manchester.spinnaker.nmpi;

import uk.ac.manchester.spinnaker.nmpi.jobmanager.JobExecuter;
import uk.ac.manchester.spinnaker.nmpi.jobmanager.JobExecuterFactory;
import uk.ac.manchester.spinnaker.nmpi.machinemanager.MachineManager;
import uk.ac.manchester.spinnaker.nmpi.model.job.nmpi.Job;
import uk.ac.manchester.spinnaker.nmpi.nmpi.NMPIQueueManager;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;
