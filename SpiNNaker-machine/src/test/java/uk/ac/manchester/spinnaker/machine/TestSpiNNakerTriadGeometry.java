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

import java.util.Collection;
import java.util.function.Consumer;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import uk.ac.manchester.spinnaker.utils.Counter;

/**
 *
 * @author Christian-B
 */
public class TestSpiNNakerTriadGeometry {

    public TestSpiNNakerTriadGeometry() {
    }
    // This table was copied from python (negated)
    //     which claims it was produced using the code in Rig
    int[][][] localTable = {
        {{0, 0}, {1, 0}, {2, 0}, {3, 0}, {4, 0}, {1, 4},
            {2, 4}, {3, 4}, {4, 4}, {5, 4}, {6, 4}, {7, 4}},
        {{0, 1}, {1, 1}, {2, 1}, {3, 1}, {4, 1}, {5, 1},
             {2, 5}, {3, 5}, {4, 5}, {5, 5}, {6, 5}, {7, 5}},
        {{0, 2}, {1, 2}, {2, 2}, {3, 2}, {4, 2}, {5, 2},
             {6, 2}, {3, 6}, {4, 6}, {5, 6}, {6, 6}, {7, 6}},
        {{0, 3}, {1, 3}, {2, 3}, {3, 3}, {4, 3}, {5, 3},
             {6, 3}, {7, 3}, {4, 7}, {5, 7}, {6, 7}, {7, 7}},
        {{4, 0}, {1, 4}, {2, 4}, {3, 4}, {4, 4}, {5, 4},
             {6, 4}, {7, 4}, {0, 0}, {1, 0}, {2, 0}, {3, 0}},
        {{4, 1}, {5, 1}, {2, 5}, {3, 5}, {4, 5}, {5, 5},
             {6, 5}, {7, 5}, {0, 1}, {1, 1}, {2, 1}, {3, 1}},
        {{4, 2}, {5, 2}, {6, 2}, {3, 6}, {4, 6}, {5, 6},
             {6, 6}, {7, 6}, {0, 2}, {1, 2}, {2, 2}, {3, 2}},
        {{4, 3}, {5, 3}, {6, 3}, {7, 3}, {4, 7}, {5, 7},
             {6, 7}, {7, 7}, {0, 3}, {1, 3}, {2, 3}, {3, 3}},
        {{4, 4}, {5, 4}, {6, 4}, {7, 4}, {0, 0}, {1, 0},
             {2, 0}, {3, 0}, {4, 0}, {1, 4}, {2, 4}, {3, 4}},
        {{4, 5}, {5, 5}, {6, 5}, {7, 5}, {0, 1}, {1, 1},
             {2, 1}, {3, 1}, {4, 1}, {5, 1}, {2, 5}, {3, 5}},
        {{4, 6}, {5, 6}, {6, 6}, {7, 6}, {0, 2}, {1, 2},
             {2, 2}, {3, 2}, {4, 2}, {5, 2}, {6, 2}, {3, 6}},
        {{4, 7}, {5, 7}, {6, 7}, {7, 7}, {0, 3}, {1, 3},
             {2, 3}, {3, 3}, {4, 3}, {5, 3}, {6, 3}, {7, 3}}
        };

    /**
     * Test of getEthernetChip method, of class SpiNNakerTriadGeometry.
     */
    @Test
    public void testLocalChipCoordinate() {
        SpiNNakerTriadGeometry instance = SpiNNakerTriadGeometry.getSpinn5Geometry();
        for (int x = 0; x < 12; x++) {
            for (int y = 0; y < 12; y++) {
                //px, py = delta_table[y][x]
                ChipLocation result = instance.getLocalChipCoordinate(new ChipLocation(x, y));
                assertEquals(localTable[y][x][0], result.getX());
                assertEquals(localTable[y][x][1], result.getY());
            }
        }

        assertEquals(new ChipLocation(0,0), instance.getLocalChipCoordinate(new ChipLocation(32,28)));
        assertEquals(new ChipLocation(1,1), instance.getLocalChipCoordinate(new ChipLocation(33,29)));
    }

