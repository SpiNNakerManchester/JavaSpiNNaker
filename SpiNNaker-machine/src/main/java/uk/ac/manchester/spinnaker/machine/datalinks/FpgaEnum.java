/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine.datalinks;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/**
 * Hard codes all the FPGA Link Ids.
 * <p>
 * <img src="doc-files/spin4-5-1.png" width="600" height="600"
 *      alt="Diagram of the FPGA links">
 * </p>
 * Based on the <a
 * href="https://drive.google.com/open?id=0B9312BuJXntlVWowQlJ3RE8wWVE">
 * FPGA Diagram</a>
 *
 * @author Christian-B
 */
public enum FpgaEnum implements HasChipLocation {

    //Left Side
    /**  Link 0 on FPGA 1/Left From Chip(0,0) towards the SouthWest. */
    ZERO_ZERO_SW(0, 0, Direction.SOUTHWEST, FpgaId.LEFT, 0),

    /**  Link 1 on FPGA 1/Left From Chip(0,0) towards the West. */
    ZERO_ZERO_W(0, 0, Direction.WEST, FpgaId.LEFT, 1),

    /**  Link 2 on FPGA 1/Left From Chip(0,1) towards the SouthWest. */
    ZERO_ONE_SW(0, 1, Direction.SOUTHWEST, FpgaId.LEFT, 2),

    /**  Link 3 on FPGA 1/Left From Chip(0,1) towards the West. */
    ZERO_ONE_W(0, 1, Direction.WEST, FpgaId.LEFT, 3),

    /**  Link 4 on FPGA 1/Left From Chip(0,2) towards the SouthWest. */
    ZERO_TWO_SW(0, 2, Direction.SOUTHWEST, FpgaId.LEFT, 4),

    /**  Link 5 on FPGA 1/Left From Chip(0,2) towards the West. */
    ZERO_TWO_W(0, 2, Direction.WEST, FpgaId.LEFT, 5),

    /**  Link 6 on FPGA 1/Left From Chip(0,3) towards the SouthWest. */
    ZERO_THREE_SW(0, 3, Direction.SOUTHWEST, FpgaId.LEFT, 6),

    /**  Link 7 on FPGA 1/Left From Chip(0,3) towards the West. */
    ZERO_THREE_W(0, 3, Direction.WEST, FpgaId.LEFT, 7),

    //Left Top
    /**  Link 8 on FPGA 1/Left From Chip(0,3) towards the North. */
    ZERO_THREE_N(0, 3, Direction.NORTH, FpgaId.LEFT, 8),

    /**  Link 9 on FPGA 1/Left From Chip(1,4) towards the West. */
    ONE_FOUR_W(1, 4, Direction.WEST, FpgaId.LEFT, 9),

    /**  Link 10 on FPGA 1/Left From Chip(1,4) towards the North. */
    ONE_FOUR_N(1, 4, Direction.NORTH, FpgaId.LEFT, 10),

    /**  Link 11 on FPGA 1/Left From Chip(2,5) towards the West. */
    TWO_FIVE_W(2, 5, Direction.WEST, FpgaId.LEFT, 11),

    /**  Link 12 on FPGA 1/Left From Chip(2,5) towards the North. */
    TWO_FIVE_N(2, 5, Direction.NORTH, FpgaId.LEFT, 12),

    /**  Link 13 on FPGA 1/Left From Chip(3,6) towards the West. */
    THREE_SIX_W(3, 6, Direction.WEST, FpgaId.LEFT, 13),

    /**  Link 14 on FPGA 1/Left From Chip(3,6) towards the North. */
    THREE_SIX_N(3, 6, Direction.NORTH, FpgaId.LEFT, 14),

    /**  Link 15 on FPGA 1/Left From Chip(4,7) towards the West. */
    FOUR_SEVEN_W(4, 7, Direction.WEST, FpgaId.LEFT, 15),


    // Top
    /**  Link 0 on FPGA 2/TopRight From Chip(4,7) towards the North. */
    FOUR_SEVEN_N(4, 7, Direction.NORTH, FpgaId.TOP_RIGHT, 0),

    /**  Link 1 on FPGA 2/TopRight From Chip(4,7) towards the NorthEast. */
    FOUR_SEVEN_NE(4, 7, Direction.NORTHEAST, FpgaId.TOP_RIGHT, 1),

