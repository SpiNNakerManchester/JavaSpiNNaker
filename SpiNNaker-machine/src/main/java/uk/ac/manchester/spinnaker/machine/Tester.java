/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.util.ArrayList;

/**
 *
 * @author Christian-B
 */
public class Tester {

    final int x;
    final int y;

    Tester(int x, int y){
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString(){
        return ("(" + x + ", " + y + ")");
    }

    static boolean legalEthernet(int x, int y) {
        int modX = (x + 12) % 12;
        int modY = (y + 12) % 12;
        if (modX == 0 && modY == 0) {
            return true;
        }
        if (modX == 4 && modY == 8) {
            return true;
        }
        if (modX == 8 && modY == 4) {
            return true;
        }
        return false;
    }

    private static float hexagonalMetricDistance(int x, int y, float x_centre, float y_centre) {
        float dx = x - x_centre;
        float dy = y - y_centre;
        return Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dx - dy)));
    }

    private static Tester locateEthernet(int x, int y, ArrayList<Tester> ethernetChips) {
        Tester bestEthernet = null;
        float bestDistance = 10000;
        for (Tester ethernet:ethernetChips) {
            float calc = hexagonalMetricDistance(x, y, ethernet.x + (float)3.6, ethernet.y + (float)3.4);
            if (calc < bestDistance) {
                bestDistance = calc;
                bestEthernet = ethernet;
            }
        }
        //if (bestEthernet.x < 0) {
        //    bestEthernet = new Test(bestEthernet.x + 12, bestEthernet.y);
        //}
        //if (bestEthernet.y < 0) {
        //   bestEthernet = new Test(bestEthernet.x, bestEthernet.y + 12);
        //}
        return bestEthernet;
    }

    public static void main(String[] args) {
        ArrayList<Tester> extendedEthernetChips= new ArrayList<>();
        for (int x = -4; x <= 8; x++) {
            for (int y = -4; y <= 8; y++) {
                if (legalEthernet (x, y)) {
                    extendedEthernetChips.add(new Tester(x, y));
                }
            }
        }
        //Test[][] localEethernets = new Test[12][12];
        for (int x = 0; x < 12; x++) {
            for (int y = 0; y < 12; y++) {
                Tester localEthernet = locateEthernet(x, y, extendedEthernetChips);
                int localX = x - localEthernet.x;
                //if (localX < 0) {
                //    localX+= 8;
                //}
                int localY = y - localEthernet.y;
                //if (localY < 0) {
                //    localY += 8;
                //}
                System.out.println(x +"," + y + ": " + localEthernet + "  " + (localX) + "," + (localY));
            }
        }
        System.out.println((8 + 12) % 12);
    }
}
