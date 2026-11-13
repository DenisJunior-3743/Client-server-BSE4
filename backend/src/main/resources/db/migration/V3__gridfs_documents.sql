-- Existing metadata-only rows remain visible, but have no downloadable file.
ALTER TABLE documents ADD COLUMN gridfs_file_id VARCHAR(24);
CREATE UNIQUE INDEX ix_documents_gridfs_file ON documents(gridfs_file_id) WHERE gridfs_file_id IS NOT NULL;
