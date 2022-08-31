# Copyright (c) 2021 The University of Manchester
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

from spalloc_server.configuration import *
from spalloc_server.links import *

m = Machine(name="my-three-board-machine",
            board_locations={
                #X  Y  Z    C  F  B
                (0, 0, 0): (0, 0, 0),
                (0, 0, 1): (0, 0, 2),
                (0, 0, 2): (0, 0, 5),
            },
            # Just one BMP
            bmp_ips={
                #C  F
                (0, 0): "192.168.240.0",
            },
            # Each SpiNNaker board has an IP
            spinnaker_ips={
                #X  Y  Z
                (0, 0, 0): "192.168.240.1",
                (0, 0, 1): "192.168.240.17",
                (0, 0, 2): "192.168.240.41",
            },
            dead_links={
                #X  Y  Z  Direction
                (0, 0, 0, Links.east)
            })
configuration = Configuration(machines=[m])
