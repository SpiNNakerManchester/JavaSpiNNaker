/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine.datalinks;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;

/**
 *
 * @author Christian-B
 */
public class TestFpgaLinkId {

    private int checkFpgaLink(
            FpgaId fpga, int linkId, int x, int y, Direction link) {
        FpgaEnum found =  FpgaEnum.findId(x, y, link);
        String message = "x: " + x +  " y: " + y + " direction: " + link;
        assertEquals(fpga, found.fpgaId, message + " fpga");
        assertEquals(linkId, found.id, message + " linkId");
        return linkId + 1;
    }

    private int checkLeftFpgaLinks(int leftLinkId, int x, int y) {
        leftLinkId = checkFpgaLink(FpgaId.LEFT, leftLinkId, x, y,
                Direction.SOUTHWEST);
        leftLinkId = checkFpgaLink(FpgaId.LEFT, leftLinkId, x, y,
                Direction.WEST);
        return leftLinkId;
    }

    private int checkLeftUpperLeftFpgaLinks(int leftLinkId, int x, int y) {
        leftLinkId = checkFpgaLink(FpgaId.LEFT, leftLinkId, x, y,
                Direction.SOUTHWEST);
        leftLinkId = checkFpgaLink(FpgaId.LEFT, leftLinkId, x, y,
                Direction.WEST);
        leftLinkId = checkFpgaLink(FpgaId.LEFT, leftLinkId, x, y,
                Direction.NORTH);
        return leftLinkId;
    }

    private int checkUpperLeftFpgaLinks(int leftLinkId, int x, int y) {
        leftLinkId = checkFpgaLink(FpgaId.LEFT, leftLinkId, x, y,
                Direction.WEST);
        leftLinkId = checkFpgaLink(FpgaId.LEFT, leftLinkId, x, y,
                Direction.NORTH);
        return leftLinkId;
    }

    private int checkUpperLeftTopFpgaLinks(
            int leftLinkId, int topLinkId, int x, int y) {
        leftLinkId = checkFpgaLink(FpgaId.LEFT, leftLinkId, x, y,
                Direction.WEST);
        topLinkId = checkFpgaLink(FpgaId.TOP_RIGHT, topLinkId, x, y,
                Direction.NORTH);
        topLinkId = checkFpgaLink(FpgaId.TOP_RIGHT, topLinkId, x, y,
                Direction.NORTHEAST);
        return topLinkId;
    }

    private int checkTopFpgaLinks(int topLinkId, int x, int y) {
        topLinkId = checkFpgaLink(FpgaId.TOP_RIGHT, topLinkId, x, y,
                Direction.NORTH);
        topLinkId = checkFpgaLink(FpgaId.TOP_RIGHT, topLinkId, x, y,
                Direction.NORTHEAST);
        return topLinkId;
    }

    private int checkTopRightFpgaLinks(int topLinkId, int x, int y) {
        topLinkId = checkFpgaLink(FpgaId.TOP_RIGHT, topLinkId, x, y,
                Direction.NORTH);
        topLinkId = checkFpgaLink(FpgaId.TOP_RIGHT, topLinkId, x, y,
                Direction.NORTHEAST);
        topLinkId = checkFpgaLink(FpgaId.TOP_RIGHT, topLinkId, x, y,
                Direction.EAST);
        return topLinkId;
    }

    private int checkRightFpgaLinks(int topLinkId, int x, int y) {
        topLinkId = checkFpgaLink(FpgaId.TOP_RIGHT, topLinkId, x, y,
                Direction.NORTHEAST);
        topLinkId = checkFpgaLink(FpgaId.TOP_RIGHT, topLinkId, x, y,
                Direction.EAST);
        return topLinkId;
    }

    private int checkRightLowerRightFpgaLinks(
            int topLinkId, int bottomLinkId, int x, int y) {
        topLinkId = checkFpgaLink(FpgaId.TOP_RIGHT, topLinkId, x, y,
                Direction.NORTHEAST);
        bottomLinkId = checkFpgaLink(FpgaId.BOTTOM, bottomLinkId, x, y,
                Direction.EAST);
        bottomLinkId = checkFpgaLink(FpgaId.BOTTOM, bottomLinkId, x, y,
                Direction.SOUTH);
        return bottomLinkId;
    }

    private int checkLowerRightFpgaLinks(int bottomLinkId, int x, int y) {
        bottomLinkId = checkFpgaLink(FpgaId.BOTTOM, bottomLinkId, x, y,
                Direction.EAST);
        bottomLinkId = checkFpgaLink(FpgaId.BOTTOM, bottomLinkId, x, y,
                Direction.SOUTH);
        return bottomLinkId;
    }

