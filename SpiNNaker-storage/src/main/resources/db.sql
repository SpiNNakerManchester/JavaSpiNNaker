-- Copyright (c) 2018 The University of Manchester
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

-- "Global variables"; this table should only ever have one row
CREATE TABLE IF NOT EXISTS last_run(
	reset_counter INTEGER NOT NULL,
	run_counter INTEGER NOT NULL);
INSERT INTO last_run(reset_counter, run_counter)
	SELECT 1, 1
	WHERE NOT EXISTS (SELECT 1 FROM last_run);


-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
-- A table describing the cores.
CREATE TABLE IF NOT EXISTS core(
    core_id INTEGER PRIMARY KEY AUTOINCREMENT,
	x INTEGER NOT NULL,
	y INTEGER NOT NULL,
	processor INTEGER NOT NULL,
	ethernet_core_id INTEGER,
	FOREIGN KEY(ethernet_core_id) REFERENCES core(core_id));
-- Every processor has a unique ID
CREATE UNIQUE INDEX IF NOT EXISTS coreSanity ON core(
	x ASC, y ASC, processor ASC);


-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
-- A table describing (DSE?) writes...
CREATE TABLE IF NOT EXISTS write(
	write_id INTEGER PRIMARY KEY AUTOINCREMENT,
	core_id INTEGER NOT NULL,
	file TEXT NOT NULL,
	start_address INTEGER NOT NULL,
	memory_used INTEGER NOT NULL,
	memory_written INTEGER NOT NULL,
	FOREIGN KEY(core_id) REFERENCES core(core_id));
-- No candidate key yet

CREATE VIEW IF NOT EXISTS write_view AS
	SELECT x, y, processor, write_id, core_id, file, start_address,
		memory_used, memory_written
	FROM write NATURAL JOIN core;


-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
-- A table describing DSE regions.
CREATE TABLE IF NOT EXISTS dse(
	dse_id INTEGER PRIMARY KEY AUTOINCREMENT,
	core_id INTEGER NOT NULL,
	dse_index INTEGER NOT NULL,
	address INTEGER NOT NULL,
	size INTEGER NOT NULL,
	FOREIGN KEY(core_id) REFERENCES core(core_id));
-- Every DSE region has a unique ID
CREATE UNIQUE INDEX IF NOT EXISTS dseSanity ON dse(
	core_id ASC, dse_index ASC);

CREATE VIEW IF NOT EXISTS dse_view AS
	SELECT x, y, processor, dse_index, address, size, core_id, dse_id
	FROM dse NATURAL JOIN core;


-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
-- A table holding storage for DSE regions.
CREATE TABLE IF NOT EXISTS dse_storage(
	dse_storage_id INTEGER PRIMARY KEY AUTOINCREMENT,
	dse_id INTEGER NOT NULL,
	reset_counter INTEGER NOT NULL,
	run_counter INTEGER NOT NULL,
	content BLOB,
	creation_time INTEGER NOT NULL,
	FOREIGN KEY(dse_id) REFERENCES dse(dse_id));
-- Every DSE region storage has a unique DSE region and execution phase
CREATE UNIQUE INDEX IF NOT EXISTS dseStorageSanity ON dse_storage(
	dse_id ASC, reset_counter ASC, run_counter ASC);

CREATE VIEW IF NOT EXISTS dse_storage_view AS
	SELECT x, y, processor, dse_index, address, size, reset_counter,
		run_counter, content, creation_time, core_id, dse_storage_id
	FROM core NATURAL JOIN dse NATURAL JOIN dse_storage;


-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
-- A table holding descriptions of vertices.
CREATE TABLE IF NOT EXISTS vertex(
	vertex_id INTEGER PRIMARY KEY AUTOINCREMENT,
	meta_data_id INTEGER UNIQUE NOT NULL,
	label TEXT,
	FOREIGN KEY(meta_data_id) REFERENCES dse(dse_id));

CREATE VIEW IF NOT EXISTS vertex_view AS
	SELECT x, y, processor, dse_index, address AS meta_data_address,
		size AS meta_data_size, label, core_id, meta_data_id, vertex_id
	FROM dse NATURAL JOIN core JOIN vertex ON (vertex.meta_data_id = dse.dse_id);


-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
-- A table describing recording regions.
CREATE TABLE IF NOT EXISTS region(
	region_id INTEGER PRIMARY KEY AUTOINCREMENT,
	vertex_id INTEGER NOT NULL,
	local_region_index INTEGER NOT NULL,
	address INTEGER NOT NULL,
	FOREIGN KEY(vertex_id) REFERENCES vertex(vertex_id));
-- Every recording region has a unique vertex and index
CREATE UNIQUE INDEX IF NOT EXISTS regionSanity ON region(
	vertex_id ASC, local_region_index ASC);

CREATE VIEW IF NOT EXISTS region_view AS
	SELECT x, y, processor, dse_index, meta_data_address, meta_data_size,
		label, core_id, meta_data_id, local_region_index,
		region.address AS recording_address, region_id
	FROM vertex_view NATURAL JOIN region;


-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
-- A table holding storage for recording regions.
CREATE TABLE IF NOT EXISTS region_storage(
	region_storage_id INTEGER PRIMARY KEY AUTOINCREMENT,
	region_id INTEGER NOT NULL,
	reset_counter INTEGER NOT NULL,
	content BLOB NOT NULL DEFAULT X'',
	append_time INTEGER,
	fetches INTEGER NOT NULL DEFAULT 0,
	FOREIGN KEY(region_id) REFERENCES region(region_id));
-- Every recording region storage has a unique recording region and execution phase
CREATE UNIQUE INDEX IF NOT EXISTS regionStorageSanity ON region_storage(
	region_id ASC, reset_counter ASC);

CREATE VIEW IF NOT EXISTS region_storage_view AS
	SELECT x, y, processor, dse_index, meta_data_address, meta_data_size,
		label, core_id, meta_data_id, local_region_index, recording_address,
		region_id, region_storage_id, content, append_time, reset_counter,
		fetches
	FROM region_view NATURAL JOIN region_storage;


-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
-- Deleting things will cascade
CREATE TRIGGER IF NOT EXISTS coreDeletion AFTER DELETE ON core
	BEGIN
		DELETE FROM dse
			WHERE core_id = OLD.core_id;
	END;

CREATE TRIGGER IF NOT EXISTS dseStorageDeletion AFTER DELETE ON dse
	BEGIN
		DELETE FROM dse_storage
			WHERE dse_id = OLD.dse_id;
		DELETE FROM vertex
			WHERE meta_data_id = OLD.dse_id;
	END;

CREATE TRIGGER IF NOT EXISTS vertexDeletion AFTER DELETE ON vertex
	BEGIN
		DELETE FROM region
			WHERE vertex_id = OLD.vertex_id;
	END;

CREATE TRIGGER IF NOT EXISTS regionDeletion AFTER DELETE ON region
	BEGIN
		DELETE FROM region_storage
			WHERE region_id = OLD.region_id;
	END;
