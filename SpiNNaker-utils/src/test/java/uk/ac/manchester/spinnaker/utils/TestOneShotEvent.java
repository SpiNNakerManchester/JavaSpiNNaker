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
				OneShotEvent event = new OneShotEvent();
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
				OneShotEvent event = new OneShotEvent();
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
			OneShotEvent event = new OneShotEvent();
			event.fire();
		}
	};

	private Runnable multiple = new Runnable() {
		@Override
		public void run() {
			try {
				OneShotEvent event = new OneShotEvent();
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
	public void testMultiple() throws InterruptedException {
		Thread thanger = new Thread(hanger);
		thanger.start();
		Thread tinOrder = new Thread(inOrder);
		tinOrder.start();
		Thread tfirer = new Thread(firer);
		tfirer.start();
		Thread tmultiple = new Thread(multiple);
		tmultiple.start();
		Thread twaiter = new Thread(waiter);
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
