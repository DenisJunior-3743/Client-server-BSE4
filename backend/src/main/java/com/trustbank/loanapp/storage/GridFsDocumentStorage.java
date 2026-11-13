package com.trustbank.loanapp.storage;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.gridfs.GridFSUploadStream;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.MongoGridFSException;
import com.trustbank.loanapp.common.ApiException;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** One thread-safe connection pool and bucket per process, with bounded network waits. */
@Service
public class GridFsDocumentStorage implements DocumentStorage {
    private final MongoClient client;
    private final GridFSBucket bucket;

    public GridFsDocumentStorage(@Value("${app.documents.mongodb-uri:}") String uri,
            @Value("${app.documents.database:trustbank_documents}") String database,
            @Value("${app.documents.bucket:loan_documents}") String bucketName) {
        if (uri.isBlank()) {
            client = null;
            bucket = null;
        } else {
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(uri))
                    .applyToClusterSettings(s -> s.serverSelectionTimeout(10, TimeUnit.SECONDS))
                    .applyToSocketSettings(s -> s.connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS))
                    .applyToConnectionPoolSettings(s -> s.maxSize(10).maxWaitTime(3, TimeUnit.SECONDS))
                    .build();
            client = MongoClients.create(settings);
            bucket = GridFSBuckets.create(client.getDatabase(database), bucketName);
        }
    }

    private GridFSBucket bucket() {
        if (bucket == null) throw unavailable();
        return bucket;
    }

    @Override
    public String store(InputStream input, String filename, String contentType, UUID applicationId, UUID borrowerId) {
        GridFSUploadOptions options = new GridFSUploadOptions().metadata(new Document("contentType", contentType)
                .append("applicationId", applicationId.toString()).append("borrowerId", borrowerId.toString()));
        GridFSUploadStream upload;
        try {
            upload = bucket().openUploadStream(filename, options);
        } catch (MongoException ex) {
            throw unavailable();
        }
        try {
            input.transferTo(upload);
            upload.close();
            return upload.getObjectId().toHexString();
        } catch (IOException | MongoException ex) {
            try { upload.abort(); } catch (Exception ignored) { /* best effort chunk cleanup */ }
            throw unavailable();
        }
    }

    @Override
    public InputStream open(String fileId) {
        if (!ObjectId.isValid(fileId)) throw ApiException.notFound("Document file");
        try {
            GridFSDownloadStream stream = bucket().openDownloadStream(new ObjectId(fileId));
            stream.getGridFSFile(); // Resolve missing files before HTTP headers are committed.
            return stream;
        } catch (MongoGridFSException ex) {
            throw ApiException.notFound("Document file");
        } catch (MongoException ex) {
            throw unavailable();
        }
    }

    @Override
    public void delete(String fileId) {
        try { bucket().delete(new ObjectId(fileId)); }
        catch (MongoException ex) { throw unavailable(); }
    }

    private static ApiException unavailable() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "DOCUMENT_STORAGE_UNAVAILABLE",
                "Document storage is unavailable. Please try again shortly.");
    }

    @PreDestroy
    public void close() { if (client != null) client.close(); }
}
