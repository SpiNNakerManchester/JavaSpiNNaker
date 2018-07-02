/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.*;


/**
 *
 * @author Christian-B
 */
public class TestProcessor {

    private void checkDifferent(Processor p1, Processor p2) {
        assertNotEquals(p1, p2);
        assertNotEquals(p1.hashCode(), p2.hashCode());
        assertNotEquals(p1.toString(), p2.toString());
    }

    private void checkLess(Processor smaller, Processor larger) {
        checkDifferent(smaller, larger);
        assertThat(smaller, lessThan(larger));
        assertThat(larger, greaterThan(smaller));
    }

    private void checkSame(Processor p1, Processor p2) {
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertEquals(p1.toString(), p2.toString());
    }

    private void checkDifferent2(Processor p1, Processor p2) {
        assertNotEquals(p1, p2);
        assertNotEquals(p1.hashCode(), p2.hashCode());
    }

    private void checkLess2(Processor smaller, Processor larger) {
        checkDifferent2(smaller, larger);
        assertThat(smaller, lessThan(larger));
        assertThat(larger, greaterThan(smaller));
    }

    @Test
    public void testEquals() {
        Processor p1 = Processor.factory(1);
        Processor p2 = Processor.factory(2);
        checkDifferent(p1, p2);
        Processor p1f = Processor.factory(1, false);
        checkSame(p1, p1f);
        Processor p1m = Processor.factory(1, true);
        checkDifferent(p1, p1m);
        assertNotEquals(p1 ,null);
        assertNotEquals(p1, "p1");
        Processor faster = Processor.factory(
                1, MachineDefaults.PROCESSOR_CLOCK_SPEED * + 10000000,
                MachineDefaults.DTCM_AVAILABLE, false);
        Processor faster2 = Processor.factory(
                1, MachineDefaults.PROCESSOR_CLOCK_SPEED * + 10000000,
                MachineDefaults.DTCM_AVAILABLE, false);
        checkSame(faster, faster2);
    }

    @Test
    public void testComparesTo() {
        Processor p1 = Processor.factory(1);
        Processor standard = Processor.factory(
                1, MachineDefaults.PROCESSOR_CLOCK_SPEED,
                MachineDefaults.DTCM_AVAILABLE, false);
        Processor two = Processor.factory(
                2, MachineDefaults.PROCESSOR_CLOCK_SPEED,
                MachineDefaults.DTCM_AVAILABLE, false);
        Processor monitor = Processor.factory(
                1, MachineDefaults.PROCESSOR_CLOCK_SPEED,
                MachineDefaults.DTCM_AVAILABLE, true);
        Processor faster = Processor.factory(
                1, MachineDefaults.PROCESSOR_CLOCK_SPEED * + 10000000,
                MachineDefaults.DTCM_AVAILABLE, false);
        Processor more = Processor.factory(
                1, MachineDefaults.PROCESSOR_CLOCK_SPEED,
                MachineDefaults.DTCM_AVAILABLE + 10, false);
        assertThat(p1, lessThanOrEqualTo(standard));
        checkLess(standard, two);
        checkLess(standard, monitor);
        checkLess(standard, faster);
        checkLess2(standard, more);
     }

    @Test
    public void testClone() {
        Processor p1 = Processor.factory(1);
        Processor p1m = Processor.factory(1, true);
        Processor clone = p1.cloneAsSystemProcessor();
        checkSame(p1m, clone);
        Processor faster = Processor.factory(
                1, MachineDefaults.PROCESSOR_CLOCK_SPEED + 10,
                MachineDefaults.DTCM_AVAILABLE, false);
        Processor fasterM = Processor.factory(
                1, MachineDefaults.PROCESSOR_CLOCK_SPEED + 10,
                MachineDefaults.DTCM_AVAILABLE, true);
        clone = faster.cloneAsSystemProcessor();
        checkSame(fasterM, clone);
    }

    @Test
    public void testCpuCyclesAvailable() {
        Processor p1 = Processor.factory(1);
        assertEquals(200000, p1.cpuCyclesAvailable());
    }

    @Test
    public void testBad() {
        assertThrows(IllegalArgumentException.class, () -> {
            Processor bad = Processor.factory(
                1, -10, MachineDefaults.DTCM_AVAILABLE, false);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            Processor bad = Processor.factory(
                1, MachineDefaults.PROCESSOR_CLOCK_SPEED, -10, false);
        });
    }

}
