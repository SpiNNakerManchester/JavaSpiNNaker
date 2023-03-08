# Copyright (c) 2017 The University of Manchester
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

SIZE_X_OF_ONE_BOARD = 8
SIZE_Y_OF_ONE_BOARD = 8


class SpiNNakerTriadGeometry(object):
    __slots__ = [
        "_ethernet_offset",
        "_triad_height",
        "_triad_width",
        "_roots"]

    # Stored singleton
    spinn5_triad_geometry = None

    @staticmethod
    def get_spinn5_geometry():
        if SpiNNakerTriadGeometry.spinn5_triad_geometry is None:
            SpiNNakerTriadGeometry.spinn5_triad_geometry = \
                SpiNNakerTriadGeometry(
                    12, 12, [(0, 0), (4, 8), (8, 4)], (3.6, 3.4))
        return SpiNNakerTriadGeometry.spinn5_triad_geometry

    def __init__(self, triad_width, triad_height, roots, centre):
        self._triad_width = triad_width
        self._triad_height = triad_height
        self._roots = roots

        extended_ethernet_chips = [
            (x + x1, y + y1) for (x, y) in roots
            for x1 in (-triad_width, 0, triad_width)
            for y1 in (-triad_height, 0, triad_height)
        ]

        nearest_ethernets = (
            (self._locate_nearest_ethernet(
                x, y, extended_ethernet_chips, centre)
             for x in range(triad_width))
            for y in range(triad_height)
        )

        self._ethernet_offset = [
            [(x - vx, y - vy) for x, (vx, vy) in enumerate(row)]
            for y, row in enumerate(nearest_ethernets)]

    @staticmethod
    def _hexagonal_metric_distance(x, y, x_centre, y_centre):
        dx = x - x_centre
        dy = y - y_centre
        return max(abs(dx), abs(dy), abs(dx - dy))

    def _locate_nearest_ethernet(self, x, y, ethernet_chips, centre):
        x_c, y_c = centre

        # Find the coordinates of the closest Ethernet chip by measuring
        # the distance to the nominal centre of each board; the closest
        # Ethernet is the one that is on the same board as the one the chip
        # is closest to the centre of
        x1, y1, _ = min(
            ((x0, y0, self._hexagonal_metric_distance(
                x, y, x0 + x_c, y0 + y_c))
             for x0, y0 in ethernet_chips),
            key=lambda tupl: tupl[2])
        return (x1, y1)

    # pylint: disable=too-many-arguments
    def get_ethernet_chip_coordinates(
            self, x, y, width, height, root_x=0, root_y=0):
        dx, dy = self.get_local_chip_coordinate(x, y, root_x, root_y)
        return ((x - dx) % width), ((y - dy) % height)

    def get_local_chip_coordinate(self, x, y, root_x=0, root_y=0):
        dx = (x - root_x) % self._triad_width
        dy = (y - root_y) % self._triad_height
        return self._ethernet_offset[dy][dx]

    def get_potential_ethernet_chips(self, width, height):
        if width % self._triad_width == 0:
            eth_width = width
        else:
            eth_width = width - SIZE_X_OF_ONE_BOARD + 1
        if height % self._triad_height == 0:
            eth_height = height
        else:
            eth_height = height - SIZE_Y_OF_ONE_BOARD + 1
        # special case for single boards like the 2,2
        if (eth_width <= 0 or eth_height <= 0):
            return [(0, 0)]
        return [
            (x, y)
            for start_x, start_y in self._roots
            for y in range(start_y, eth_height, self._triad_height)
            for x in range(start_x, eth_width, self._triad_width)]
