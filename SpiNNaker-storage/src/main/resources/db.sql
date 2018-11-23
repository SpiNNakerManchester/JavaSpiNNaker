CREATE TABLE IF NOT EXISTS global_setup(
	current_run INTEGER NOT NULL);
INSERT INTO global_setup(current_run) VALUES (1);


-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
-- A table describing the vertices.
CREATE TABLE IF NOT EXISTS locations(
    global_location_id INTEGER PRIMARY KEY AUTOINCREMENT,
	x INTEGER NOT NULL,
	y INTEGER NOT NULL,
	processor INTEGER NOT NULL,
	vertex_id INTEGER UNIQUE);
-- Every processor has a unique ID
CREATE UNIQUE INDEX IF NOT EXISTS locationSanity ON locations(
	x ASC, y ASC, processor ASC);


-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
-- A table mapping unique IDs to blobs of data. It's trivial!
CREATE TABLE IF NOT EXISTS storage(
	storage_id INTEGER PRIMARY KEY AUTOINCREMENT,
	content BLOB,
	creation_time INTEGER NOT NULL);


-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
-- A table describing the DSE regions of every core.
CREATE TABLE IF NOT EXISTS dse_regions(
    dse_id INTEGER PRIMARY KEY AUTOINCREMENT,
	global_location_id INTEGER NOT NULL,
	dse_index INTEGER NOT NULL,
    address INTEGER NOT NULL,
    size INTEGER NOT NULL,
	storage_id INTEGER UNIQUE,
    run INTEGER NOT NULL DEFAULT 1,
    is_recording_region INTEGER NOT NULL DEFAULT 0,
	FOREIGN KEY(global_location_id) REFERENCES locations(global_location_id),
	FOREIGN KEY(storage_id) REFERENCES storage(storage_id));
-- Every recording region for a vertex has a unique ID
CREATE UNIQUE INDEX IF NOT EXISTS dseSanity ON dse_regions(
	global_location_id, dse_index, run);

CREATE VIEW IF NOT EXISTS dse_view AS
	SELECT x, y, processor, vertex_id, dse_index, address, size, storage_id, run, dse_id
	FROM dse_regions NATURAL JOIN locations;


-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
-- A table describing the recording regions of every core. All the recording
-- regions on a core should be associated with a single DSE region on that
-- core (C library constraint).
CREATE TABLE IF NOT EXISTS recording_regions(
	recording_region_id INTEGER PRIMARY KEY AUTOINCREMENT,
	dse_id INTEGER NOT NULL,
	local_region_id INTEGER NOT NULL,
	storage_id INTEGER UNIQUE,
	fetches INTEGER DEFAULT 0,
	FOREIGN KEY(dse_id) REFERENCES dse_regions(dse_id),
	FOREIGN KEY(storage_id) REFERENCES storage(storage_id));
-- Every recording region for a vertex has a unique ID
CREATE UNIQUE INDEX IF NOT EXISTS recordingSanity ON recording_regions(
	dse_id, local_region_id);

CREATE VIEW IF NOT EXISTS recording_view AS
	SELECT x, y, processor, vertex_id, dse_index, local_region_id,
		recording_regions.storage_id AS storage_id, run, fetches
	FROM recording_regions JOIN dse_regions USING (dse_id)
		NATURAL JOIN locations;


-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
-- Deleting a DSE region deletes its associated recording regions and storage
CREATE TRIGGER IF NOT EXISTS dseStorageDeletion AFTER DELETE ON dse_regions
	BEGIN
		DELETE FROM storage
			WHERE OLD.storage_id IS NOT NULL AND storage_id = OLD.storage_id;
		DELETE FROM recording_regions
			WHERE dse_id = OLD.dse_id;
	END;

-- Setting the storage for a DSE region will delete its old storage
CREATE TRIGGER IF NOT EXISTS dseStorageUpdate AFTER UPDATE ON dse_regions
	WHEN OLD.storage_id IS NOT NULL
	BEGIN
		DELETE FROM storage
			WHERE storage_id = OLD.storage_id;
	END;

-- Deleting a recording region deletes its associated storage
CREATE TRIGGER IF NOT EXISTS recordingStorageDeletion AFTER DELETE ON recording_regions
	WHEN OLD.storage_id IS NOT NULL
	BEGIN
		DELETE FROM storage
			WHERE storage_id = OLD.storage_id;
	END;

-- Deleting a location deletes its DSE regions
CREATE TRIGGER IF NOT EXISTS locationDeletion AFTER DELETE ON locations
	BEGIN
		DELETE FROM dse_regions
			WHERE global_location_id = OLD.global_location_id;
	END;