    private int checkLowerRightBottomFpgaLinks(int bottomLinkId, int x, int y) {
        bottomLinkId = checkFpgaLink(FpgaId.BOTTOM, bottomLinkId, x, y,
                Direction.EAST);
        bottomLinkId = checkFpgaLink(FpgaId.BOTTOM, bottomLinkId, x, y,
                Direction.SOUTH);
        bottomLinkId = checkFpgaLink(FpgaId.BOTTOM, bottomLinkId, x, y,
                Direction.SOUTHWEST);
        return bottomLinkId;
    }

    private int checkBottomFpgaLinks(int bottomLinkId, int x, int y) {
        bottomLinkId = checkFpgaLink(FpgaId.BOTTOM, bottomLinkId, x, y,
                Direction.SOUTH);
        bottomLinkId = checkFpgaLink(FpgaId.BOTTOM, bottomLinkId, x, y,
                Direction.SOUTHWEST);
        return bottomLinkId;
    }

    private int checkBottomLeftFpgaLinks(int bottomLinkId, int x, int y) {
        bottomLinkId = checkFpgaLink(FpgaId.BOTTOM, bottomLinkId, x, y,
                Direction.SOUTH);
        return bottomLinkId;
    }

    @Test
    public void testEach() {
        // the side of the hexagon shape of the board are as follows
        //                                               Top
        //                                     (4,7) (5,7) (6,7) (7,7)
        //                               (3,6) (4,6) (5,6) (6,6) (7,6)
        //             UpperLeft   (2,5) (3,5) (4,5) (5,5) (6,5) (7,5)   Right
        //                   (1,4) (2,4) (3,4) (4,4) (5,4) (6,4) (7,4)
        //             (0,3) (1,3) (2,3) (3,3) (4,3) (5,3) (6,2) (7,3)
        //             (0,2) (1,2) (2,2) (3,2) (4,2) (5,2) (6,2)
        //    Left     (0,1) (1,1) (2,1) (3,1) (4,1) (5,1)    LowerRight
        //             (0,0) (1,0) (2,0) (3,0) (4,0)
        //                          Bottom

        int leftLinkId = 0;
        int topLinkId = 0;
        int bottomLinkId = 0;

        leftLinkId = checkLeftFpgaLinks(leftLinkId, 0, 0);
        leftLinkId = checkLeftFpgaLinks(leftLinkId, 0, 1);
        leftLinkId = checkLeftFpgaLinks(leftLinkId, 0, 2);

        leftLinkId = checkLeftUpperLeftFpgaLinks(leftLinkId, 0, 3);

        leftLinkId = checkUpperLeftFpgaLinks(leftLinkId, 1, 4);
        leftLinkId = checkUpperLeftFpgaLinks(leftLinkId, 2, 5);
        leftLinkId = checkUpperLeftFpgaLinks(leftLinkId, 3, 6);

        topLinkId = checkUpperLeftTopFpgaLinks(leftLinkId, topLinkId, 4, 7);

        topLinkId = checkTopFpgaLinks(topLinkId, 5, 7);
        topLinkId = checkTopFpgaLinks(topLinkId, 6, 7);

        topLinkId = checkTopRightFpgaLinks(topLinkId, 7, 7);

        topLinkId = checkRightFpgaLinks(topLinkId, 7, 6);
        topLinkId = checkRightFpgaLinks(topLinkId, 7, 5);
        topLinkId = checkRightFpgaLinks(topLinkId, 7, 4);

        bottomLinkId = checkRightLowerRightFpgaLinks(topLinkId, bottomLinkId, 7, 3);

        bottomLinkId = checkLowerRightFpgaLinks(bottomLinkId, 6, 2);
        bottomLinkId = checkLowerRightFpgaLinks(bottomLinkId, 5, 1);

        bottomLinkId = checkLowerRightBottomFpgaLinks(bottomLinkId, 4, 0);

        bottomLinkId = checkBottomFpgaLinks(bottomLinkId, 3, 0);
        bottomLinkId = checkBottomFpgaLinks(bottomLinkId, 2, 0);
        bottomLinkId = checkBottomFpgaLinks(bottomLinkId, 1, 0);

        checkBottomLeftFpgaLinks(bottomLinkId, 0, 0);
    }

    @Test
    public void testStatic() {
        FpgaEnum id1 = FpgaEnum.findId(6, 2, Direction.EAST);
        FpgaEnum id2 = FpgaEnum.findId(FpgaId.BOTTOM, 2);

        assertEquals(id1, id2);
        assertEquals(new ChipLocation(6, 2), id1.asChipLocation());
        assertEquals(6, id1.getX());
        assertEquals(2, id1.getY());

        assertThrows(IllegalArgumentException.class, () -> {
            FpgaEnum.findId(2, 2, Direction.EAST);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            FpgaEnum.findId(FpgaId.BOTTOM, 16);
        });
    }

}
