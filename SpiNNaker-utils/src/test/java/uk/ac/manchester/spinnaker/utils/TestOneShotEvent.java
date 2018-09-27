package uk.ac.manchester.spinnaker.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Christian
 */
public class TestOneShotEvent {
    

    public TestOneShotEvent() {
    }

    Runnable hanger = new Runnable() {
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
        
    Runnable inOrder = new Runnable() {
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
        
    Runnable firer = new Runnable() {
        @Override
        public void run() {
            OneShotEvent event = new OneShotEvent();
            event.fire();
        }
    };

    Runnable multiple = new Runnable() {
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

    OneShotEvent event1 = new OneShotEvent();

    Runnable waiter = new Runnable() {
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
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            System.out.println("pop");
        }
        assert (thanger.isAlive());
        thanger.interrupt();
        assert (!tinOrder.isAlive());
        assert (!tfirer.isAlive());
        assert (!tmultiple.isAlive());
        assert (twaiter.isAlive());
        event1.fire();
        try {
            Thread.sleep(50);
        } catch (InterruptedException ex) {
            System.out.println("pop2");
        }
        assert (!twaiter.isAlive());
    }

}
