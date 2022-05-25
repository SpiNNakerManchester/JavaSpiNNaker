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
package uk.ac.manchester.spinnaker.machine.bean;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.Machine;

/**
 *
 * @author Christian
 */
public class TestMachineBean {

    @Test
    public void testSpinn4() throws IOException {
        var url = TestMachineBean.class.getResource("/spinn4.json");
        var mapper = MapperFactory.createMapper();
        var fromJson = mapper.readValue(url, MachineBean.class);

        var machine = new Machine(fromJson);
        assertNotNull(machine);
    }

    @Test
    public void testSpinn4Fiddle() throws IOException {
        var url = TestMachineBean.class.getResource("/spinn4_fiddle.json");
        var mapper = MapperFactory.createMapper();
        var fromJson = mapper.readValue(url, MachineBean.class);

        var machine = new Machine(fromJson);
        assertNotNull(machine);
    }

    @Test
    public void testVirtual() throws IOException {
        var url = TestMachineBean.class.getResource("/with_virtual.json");
        var mapper = MapperFactory.createMapper();
        var fromJson = mapper.readValue(url, MachineBean.class);

        var machine = new Machine(fromJson);
        var virtual = machine.getChipAt(0, 4);
        assert (virtual.virtual);
        var fromVirtual = virtual.router.getLink(Direction.SOUTH);
        assertEquals (2, fromVirtual.destination.getX());
        assertEquals (5, fromVirtual.destination.getY());
        var connect = machine.getChipAt(2, 5);
        assert (!connect.virtual);
        var toVirtual = connect.router.getLink(Direction.NORTH);
        assertEquals (0, toVirtual.destination.getX());
        assertEquals (4, toVirtual.destination.getY());
    }

    @Test
    public void testSevenEight() throws IOException {
        var url = TestMachineBean.class.getResource("/test24_12.json");
        var mapper = MapperFactory.createMapper();
        var fromJson = mapper.readValue(url, MachineBean.class);

        var machine = new Machine(fromJson);
        assertEquals(24, machine.machineDimensions.height);
        assertEquals(12, machine.machineDimensions.width);
        assertNotNull(machine);
    }

    public void testPop() throws IOException {
        var url = TestMachineBean.class.getResource("/h40w16.json");
        var mapper = MapperFactory.createMapper();
        var fromJson = mapper.readValue(url, MachineBean.class);

        var machine = new Machine(fromJson);
        assertNotNull(machine);
    }
}