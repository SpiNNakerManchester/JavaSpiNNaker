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
