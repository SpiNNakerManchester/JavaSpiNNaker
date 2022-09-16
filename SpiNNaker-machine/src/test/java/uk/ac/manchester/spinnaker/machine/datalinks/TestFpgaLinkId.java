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
package uk.ac.manchester.spinnaker.machine.datalinks;

import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.machine.Direction.EAST;
import static uk.ac.manchester.spinnaker.machine.Direction.NORTH;
import static uk.ac.manchester.spinnaker.machine.Direction.NORTHEAST;
import static uk.ac.manchester.spinnaker.machine.Direction.SOUTH;
import static uk.ac.manchester.spinnaker.machine.Direction.SOUTHWEST;
import static uk.ac.manchester.spinnaker.machine.Direction.WEST;
import static uk.ac.manchester.spinnaker.machine.datalinks.FpgaId.BOTTOM;
import static uk.ac.manchester.spinnaker.machine.datalinks.FpgaId.LEFT;
import static uk.ac.manchester.spinnaker.machine.datalinks.FpgaId.TOP_RIGHT;

import org.junit.jupiter.api.Test;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;

/**
 *
 * @author Christian-B
 */
public class TestFpgaLinkId {

	private int checkFpgaLink(FpgaId fpga, int linkId, int x, int y,
			Direction link) {
		var found = FpgaEnum.findId(x, y, link);
		var message = "x: " + x + " y: " + y + " direction: " + link;
		assertEquals(fpga, found.fpgaId, message + " fpga");
		assertEquals(linkId, found.id, message + " linkId");
		return linkId + 1;
	}

	private int checkLeftFpgaLinks(int leftLinkId, int x, int y) {
		leftLinkId = checkFpgaLink(LEFT, leftLinkId, x, y, SOUTHWEST);
		leftLinkId = checkFpgaLink(LEFT, leftLinkId, x, y, WEST);
		return leftLinkId;
	}

	private int checkLeftUpperLeftFpgaLinks(int leftLinkId, int x, int y) {
		leftLinkId = checkFpgaLink(LEFT, leftLinkId, x, y, SOUTHWEST);
		leftLinkId = checkFpgaLink(LEFT, leftLinkId, x, y, WEST);
		leftLinkId = checkFpgaLink(LEFT, leftLinkId, x, y, NORTH);
		return leftLinkId;
	}

	private int checkUpperLeftFpgaLinks(int leftLinkId, int x, int y) {
		leftLinkId = checkFpgaLink(LEFT, leftLinkId, x, y, WEST);
		leftLinkId = checkFpgaLink(LEFT, leftLinkId, x, y, NORTH);
		return leftLinkId;
	}

	private int checkUpperLeftTopFpgaLinks(int leftLinkId, int topLinkId, int x,
			int y) {
		leftLinkId = checkFpgaLink(LEFT, leftLinkId, x, y, WEST);
		topLinkId = checkFpgaLink(TOP_RIGHT, topLinkId, x, y, NORTH);
		topLinkId = checkFpgaLink(TOP_RIGHT, topLinkId, x, y, NORTHEAST);
		return topLinkId;
	}

	private int checkTopFpgaLinks(int topLinkId, int x, int y) {
		topLinkId = checkFpgaLink(TOP_RIGHT, topLinkId, x, y, NORTH);
		topLinkId = checkFpgaLink(TOP_RIGHT, topLinkId, x, y, NORTHEAST);
		return topLinkId;
	}

	private int checkTopRightFpgaLinks(int topLinkId, int x, int y) {
		topLinkId = checkFpgaLink(TOP_RIGHT, topLinkId, x, y, NORTH);
		topLinkId = checkFpgaLink(TOP_RIGHT, topLinkId, x, y, NORTHEAST);
		topLinkId = checkFpgaLink(TOP_RIGHT, topLinkId, x, y, EAST);
		return topLinkId;
	}

	private int checkRightFpgaLinks(int topLinkId, int x, int y) {
		topLinkId = checkFpgaLink(TOP_RIGHT, topLinkId, x, y, NORTHEAST);
		topLinkId = checkFpgaLink(TOP_RIGHT, topLinkId, x, y, EAST);
		return topLinkId;
	}

	private int checkRightLowerRightFpgaLinks(int topLinkId, int bottomLinkId,
			int x, int y) {
		topLinkId = checkFpgaLink(TOP_RIGHT, topLinkId, x, y, NORTHEAST);
		bottomLinkId = checkFpgaLink(BOTTOM, bottomLinkId, x, y, EAST);
		bottomLinkId = checkFpgaLink(BOTTOM, bottomLinkId, x, y, SOUTH);
		return bottomLinkId;
	}

	private int checkLowerRightFpgaLinks(int bottomLinkId, int x, int y) {
		bottomLinkId = checkFpgaLink(BOTTOM, bottomLinkId, x, y, EAST);
		bottomLinkId = checkFpgaLink(BOTTOM, bottomLinkId, x, y, SOUTH);
		return bottomLinkId;
	}

	private int checkLowerRightBottomFpgaLinks(int bottomLinkId, int x, int y) {
		bottomLinkId = checkFpgaLink(BOTTOM, bottomLinkId, x, y, EAST);
		bottomLinkId = checkFpgaLink(BOTTOM, bottomLinkId, x, y, SOUTH);
		bottomLinkId = checkFpgaLink(BOTTOM, bottomLinkId, x, y, SOUTHWEST);
		return bottomLinkId;
	}

	private int checkBottomFpgaLinks(int bottomLinkId, int x, int y) {
		bottomLinkId = checkFpgaLink(BOTTOM, bottomLinkId, x, y, SOUTH);
		bottomLinkId = checkFpgaLink(BOTTOM, bottomLinkId, x, y, SOUTHWEST);
		return bottomLinkId;
	}

	private int checkBottomLeftFpgaLinks(int bottomLinkId, int x, int y) {
		bottomLinkId = checkFpgaLink(BOTTOM, bottomLinkId, x, y, SOUTH);
		return bottomLinkId;
	}

	@Test
	public void testEach() {
		// the side of the hexagon shape of the board are as follows
		// Top
		// (4,7) (5,7) (6,7) (7,7)
		// (3,6) (4,6) (5,6) (6,6) (7,6)
		// UpperLeft (2,5) (3,5) (4,5) (5,5) (6,5) (7,5) Right
		// (1,4) (2,4) (3,4) (4,4) (5,4) (6,4) (7,4)
		// (0,3) (1,3) (2,3) (3,3) (4,3) (5,3) (6,2) (7,3)
		// (0,2) (1,2) (2,2) (3,2) (4,2) (5,2) (6,2)
		// Left (0,1) (1,1) (2,1) (3,1) (4,1) (5,1) LowerRight
		// (0,0) (1,0) (2,0) (3,0) (4,0)
		// Bottom

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

		bottomLinkId =
				checkRightLowerRightFpgaLinks(topLinkId, bottomLinkId, 7, 3);

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
		var id1 = FpgaEnum.findId(6, 2, Direction.EAST);
		var id2 = FpgaEnum.findId(FpgaId.BOTTOM, 2);

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
