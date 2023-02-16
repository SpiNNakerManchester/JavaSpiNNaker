/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * The core allocation system.
 * <p>
 * There are two key classes in this package, {@link Spalloc} and
 * {@link AllocatorTask}. {@code Spalloc} provides the main model
 * implementations of {@link SpallocAPI.Machine} and {@link SpallocAPI.Job};
 * when a job is submitted, it is responsible for assigning the job to a machine
 * and asking the allocator to choose which boards. The allocator
 * ({@code AllocatorTask}) runs periodically and will choose which boards are
 * assigned to a job, as well as checking for whether a job has exceeded its
 * keep-alive time limit.
 * <p>
 * Actual control over individual boards is done in the
 * {@link uk.ac.manchester.spinnaker.alloc.bmp.BMPController BMPController}.
 */
package uk.ac.manchester.spinnaker.alloc.allocator;