    @Test
    public void testGetEthernetChip() {
        SpiNNakerTriadGeometry instance = SpiNNakerTriadGeometry.getSpinn5Geometry();
        assertEquals(new ChipLocation(0,0), instance.getRootChip(new ChipLocation(0,0), 96, 60));
        assertEquals(new ChipLocation(0,0), instance.getRootChip(new ChipLocation(0,3), 96, 60));
        assertEquals(new ChipLocation(0,0), instance.getRootChip(new ChipLocation(4,7), 96, 60));
        assertEquals(new ChipLocation(0,0), instance.getRootChip(new ChipLocation(7,7), 96, 60));
        assertEquals(new ChipLocation(0,0), instance.getRootChip(new ChipLocation(7,3), 96, 60));
        assertEquals(new ChipLocation(0,0), instance.getRootChip(new ChipLocation(4,0), 96, 60));

        assertEquals(new ChipLocation(0,0), instance.getLocalChipCoordinate(new ChipLocation(8,4)));
        assertEquals(new ChipLocation(8,4), instance.getRootChip(new ChipLocation(8,4), 96, 60));
        assertEquals(new ChipLocation(8,4), instance.getRootChip(new ChipLocation(8,7), 96, 60));
        assertEquals(new ChipLocation(8,4), instance.getRootChip(new ChipLocation(11,10), 96, 60));
        assertEquals(new ChipLocation(8,4), instance.getRootChip(new ChipLocation(11,4), 96, 60));
        assertEquals(new ChipLocation(8,4), instance.getRootChip(new ChipLocation(0,4), 12, 12));
        assertEquals(new ChipLocation(8,4), instance.getRootChip(new ChipLocation(0,11), 12, 12));
        assertEquals(new ChipLocation(92,4), instance.getRootChip(new ChipLocation(0,11), 60, 96));
        assertEquals(new ChipLocation(8,4), instance.getRootChip(new ChipLocation(3,11), 12, 12));
        assertEquals(new ChipLocation(8,4), instance.getRootChip(new ChipLocation(3,7), 12, 12));

        assertEquals(new ChipLocation(4,8), instance.getRootChip(new ChipLocation(4,8), 96, 60));
        assertEquals(new ChipLocation(4,8), instance.getRootChip(new ChipLocation(4,11), 12, 12));
        assertEquals(new ChipLocation(4,8), instance.getRootChip(new ChipLocation(11,11), 12, 12));
        assertEquals(new ChipLocation(4,8), instance.getRootChip(new ChipLocation(8,8), 60, 96));
        assertEquals(new ChipLocation(4,8), instance.getRootChip(new ChipLocation(8,3), 12, 12));
        assertEquals(new ChipLocation(4,8), instance.getRootChip(new ChipLocation(11,3), 12, 12));
        assertEquals(new ChipLocation(4,8), instance.getRootChip(new ChipLocation(11,0), 12, 12));
        assertEquals(new ChipLocation(4,8), instance.getRootChip(new ChipLocation(5,0), 12, 12));
        assertEquals(new ChipLocation(4,56), instance.getRootChip(new ChipLocation(5,0), 60, 96));
    }

    @Test
    public void testGetPotentialEthernetChips() {
        SpiNNakerTriadGeometry instance = SpiNNakerTriadGeometry.getSpinn5Geometry();

        Collection<ChipLocation> ethers = instance.getPotentialRootChips(
                new MachineDimensions(2, 2));
        assertThat(ethers, contains(ChipLocation.ZERO_ZERO));

        ethers = instance.getPotentialRootChips(new MachineDimensions(8, 8));
        assertThat(ethers, contains(ChipLocation.ZERO_ZERO));

        ChipLocation chip48 = new ChipLocation(4, 8);
        ChipLocation chip84 = new ChipLocation(8, 4);

        ethers = instance.getPotentialRootChips(new MachineDimensions(12, 12));
        assertThat(ethers, containsInAnyOrder(
                ChipLocation.ZERO_ZERO, chip48, chip84));

        ethers = instance.getPotentialRootChips(new MachineDimensions(16, 16));
        assertThat(ethers, containsInAnyOrder(
                ChipLocation.ZERO_ZERO, chip48, chip84));

        ChipLocation chip12_12 = new ChipLocation(12, 12);
        ChipLocation chip0_12 = new ChipLocation(0, 12);
        ChipLocation chip12_0 = new ChipLocation(12, 0);

        ethers = instance.getPotentialRootChips(new MachineDimensions(20, 20));
        assertThat(ethers, containsInAnyOrder(
                ChipLocation.ZERO_ZERO, chip48, chip84, chip12_0, chip12_12,
                chip0_12));

        ethers = instance.getPotentialRootChips(new MachineDimensions(24, 24));
        assertThat(ethers, hasSize(12));

    }

    @Test
    public void testSingleBoard() {
        SpiNNakerTriadGeometry instance = SpiNNakerTriadGeometry.getSpinn5Geometry();
        int count = 0;
        for (ChipLocation chip: instance.singleBoard()) {
            count+= 1;
            assertEquals(ChipLocation.ZERO_ZERO, instance.getRootChip(chip, 12, 12));
        }
        assertEquals(48, count);
    }

    @Test
    public void testSingleBoardForEach() {
        SpiNNakerTriadGeometry instance = SpiNNakerTriadGeometry.getSpinn5Geometry();
        final Counter count = new Counter();
        Consumer<ChipLocation> checkandCount = chip -> {
            count.increment();
            assertEquals(ChipLocation.ZERO_ZERO, instance.getRootChip(chip, 12, 12));
        };
        instance.singleBoardForEach(checkandCount);
        assertEquals(48, count.get());
    }

}