    /**  Link 2 on FPGA 2/TopRight From Chip(5,7) towards the North. */
    FIVE_SEVEN_N(5, 7, Direction.NORTH, FpgaId.TOP_RIGHT, 2),

    /**  Link 3 on FPGA 2/TopRight From Chip(5,7) towards the NorthEast. */
    FIVE_SEVEN_NE(5, 7, Direction.NORTHEAST, FpgaId.TOP_RIGHT, 3),

    /**  Link 4 on FPGA 2/TopRight From Chip(6,7) towards the North. */
    SIX_SEVEN_N(6, 7, Direction.NORTH, FpgaId.TOP_RIGHT, 4),

    /**  Link 5 on FPGA 2/TopRight From Chip(6,7) towards the NorthEast. */
    SIX_SEVEN_NE(6, 7, Direction.NORTHEAST, FpgaId.TOP_RIGHT, 5),

    /**  Link 6 on FPGA 2/TopRight From Chip(7,7) towards the North. */
    SEVEN_SEVEN_N(7, 7, Direction.NORTH, FpgaId.TOP_RIGHT, 6),

    /**  Link 7 on FPGA 2/TopRight From Chip(7,7) towards the NorthEast. */
    SEVEN_SEVEN_NE(7, 7, Direction.NORTHEAST, FpgaId.TOP_RIGHT, 7),

    // Right
    /**  Link 8 on FPGA 2/TopRight From Chip(7,7) towards the East. */
    SEVEN_SEVEN_E(7, 7, Direction.EAST, FpgaId.TOP_RIGHT, 8),

    /**  Link 9 on FPGA 2/TopRight From Chip(7,6) towards the NorthEast. */
    SEVEN_SIX_NE(7, 6, Direction.NORTHEAST, FpgaId.TOP_RIGHT, 9),

    /**  Link 10 on FPGA 2/TopRight From Chip(7,6) towards the East. */
    SEVEN_SIX_E(7, 6, Direction.EAST, FpgaId.TOP_RIGHT, 10),

    /**  Link 11 on FPGA 2/TopRight From Chip(7,5) towards the NorthEast. */
    SEVEN_FIVE_NE(7, 5, Direction.NORTHEAST, FpgaId.TOP_RIGHT, 11),

    /**  Link 12 on FPGA 2/TopRight From Chip(7,5) towards the East. */
    SEVEN_FIVE_E(7, 5, Direction.EAST, FpgaId.TOP_RIGHT, 12),

    /**  Link 13 on FPGA 2/TopRight From Chip(7,4) towards the NorthEast. */
    SEVEN_FOUR_NE(7, 4, Direction.NORTHEAST, FpgaId.TOP_RIGHT, 13),

    /**  Link 14 on FPGA 2/TopRight From Chip(7,4) towards the East. */
    SEVEN_FOUR_E(7, 4, Direction.EAST, FpgaId.TOP_RIGHT, 14),

    /**  Link 15 on FPGA 2/TopRight From Chip(7,3) towards the NorthEast. */
    SEVEN_THREE_NE(7, 3, Direction.NORTHEAST, FpgaId.TOP_RIGHT, 15),

    // Bottom Left
    /**  Link 0 on FPGA 0/Bottom From Chip(7,3) towards the East. */
    SEVEN_THREE_E(7, 3, Direction.EAST, FpgaId.BOTTOM, 0),

    /**  Link 1 on FPGA 0/Bottom From Chip(7,3) towards the South. */
    SEVEN_THREE_S(7, 3, Direction.SOUTH, FpgaId.BOTTOM, 1),

    /**  Link 2 on FPGA 0/Bottom From Chip(6,2) towards the East. */
    SIX_TWO_E(6, 2, Direction.EAST, FpgaId.BOTTOM, 2),

    /**  Link 3 on FPGA 0/Bottom From Chip(6,2) towards the South. */
    SIX_TWO_S(6, 2, Direction.SOUTH, FpgaId.BOTTOM, 3),

    /**  Link 4 on FPGA 0/Bottom From Chip(5,1) towards the East. */
    FIVE_ONE_E(5, 1, Direction.EAST, FpgaId.BOTTOM, 4),

