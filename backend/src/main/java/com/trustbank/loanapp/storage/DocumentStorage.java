package com.trustbank.loanapp.storage;

import java.io.InputStream;
import java.util.UUID;

public interface DocumentStorage {
    String store(InputStream input, String filename, String contentType, UUID applicationId, UUID borrowerId);
    InputStream open(String fileId);
    void delete(String fileId);
}
