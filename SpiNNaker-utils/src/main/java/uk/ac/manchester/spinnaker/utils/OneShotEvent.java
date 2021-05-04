/*
 * Copyright (c) 2018 The University of Manchester
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

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An asynchronous event that can be fired exactly once.
 *
 * @author Donal Fellows
 */
public final class OneShotEvent {
	private boolean flag;

	private final Lock lock = new ReentrantLock();

	private final Condition cond = lock.newCondition();

	/**
	 * Wait for the event to fire.
	 *
	 * @throws InterruptedException
	 *             If wait is interrupted.
	 */
	public void await() throws InterruptedException {
		lock.lock();
		try {
			while (!flag) {
				cond.await();
			}
		} finally {
			lock.unlock();
		}
	}

	/** Fire the event. */
	public void fire() {
		lock.lock();
		try {
			if (!flag) {
				flag = true;
				cond.signalAll();
			}
		} finally {
			lock.unlock();
		}
	}
}
