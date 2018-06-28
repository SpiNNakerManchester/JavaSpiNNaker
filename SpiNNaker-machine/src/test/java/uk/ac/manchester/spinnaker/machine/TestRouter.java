/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.util.ArrayList;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Christian-B
 */
public class TestRouter {

    ChipLocation chip00 = new ChipLocation(0,0);
    ChipLocation chip01 = new ChipLocation(0,1);
    ChipLocation chip10 = new ChipLocation(1,0);
    ChipLocation chip11 = new ChipLocation(1,1);

    @Test
    public void testRouterBasicUse() {
        ArrayList<Link> links = new ArrayList<>();
        links.add(new Link(chip00, Direction.NORTH, chip01));
        Router test = new Router(links);
        assertNotNull(test);
    }

}
