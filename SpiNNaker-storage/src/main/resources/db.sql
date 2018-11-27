CREATE TABLE IF NOT EXISTS global_setup(
	current_reset_counter INTEGER NOT NULL,
	current_run_counter INTEGER NOT NULL);
INSERT INTO global_setup(current_reset_counter, current_run_counter) VALUES (0, 0);


-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
-- A table describing the cores.
CREATE TABLE IF NOT EXISTS core(
    core_id INTEGER PRIMARY KEY AUTOINCREMENT,
	x INTEGER NOT NULL,
	y INTEGER NOT NULL,
	processor INTEGER NOT NULL);
-- Every processor has a unique ID
CREATE UNIQUE INDEX IF NOT EXISTS coreSanity ON core(
	x ASC, y ASC, processor ASC);


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
CREATE UNIQUE INDEX IF NOT EXISTS dseSanity on dse(
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
CREATE UNIQUE INDEX IF NOT EXISTS dseStorageSanity on dse_storage(
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
CREATE UNIQUE INDEX IF NOT EXISTS regionSanity on region(
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
CREATE UNIQUE INDEX IF NOT EXISTS regionStorageSanity on region_storage(
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
