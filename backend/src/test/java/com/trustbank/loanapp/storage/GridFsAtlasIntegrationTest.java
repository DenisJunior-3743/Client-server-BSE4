package com.trustbank.loanapp.storage;

import java.io.ByteArrayInputStream;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in: writes one temporary file to the configured Atlas bucket and deletes it. */
@EnabledIfEnvironmentVariable(named = "RUN_GRIDFS_INTEGRATION_TESTS", matches = "true")
class GridFsAtlasIntegrationTest {
    @Test void atlasRoundTripAndDelete() throws Exception {
        var storage = new GridFsDocumentStorage(System.getenv("MONGODB_URI"),
                System.getenv().getOrDefault("MONGODB_DATABASE", "trustbank_documents"),
                System.getenv().getOrDefault("MONGODB_GRIDFS_BUCKET", "loan_documents"));
        String id = null;
        try {
            byte[] bytes = "%PDF-1.4 TrustBank GridFS connectivity check".getBytes();
            id = storage.store(new ByteArrayInputStream(bytes), "connectivity-check.pdf", "application/pdf",
                    UUID.randomUUID(), UUID.randomUUID());
            try (var stream = storage.open(id)) { assertArrayEquals(bytes, stream.readAllBytes()); }
        } finally {
            try { if (id != null) storage.delete(id); } finally { storage.close(); }
        }
    }
}
