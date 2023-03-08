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
package uk.ac.manchester.spinnaker.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Christian
 */
public class TestOneShotEvent {
	private static void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			System.err.println("unexpected wake from sleep");
		}
	}

	public TestOneShotEvent() {
	}

	private Runnable hanger = new Runnable() {
		@Override
		public void run() {
			try {
				var event = new OneShotEvent();
				event.await();
				event.fire();
			} catch (InterruptedException ex) {
				// Do nothing should be required;
			}
		}
	};

	private Runnable inOrder = new Runnable() {
		@Override
		public void run() {
			try {
				var event = new OneShotEvent();
				event.fire();
				event.await();
			} catch (InterruptedException ex) {
				// Do nothing should be required;
			}
		}
	};

	private Runnable firer = new Runnable() {
		@Override
		public void run() {
			var event = new OneShotEvent();
			event.fire();
		}
	};

	private Runnable multiple = new Runnable() {
		@Override
		public void run() {
			try {
				var event = new OneShotEvent();
				event.fire();
				event.fire();
				event.await();
				event.await();
				event.fire();
				event.await();
			} catch (InterruptedException ex) {
				// Do nothing should be required;
			}
		}
	};

	private OneShotEvent event1 = new OneShotEvent();

	private Runnable waiter = new Runnable() {
		@Override
		public void run() {
			try {
				event1.await();
			} catch (InterruptedException ex) {
				// Do nothing should be required;
			}
		}
	};

	@Test
	public void testMultiple() {
		var thanger = new Thread(hanger);
		thanger.start();
		var tinOrder = new Thread(inOrder);
		tinOrder.start();
		var tfirer = new Thread(firer);
		tfirer.start();
		var tmultiple = new Thread(multiple);
		tmultiple.start();
		var twaiter = new Thread(waiter);
		twaiter.start();

		sleep(500);

		assertTrue(thanger.isAlive());
		thanger.interrupt();
		assertTrue(!tinOrder.isAlive());
		assertTrue(!tfirer.isAlive());
		assertTrue(!tmultiple.isAlive());
		assertTrue(twaiter.isAlive());
		event1.fire();

		sleep(50);

		assertTrue(!twaiter.isAlive());
	}

}
