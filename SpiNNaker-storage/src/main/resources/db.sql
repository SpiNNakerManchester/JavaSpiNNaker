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

-- A table mapping unique IDs to blobs of data. It's trivial!
CREATE TABLE IF NOT EXISTS storage(
	storage_id INTEGER PRIMARY KEY AUTOINCREMENT,
	content BLOB,
	creation_time INTEGER NOT NULL);

-- A table describing the regions of every core.
CREATE TABLE IF NOT EXISTS dse_regions(
    dse_id INTEGER PRIMARY KEY AUTOINCREMENT,
	global_location_id INTEGER NOT NULL,
	dse_index INTEGER NOT NULL,
    address INTEGER NOT NULL,
    size INTEGER NOT NULL,
	storage_id INTEGER UNIQUE,
    run INTEGER NOT NULL DEFAULT 1,
	FOREIGN KEY(global_location_id) REFERENCES locations(global_location_id),
	FOREIGN KEY(storage_id) REFERENCES storage(storage_id));
-- Every recording region for a vertex has a unique ID
CREATE UNIQUE INDEX IF NOT EXISTS dseSanity ON dse_regions(
	global_location_id, dse_index, run);

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
