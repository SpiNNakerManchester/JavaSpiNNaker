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

        ArrayList<Integer> processors = new ArrayList<>();
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
