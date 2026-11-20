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
            byte[] bytes = java.util.Base64.getDecoder().decode(
                    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/yV0AAAAASUVORK5CYII=");
            id = storage.store(new ByteArrayInputStream(bytes), "connectivity-check.png", "image/png",
                    UUID.randomUUID(), UUID.randomUUID());
            try (var stream = storage.open(id)) { assertArrayEquals(bytes, stream.readAllBytes()); }
        } finally {
            try { if (id != null) storage.delete(id); } finally { storage.close(); }
        }
    }
}
