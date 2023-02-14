/*
 * Copyright (c) 2018-2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.utils;

import static java.util.Objects.requireNonNull;

/**
 * A thread that is a daemon by default.
 *
 * @author Donal Fellows
 */
public class Daemon extends Thread {
	/**
	 * Allocate a new daemon thread. This thread is not running.
	 *
	 * @param target
	 *            the object whose {@link Runnable#run() run} method is invoked
	 *            when this thread is started. Never {@code null}.
	 */
	public Daemon(Runnable target) {
		super(requireNonNull(target));
		setDaemon(true);
	}

	/**
	 * Allocate a new daemon thread. This thread is not running.
	 *
	 * @param target
	 *            the object whose {@link Runnable#run() run} method is invoked
	 *            when this thread is started. Never {@code null}.
	 * @param name
	 *            the name of the new thread
	 */
	public Daemon(Runnable target, String name) {
		super(requireNonNull(target), name);
		setDaemon(true);
	}

	/**
	 * Allocate a new daemon thread. This thread is not running.
	 *
	 * @param group
	 *            the object whose {@link Runnable#run() run} method is invoked
	 *            when this thread is started.
	 * @param target
	 *            the object whose {@code run} method is invoked when this
	 *            thread is started. Never {@code null}.
	 */
	public Daemon(ThreadGroup group, Runnable target) {
		super(group, requireNonNull(target));
		setDaemon(true);
	}

	/**
	 * Allocate a new daemon thread. This thread is not running.
	 *
	 * @param group
	 *            the thread group. If {@code null}, the group is set to the
	 *            current thread's thread group.
	 * @param target
	 *            the object whose {@link Runnable#run() run} method is invoked
	 *            when this thread is started. Never {@code null}.
	 * @param name
	 *            the name of the new thread
	 */
	public Daemon(ThreadGroup group, Runnable target, String name) {
		super(group, requireNonNull(target), name);
		setDaemon(true);
	}
}
