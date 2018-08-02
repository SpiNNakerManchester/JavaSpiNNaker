/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Christian-B
 */
public class TestCoreSubsetsFailedChipTuple {

    ChipLocation location00 = new ChipLocation(0,0);
    ChipLocation location01 = new ChipLocation(0,1);
    ChipLocation location10 = new ChipLocation(1,0);
    ChipLocation location11 = new ChipLocation(1,1);

    Link link00_01 = new Link(location00, Direction.NORTH, location01);
    //Link link00_01a = new Link(location00, Direction.NORTH, location01);
    Link link00_10 = new Link(location00, Direction.WEST, location10);
    //Link link01_01 = new Link(location01, Direction.SOUTH, location01);


    private Router createRouter() {
        ArrayList<Link> links = new ArrayList<>();
        links.add(link00_01);
        links.add(link00_10);
        return new Router(links);
    }

    private ArrayList<Processor> getProcessors() {
        ArrayList<Processor> processors = new ArrayList<>();
        processors.add(Processor.factory(1));
        processors.add(Processor.factory(2, true));
        processors.add(Processor.factory(4));
        return processors;
    }

    public TestCoreSubsetsFailedChipTuple() {
    }

    @Test
    public void testBasic() {
        CoreSubsetsFailedChipsTuple instance = new CoreSubsetsFailedChipsTuple();
        assertEquals(0, instance.size());

        instance.addCore(0, 0, 1);
        assertEquals(1, instance.size());

        ArrayList<Integer> processors = new ArrayList();
        processors.add(1);
        instance.addCores(0, 0, processors);
        assertEquals(1, instance.size());
        assertFalse(instance.isChip(ChipLocation.ONE_ZERO));

        Chip chip = new Chip(location00, getProcessors(), createRouter(), null,
                location11);
        instance.addFailedChip(chip);

        assertEquals(1, instance.failedChips.size());
    }


}
