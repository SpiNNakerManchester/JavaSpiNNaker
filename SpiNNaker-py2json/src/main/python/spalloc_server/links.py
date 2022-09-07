# Copyright (c) 2017-2019 The University of Manchester
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

from enum import IntEnum


class Links(IntEnum):
    east = 0
    north_east = 1
    north = 2
    west = 3
    south_west = 4
    south = 5

    @classmethod
    def from_vector(cls, vector):
        x, y = vector

        if abs(x) > 1:
            x = -1 if x > 0 else 1
        if abs(y) > 1:
            y = -1 if y > 0 else 1

        lookup, _ = _LinksHelper.get_lookups()
        return lookup[(x, y)]  # pylint: disable=unsubscriptable-object

    def to_vector(self):
        _, lookup = _LinksHelper.get_lookups()
        return lookup[self]  # pylint: disable=unsubscriptable-object

    @property
    def opposite(self):
        return Links((self + 3) % 6)


class _LinksHelper(object):
    _link_direction_lookup = None
    _direction_link_lookup = None

    @classmethod
    def get_lookups(cls):
        if _LinksHelper._link_direction_lookup is None:
            ldl = _LinksHelper._link_direction_lookup = {
                (+1, +0): Links.east,
                (-1, +0): Links.west,
                (+0, +1): Links.north,
                (+0, -1): Links.south,
                (+1, +1): Links.north_east,
                (-1, -1): Links.south_west}
            _LinksHelper._direction_link_lookup = {
                l: v for (v, l) in ldl.items()}

            ldl[(+1, -1)] = Links.south_west
            ldl[(-1, +1)] = Links.north_east
        return _LinksHelper._link_direction_lookup, \
            _LinksHelper._direction_link_lookup
