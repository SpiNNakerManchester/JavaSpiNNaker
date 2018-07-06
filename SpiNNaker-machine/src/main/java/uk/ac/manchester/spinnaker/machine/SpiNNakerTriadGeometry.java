/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.util.ArrayList;
import java.util.HashMap;

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
public class SpiNNakerTriadGeometry {

    private static SpiNNakerTriadGeometry SPINN5_TRIAD_GEOMETRY = null;

    /** Height of a triad in chips. */
    public final int triadHeight;

    /** Width of a triad in chips. */
    public final int triadWidth;

    private final ArrayList<ChipLocation> realEthernets;

    private final HashMap<ChipLocation, ChipLocation> localChipCoordinates;

    private final float xCenterer;

    private final float yCenterer;

    /**
     * Constructor is private to force reuse of staticlly held Object(s).
     * 
     * @param triadHeight Height of a triad in chips.
     * @param triadWidth Width of a triad in chips.
     * @param roots Ethernet chips within the triad
     * @param xCenterer Magic number to adjust X to find the nearest ethernet.
     * @param yCenterer Magic number to adjust X to find the nearest ethernet.
     */
    private SpiNNakerTriadGeometry(
            int triadHeight, int triadWidth, ArrayList<ChipLocation> roots, 
            float xCenterer, float yCenterer) {
        this.triadHeight = triadHeight;
        this.triadWidth = triadWidth;
        this.realEthernets = new ArrayList<ChipLocation>();
  
        final ArrayList<Location> calulationEthernets = new ArrayList();

       
        for (Location location:roots) {
            if (location.x >= 0 && location.y >= 0) {
                realEthernets.add(new ChipLocation(location.x, location.y));
            }
        }
        this.xCenterer = xCenterer;
        this.yCenterer = yCenterer;

        localChipCoordinates = new HashMap<>();

        for (int x = 0; x < 12; x++) {
            for (int y = 0; y < 12; y++) {
                Location bestCalc = locateNearestCalulationEthernet(x, y);
                ChipLocation key = new ChipLocation(x, y);
                localChipCoordinates.put(key,
                        new ChipLocation((x - bestCalc.x), (y - bestCalc.y)));
            }
        }
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
    private float hexagonalMetricDistance(int x, int y, float x_centre, float y_centre) {
        float dx = x - x_centre;
        float dy = y - y_centre;
        return Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dx - dy)));
    }

    private Location locateNearestCalulationEthernet(
            int x, int y, ArrayList<Location> calulationEthernets) {
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
        Location bestCalc = null;
        float bestDistance = 10000;
        for (Location ethernet:calulationEthernets) {
            float calc = hexagonalMetricDistance(
                    x, y, ethernet.x + (float)3.6, ethernet.y + (float)3.4);
            if (calc < bestDistance) {
                bestDistance = calc;
                bestCalc = ethernet;
            }
        }
        return bestCalc;
    }

    public ChipLocation getEthernetChip(
            HasChipLocation chip, int machineHeight, int machineWidth) {
        //if (chip.getX() < triadWidth && chip.getY() < triadHeight) {
        //    ChipLocation localChip = localChipCoordinates.get(
        //            chip.asChipLocation());
        //    return new ChipLocation(chip.getX() - localChip.getX(),
        //        chip.getY() - localChip.getY());
        //}

        ChipLocation adjusted = new ChipLocation(
                chip.getX() % triadHeight, chip.getY() % triadWidth);
        ChipLocation localChip = localChipCoordinates.get(adjusted);

        return new ChipLocation(
                (chip.getX() - localChip.getX() + machineHeight) 
                        % machineHeight,
                (chip.getY() - localChip.getY() + machineWidth)
                        % machineWidth);
    }

    public ChipLocation getLocalChipCoordinate(HasChipLocation chip) {
        if (chip.getX() < triadWidth && chip.getY() < triadHeight) {
            return localChipCoordinates.get(chip);
        }

        int x = chip.getX() % triadWidth;
        int y = chip.getY() % triadHeight;
        ChipLocation adjusted = new ChipLocation(x, y);
        return localChipCoordinates.get(adjusted);
    }
    
     /**
      * Get the coordinates of chips that should be Ethernet chips.
      *
      * @param width The width of the machine to find the chips in.
      * @param height The height of the machine to find the chips in.
      * @return 
      */
    public ArrayList<ChipLocation> getPotentialEthernetChips(
            int width, int height) {
        ArrayList<ChipLocation> results = new ArrayList();
        int maxWidth;
        int maxHeight;
        if (width % triadWidth == 0 && height % triadHeight == 0) {
            maxWidth = width;
            maxHeight = height;
        } else {
            maxWidth = width - MachineDefaults.SIZE_X_OF_ONE_BOARD + 1;
            maxHeight = height - MachineDefaults.SIZE_Y_OF_ONE_BOARD + 1;  
            if (maxWidth < 0 || maxHeight < 0) {
                results.add(ChipLocation.ZERO_ZERO);
                return results;
            } 
        }
        for (ChipLocation chip:realEthernets) {
            for (int x = chip.getX(); x < maxWidth; x+=triadWidth) {
                for (int y = chip.getY(); y < maxHeight; y+=triadHeight) {
                    results.add(new ChipLocation(x, y));
                }
            }
        }
        return results;
    }
    
   /**
     * Get the geometry object for a SpiNN-5 arrangement of boards.
     * <p>
     * Note the centres are slightly offset so as to force which edges are
     *      included where
     * @return SpiNN5 geometry
     */
    public static SpiNNakerTriadGeometry getSpinn5Geometry() {
        if (SPINN5_TRIAD_GEOMETRY == null) {
            ArrayList<Location> roots = new ArrayList<>();
            roots.add(new Location(0, 0));
            roots.add(new Location(4, 8));
            roots.add(new Location(8, 4));
            roots.add(new Location(-4, 4));
            roots.add(new Location(4, -4));
            SPINN5_TRIAD_GEOMETRY = new SpiNNakerTriadGeometry(
                    12, 12, roots, (float)3.6,  (float)3.4);
        }
        return SPINN5_TRIAD_GEOMETRY;
    }

    static final class Location {
        final int x;
        final int y;

        Location(int x, int y){
            this.x = x;
            this.y = y;
        }

        //@Override
        //public String toString(){
        //    return ("(" + x + ", " + y + ")");
        //}
    }


}
