/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.util.ArrayList;
import java.util.HashSet;

// TO FINISH
/**
 *  Geometry of a "triad" of SpiNNaker boards.
 *  <p>
 *  The geometry is defined by the arguments to the constructor; the
 *      standard arrangement can be obtained from get_spinn5_geometry.
 *  <p>
 *  Note that the geometry defines what a Triad is in terms of the dimensions
 *      of a triad and where the Ethernet chips occur in the triad.
 *
 * @see <a
 * href="https://github.com/SpiNNakerManchester/SpiNNMachine/blob/master/spinn_machine/chip.py">
 * Python Version</a>
 *
 * @author Christian-B
 */
public class SpiNNakerTriadGeometryComplex {

    private static SpiNNakerTriadGeometryComplex SPINN5_TRIAD_GEOMETRY = null;

    private final Location[][] ethernetOffset;

    /** Height of a triad in chips. */
    public final int height;

    /** Width of a triad in chips. */
    public final int width;

    /** Locations of the Ethernet connected chips */
    private final ArrayList<ChipLocation> roots;

    private SpiNNakerTriadGeometryComplex(
            int triadHeight, int triadWidth, ArrayList<ChipLocation> roots, float centreX, float centreY) {
        this.height = triadHeight;
        this.width = triadWidth;
        this.roots = roots;
        ArrayList<Location> extendedEthernetChips = new ArrayList();
        for (ChipLocation root:roots) {
            for (int x1 = -width; x1 <= width; x1+=width) {
                for (int y1 = -height; y1 <= height; y1+=height) {
                    extendedEthernetChips.add(
                            new Location(root.getX() + x1, root.getY() + y1));
                }
            }
        }
        //python: [(-12, -12), (-12, 0), (-12, 12), (0, -12), (0, 0), (0, 12), (12, -12), (12, 0), (12, 12), (-8, -4), (-8, 8), (-8, 20), (4, -4), (4, 8), (4, 20), (16, -4), (16, 8), (16, 20), (-4, -8), (-4, 4), (-4, 16), (8, -8), (8, 4), (8, 16), (20, -8), (20, 4), (20, 16)]
        //Java    [(-12, -12), (-12, 0), (-12, 12), (0, -12), (0, 0), (0, 12), (12, -12), (12, 0), (12, 12), (-8, -4), (-8, 8), (-8, 20), (4, -4), (4, 8), (4, 20), (16, -4), (16, 8), (16, 20), (-4, -8), (-4, 4), (-4, 16), (8, -8), (8, 4), (8, 16), (20, -8), (20, 4), (20, 16)]
        ethernetOffset = new Location[width][height];

    }

