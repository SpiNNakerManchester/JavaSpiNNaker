-- Copyright (c) 2022 The University of Manchester
--
-- This program is free software: you can redistribute it and/or modify
-- it under the terms of the GNU General Public License as published by
-- the Free Software Foundation, either version 3 of the License, or
-- (at your option) any later version.
--
-- This program is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU General Public License for more details.
--
-- You should have received a copy of the GNU General Public License
-- along with this program.  If not, see <http://www.gnu.org/licenses/>.

ALTER TABLE boards ADD COLUMN physical_serial_id TEXT;
-- Note that the BMP serial ID is per board;
-- it's not in the bmp table because not all BMPs are directly addressible.
ALTER TABLE boards ADD COLUMN bmp_serial_id TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS boardPhysicalSerialSanity ON boards(
	physical_serial_id ASC
);
CREATE UNIQUE INDEX IF NOT EXISTS boardBMPSerialSanity ON boards(
	bmp_serial_id ASC
);
