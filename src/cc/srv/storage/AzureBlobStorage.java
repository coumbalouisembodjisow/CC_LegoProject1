package cc.srv.storage;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import java.util.logging.Logger;

/**
 * Gestionnaire de connexion Azure Blob Storage
 */
public class AzureBlobStorage {
    private static final Logger logger = Logger.getLogger(AzureBlobStorage.class.getName());
    private static AzureBlobStorage instance;
    private BlobServiceClient blobServiceClient;
    private String containerName = "media";

    private AzureBlobStorage() {
        initializeBlobStorage();
    }

    public static synchronized AzureBlobStorage getInstance() {
        if (instance == null) {
            instance = new AzureBlobStorage();
        }
        return instance;
    }

    private void initializeBlobStorage() {
        try {
            String blobConnectionString = System.getenv("blobConnectionString");
            
            this.blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(blobConnectionString)
                    .buildClient();

            logger.info("Azure Blob Storage initialized successfully");

        } catch (Exception e) {
            logger.severe("Failed to initialize Azure Blob Storage: " + e.getMessage());
            throw new RuntimeException("Azure Blob Storage initialization failed", e);
        }
    }

    public BlobServiceClient getBlobServiceClient() {
        return blobServiceClient;
    }

    public String getContainerName() {
        return containerName;
    }
}