    /**  Link 5 on FPGA 0/Bottom From Chip(5,1) towards the South. */
    FIVE_ONE_S(5, 1, Direction.SOUTH, FpgaId.BOTTOM, 5),

    /**  Link 6 on FPGA 0/Bottom From Chip(4,0) towards the East. */
    FOUR_ZERO_E(4, 0, Direction.EAST, FpgaId.BOTTOM, 6),

    // Bottom
    /**  Link 7 on FPGA 0/Bottom From Chip(4,0) towards the South. */
    FOUR_ZERO_S(4, 0, Direction.SOUTH, FpgaId.BOTTOM, 7),

    /**  Link 8 on FPGA 0/Bottom From Chip(4,0) towards the SouthWest. */
    FOUR_ZERO_SW(4, 0, Direction.SOUTHWEST, FpgaId.BOTTOM, 8),

    /**  Link 9 on FPGA 0/Bottom From Chip(3,0) towards the South. */
    THREE_ZERO_S(3, 0, Direction.SOUTH, FpgaId.BOTTOM, 9),

    /**  Link 10 on FPGA 0/Bottom From Chip(3,0) towards the SouthWest. */
    THREE_ZERO_SW(3, 0, Direction.SOUTHWEST, FpgaId.BOTTOM, 10),

    /**  Link 11 on FPGA 0/Bottom From Chip(3,0) towards the South. */
    TWO_ZERO_S(2, 0, Direction.SOUTH, FpgaId.BOTTOM, 11),

    /**  Link 12 on FPGA 0/Bottom From Chip(2,0) towards the SouthWest. */
    TWO_ZERO_SW(2, 0, Direction.SOUTHWEST, FpgaId.BOTTOM, 12),

    /**  Link 13 on FPGA 0/Bottom From Chip(2,0) towards the South. */
    ONE_ZERO_S(1, 0, Direction.SOUTH, FpgaId.BOTTOM, 13),

    /**  Link 14 on FPGA 0/Bottom From Chip(1,0) towards the SouthWest. */
    ONE_ZERO_SW(1, 0, Direction.SOUTHWEST, FpgaId.BOTTOM, 14),

    /**  Link 15 on FPGA 0/Bottom From Chip(0,0) towards the South. */
    ZERO_ZERO_S(0, 0, Direction.SOUTH, FpgaId.BOTTOM, 15);

    private final int x;

    private final int y;

    /** Direction of the link as it comes out of the source Chip. */
    public final Direction direction;

    /** Id of the FPGA. */
    public final FpgaId fpgaId;

    /** Id of the FPGA link. */
    public final int id;

    /**
     * Constructor for the enum.
     *
     * @param x x coordinate of the source Chip.
     * @param y y coordinate of the source Chip.
     * @param direction
     *      Direction of the link as it comes out of the source Chip.
     * @param fpgaId Id of the FPGA device.
     * @param id  Id of the link on this FPGA device.
     */
    FpgaEnum(int x, int y, Direction direction, FpgaId fpgaId, int id) {
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.fpgaId = fpgaId;
        this.id = id;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public ChipLocation asChipLocation() {
        return new ChipLocation(x, y);
    }

    /**
     * Find the Enum by chip coordinates and the direction.
     *
     * @param x X coordinate of chip.
     * @param y X coordinate of chip.
     * @param direction Direction of the link covered buy the fpga.
     *
     * @return The corresponding Enum.
     */
    public static FpgaEnum findId(int x, int y, Direction direction) {
        for (FpgaEnum linkId:FpgaEnum.values()) {
            if (linkId.x == x && linkId.y == y
                    && linkId.direction == direction) {
                return linkId;
            }
        }
        throw new IllegalArgumentException(
                "No FPGA link found for y: " + x + " x: " + y
                + " direction: " + direction);
    }

    /**
     * Find the Enum by fpga and link ids.
     *
     * @param fpgaId Id of the fpga device.
     * @param id Id of the link on that device.
     * @return The corresponding Enum.
     */
    public static FpgaEnum findId(FpgaId fpgaId, int id) {
        for (FpgaEnum linkId:FpgaEnum.values()) {
            if (linkId.fpgaId == fpgaId && linkId.id == id) {
                return linkId;
            }
        }
        throw new IllegalArgumentException(
                "No FPGA link found for fpgaId: " + fpgaId + " id: " + id);
    }
}
