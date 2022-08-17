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

machine = Machine.with_standard_ips(
    "SpiNNaker1M", tags=set(["default", "machine-room"]),
    board_locations=board_locations_from_spinner("cabinets.csv"),
    base_ip="10.11.193.0")

configuration = Configuration(machines=[machine])
