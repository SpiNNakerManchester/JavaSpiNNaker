/*
 * Copyright (c) 2018-2023 The University of Manchester
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

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.errorprone.annotations.concurrent.GuardedBy;

/**
 * An asynchronous event that can be fired exactly once.
 *
 * @author Donal Fellows
 */
public final class OneShotEvent {
	@GuardedBy("lock")
	private boolean flag;

	private final Lock lock = new ReentrantLock();

	@GuardedBy("lock")
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
