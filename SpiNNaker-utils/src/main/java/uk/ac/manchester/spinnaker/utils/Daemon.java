/*
 * Copyright (c) 2018-2022 The University of Manchester
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
