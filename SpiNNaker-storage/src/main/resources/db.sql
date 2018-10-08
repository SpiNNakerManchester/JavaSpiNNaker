-- A table mapping unique names to blobs of data. It's trivial!
CREATE TABLE IF NOT EXISTS storage(
	storage_id INTEGER PRIMARY KEY AUTOINCREMENT,
	x INTEGER NOT NULL,
	y INTEGER NOT NULL,
	processor INTEGER NOT NULL,
	region INTEGER NOT NULL,
	content BLOB);
