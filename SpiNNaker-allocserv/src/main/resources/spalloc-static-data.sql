-- Copyright (c) 2021 The University of Manchester
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

-- Supported models of SpiNNaker board
INSERT OR IGNORE INTO board_models(model)
VALUES
	(2), (3), (4), (5);

-- The information about chip configuration of boards
INSERT OR IGNORE INTO board_model_coords(model, chip_x, chip_y)
VALUES
	-- Version 3 boards
	(3, 0, 1), (3, 1, 1),
	(3, 0, 0), (3, 1, 0),
	-- Version 5 boards
	                                            (5, 4, 7), (5, 5, 7), (5, 6, 7), (5, 7, 7),
	                                 (5, 3, 6), (5, 4, 6), (5, 5, 6), (5, 6, 6), (5, 7, 6),
	                      (5, 2, 5), (5, 3, 5), (5, 4, 5), (5, 5, 5), (5, 6, 5), (5, 7, 5),
	           (5, 1, 4), (5, 2, 4), (5, 3, 4), (5, 4, 4), (5, 5, 4), (5, 6, 4), (5, 7, 4),
	(5, 0, 3), (5, 1, 3), (5, 2, 3), (5, 3, 3), (5, 4, 3), (5, 5, 3), (5, 6, 3), (5, 7, 3),
	(5, 0, 2), (5, 1, 2), (5, 2, 2), (5, 3, 2), (5, 4, 2), (5, 5, 2), (5, 6, 2),
	(5, 0, 1), (5, 1, 1), (5, 2, 1), (5, 3, 1), (5, 4, 1), (5, 5, 1),
	(5, 0, 0), (5, 1, 0), (5, 2, 0), (5, 3, 0), (5, 4, 0);

-- Create boards rarely seen in the wild
INSERT OR IGNORE INTO board_model_coords(model, chip_x, chip_y)
	SELECT 2, chip_x, chip_y FROM board_model_coords WHERE model = 3;
INSERT OR IGNORE INTO board_model_coords(model, chip_x, chip_y)
	SELECT 4, chip_x, chip_y FROM board_model_coords WHERE model = 5;

-- Standard directions between boards
INSERT OR IGNORE INTO directions("id", name)
VALUES
	(0, 'N'), (1, 'E'), (2, 'SE'), (3, 'S'), (4, 'W'), (5, 'NW');

INSERT OR IGNORE INTO job_states("id", name)
VALUES
	(0, 'UNKNOWN'), (1, 'QUEUED'), (2, 'POWER'), (3, 'READY'), (4, 'DESTROYED');

INSERT OR IGNORE INTO group_types("id", name)
VALUES
	(0, 'INTERNAL'), (1, 'ORGANISATION'), (2, 'COLLABRATORY');

INSERT OR IGNORE INTO movement_directions(z, direction, dx, dy, dz)
VALUES
	-- Z = 0
	(0, 0, 0, 0, +2),
	(0, 1, 0, 0, +1),
	(0, 2, 0, -1, +2),
	(0, 3, -1, -1, +1),
	(0, 4, -1, -1, +2),
	(0, 5, -1, 0, +1),
	-- Z = 1
	(1, 0, +1, +1, -1),
	(1, 1, +1, 0, +1),
	(1, 2, +1, 0, -1),
	(1, 3, 0, -1, +1),
	(1, 4, 0, 0, -1),
	(1, 5, 0, 0, +1),
	-- Z = 2
	(2, 0, 0, +1, -1),
	(2, 1, +1, +1, -2),
	(2, 2, 0, 0, -1),
	(2, 3, 0, 0, -2),
	(2, 4, -1, 0, -1),
	(2, 5, 0, +1, -2);

-- Lock down the board_models table
CREATE TRIGGER IF NOT EXISTS "board_model_support_is_static no_update"
BEFORE UPDATE ON board_models
BEGIN
    SELECT RAISE(IGNORE);
END;

CREATE TRIGGER IF NOT EXISTS "board_model_support_is_static no_insert"
BEFORE INSERT ON board_models
BEGIN
    SELECT RAISE(IGNORE);
END;

CREATE TRIGGER IF NOT EXISTS "board_model_support_is_static no_delete"
BEFORE DELETE ON board_models
BEGIN
    SELECT RAISE(IGNORE);
END;

-- Lock down the board_model_coords table
CREATE TRIGGER IF NOT EXISTS "board_layout_is_static no_update"
BEFORE UPDATE ON board_model_coords
BEGIN
    SELECT RAISE(IGNORE);
END;

CREATE TRIGGER IF NOT EXISTS "board_layout_is_static no_insert"
BEFORE INSERT ON board_model_coords
BEGIN
    SELECT RAISE(IGNORE);
END;

CREATE TRIGGER IF NOT EXISTS "board_layout_is_static no_delete"
BEFORE DELETE ON board_model_coords
BEGIN
    SELECT RAISE(IGNORE);
END;

-- Lock down the movement directions
CREATE TRIGGER IF NOT EXISTS "directions_is_static no_update"
BEFORE UPDATE ON directions
BEGIN
	SELECT RAISE(IGNORE);
END;

CREATE TRIGGER IF NOT EXISTS "directions_is_static no_insert"
BEFORE INSERT ON directions
BEGIN
	SELECT RAISE(IGNORE);
END;

CREATE TRIGGER IF NOT EXISTS "directions_is_static no_delete"
BEFORE DELETE ON directions
BEGIN
	SELECT RAISE(IGNORE);
END;

-- Lock down the job_states
CREATE TRIGGER IF NOT EXISTS "job_states_is_static no_update"
BEFORE UPDATE ON job_states
BEGIN
	SELECT RAISE(IGNORE);
END;

CREATE TRIGGER IF NOT EXISTS "job_states_is_static no_insert"
BEFORE INSERT ON job_states
BEGIN
	SELECT RAISE(IGNORE);
END;

CREATE TRIGGER IF NOT EXISTS "job_states_is_static no_delete"
BEFORE DELETE ON job_states
BEGIN
	SELECT RAISE(IGNORE);
END;

-- Lock down the group_types
CREATE TRIGGER IF NOT EXISTS "group_types_is_static no_update"
BEFORE UPDATE ON group_types
BEGIN
	SELECT RAISE(IGNORE);
END;

CREATE TRIGGER IF NOT EXISTS "group_types_is_static no_insert"
BEFORE INSERT ON group_types
BEGIN
	SELECT RAISE(IGNORE);
END;

CREATE TRIGGER IF NOT EXISTS "group_types_is_static no_delete"
BEFORE DELETE ON group_types
BEGIN
	SELECT RAISE(IGNORE);
END;

-- Lock down the inter-board movement patterns
CREATE TRIGGER IF NOT EXISTS "movement_directions_is_static no_update"
BEFORE UPDATE ON movement_directions
BEGIN
	SELECT RAISE(IGNORE);
END;

CREATE TRIGGER IF NOT EXISTS "movement_directions_is_static no_insert"
BEFORE INSERT ON movement_directions
BEGIN
	SELECT RAISE(IGNORE);
END;

CREATE TRIGGER IF NOT EXISTS "movement_directions_is_static no_delete"
BEFORE DELETE ON movement_directions
BEGIN
	SELECT RAISE(IGNORE);
END;