    /**
     * Get the hexagonal metric distance of a point from the centre of the
     * hexagon.
     * <p>
     * Computes the max of the magnitude of the dot products with the normal
     * vectors for the hexagon sides.
     * The normal vectors are (1,0), (0,1) and (1,-1);
     * we don't need to be careful with the signs of the vectors because we're
     * about to do abs() of them anyway.
     * @param x The x-coordinate of the chip to get the distance for
     * @param y The y-coordinate of the chip to get the distance for
     * @param x_centre The x-coordinate of the centre of the hexagon.
     *      Note that this is the theoretical centre,
     *      it might not be an actual chip
     * @param y_centre The y-coordinate of the centre of the hexagon.
     *      Note that this is the theoretical centre,
     *      it might not be an actual chip
     * @return how far the chip is away from the centre of the hexagon
     */
    private float HexagonalMetricDistance(int x, int y, float x_centre, float y_centre) {
        float dx = x - x_centre;
        float dy = y - y_centre;
        return Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dx - dy)));
    }

    private Location locateNearestEthernet(
            int x, int y, ArrayList<Location> ethernet_chips, float x_c, float y_c) {
        //""" Get the coordinate of the nearest Ethernet chip down and left from\
        //    a given chip

        //:param x: The x-coordinate of the chip to find the nearest Ethernet to
        //:param y: The y-coordinate of the chip to find the nearest Ethernet to
        //:param ethernet_chips: The locations of the Ethernet chips
        //:param centre:\
        //    The distance from the Ethernet chip of the centre of the hexagonal\
        //    board
        //:return: The nearest Ethernet coordinates as a tuple of x, y
        //"""

        // Find the coordinates of the closest Ethernet chip by measuring
        // the distance to the nominal centre of each board; the closest
        // Ethernet is the one that is on the same board as the one the chip
        // is closest to the centre of
        //x1, y1, _ = min(
        //    ((x0, y0, self._hexagonal_metric_distance(
        //        x, y, x0 + x_c, y0 + y_c))
        //     for x0, y0 in ethernet_chips),
        //    key=lambda tupl: tupl[2])
        //return (x1, y1)
        return null;
    }

    //<class 'list'>: [[(0, 0), (1, 0), (2, 0), (3, 0), (4, 0), (1, 4), (2, 4), (3, 4), (4, 4), (5, 4), (6, 4), (7, 4)], [(0, 1), (1, 1), (2, 1), (3, 1), (4, 1), (5, 1), (2, 5), (3, 5), (4, 5), (5, 5), (6, 5), (7, 5)], [(0, 2), (1, 2), (2, 2), (3, 2), (4, 2), (5, 2), (6, 2), (3, 6), (4, 6), (5, 6), (6, 6), (7, 6)], [(0, 3), (1, 3), (2, 3), (3, 3), (4, 3), (5, 3), (6, 3), (7, 3), (4, 7), (5, 7), (6, 7), (7, 7)], [(4, 0), (1, 4), (2, 4), (3, 4), (4, 4), (5, 4), (6, 4), (7, 4), (0, 0), (1, 0), (2, 0), (3, 0)], [(4, 1), (5, 1), (2, 5), (3, 5), (4, 5), (5, 5), (6, 5), (7, 5), (0, 1), (1, 1), (2, 1), (3, 1)], [(4, 2), (5, 2), (6, 2), (3, 6), (4, 6), (5, 6), (6, 6), (7, 6), (0, 2), (1, 2), (2, 2), (3, 2)], [(4, 3), (5, 3), (6, 3), (7, 3), (4, 7), (5, 7), (6, 7), (7, 7), (0, 3), (1, 3), (2, 3), (3, 3)], [(4, 4), (5, 4), (6, 4), (7, 4), (0, 0), (1, 0), (2, 0), (3, 0), (4, 0), (1, 4), (2, 4), (3, 4)], [(4, 5), (5, 5), (6, 5), (7, 5), (0, 1), (1, 1), (2, 1), (3, 1), (4, 1), (5, 1), (2, 5), (3, 5)], [(4, 6), (5, 6), (6, 6), (7, 6), (0, 2), (1, 2), (2, 2), (3, 2), (4, 2), (5, 2), (6, 2), (3, 6)], [(4, 7), (5, 7), (6, 7), (7, 7), (0, 3), (1, 3), (2, 3), (3, 3), (4, 3), (5, 3), (6, 3), (7, 3)]]

   /**
     * Get the geometry object for a SpiNN-5 arrangement of boards.
     * <p>
     * Note the centres are slightly offset so as to force which edges are
     *      included where
     * @return SpiNN5 geometry
     */
    public static SpiNNakerTriadGeometryComplex getSpinn5Geometry() {
        if (SPINN5_TRIAD_GEOMETRY == null) {
            ArrayList<ChipLocation> roots = new ArrayList<>();
            roots.add(new ChipLocation(0, 0));
            roots.add(new ChipLocation(4, 8));
            roots.add(new ChipLocation(8, 4));
            roots.add(new ChipLocation(-4, 4));
            roots.add(new ChipLocation(8, -4));
            SPINN5_TRIAD_GEOMETRY = new SpiNNakerTriadGeometryComplex(
                    12, 12, roots, (float)3.6,  (float)3.4);
        }
        return SPINN5_TRIAD_GEOMETRY;
    }

    public static void main(String[] args) {
        SpiNNakerTriadGeometryComplex test = getSpinn5Geometry();
        //System.out.println(test.nearestEthernets());
    }

    private class Location {
        final int x;
        final int y;

        Location(int x, int y){
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString(){
            return ("(" + x + ", " + y + ")");
        }
    }

}
