package cc.srv.data;

import cc.srv.cache.CacheService;
import cc.srv.storage.AzureBlobStorage;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.logging.Logger;

/**
 * Resource for managing media files with Azure Blob Storage
 */
@Path("/media")
public class MediaResource {
    private static final Logger logger = Logger.getLogger(MediaResource.class.getName());
    private BlobContainerClient blobContainerClient;

    public MediaResource() {
        try {
            AzureBlobStorage storage = AzureBlobStorage.getInstance();
            this.blobContainerClient = storage.getBlobServiceClient()
                    .getBlobContainerClient(storage.getContainerName());
            logger.info("MediaResource with Azure Blob Storage initialized");
        } catch (Exception e) {
            logger.severe("Azure Blob init failed: " + e.getMessage());
        }
    }

    /**
     * Post a new media file to Azure Blob Storage
     */
@POST
@Path("/")
@Consumes({"image/jpeg", "image/png", "application/octet-stream"}) 
@Produces(MediaType.APPLICATION_JSON)
public String upload(
        @HeaderParam("Content-Type") String contentType, 
        byte[] contents) {
    
    try {
        logger.info("Upload received - ContentType: " + contentType + ", Size: " + contents.length);

        // Générer un ID unique
        String mediaId = UUID.randomUUID().toString();
        
        // Déterminer l'extension basée sur le Content-Type
        String extension = getExtensionFromContentType(contentType);
        mediaId += extension;

        // Upload vers Azure Blob Storage
        BlobClient blobClient = blobContainerClient.getBlobClient(mediaId);
        
        BlobHttpHeaders headers = new BlobHttpHeaders().setContentType(contentType);
        
        blobClient.upload(new ByteArrayInputStream(contents), contents.length, true);
        blobClient.setHttpHeaders(headers);

        logger.info("File uploaded to Azure: " + mediaId);

        // Réponse JSON (format du prof)
        return "{\"id\": \"" + mediaId + "\"}";
        
    } catch (Exception e) {
        logger.severe("Upload error: " + e.getMessage());
        return "{\"error\": \"Upload failed: " + e.getMessage() + "\"}";
    }
}

/**
 * Détermine l'extension basée sur le Content-Type
 */
private String getExtensionFromContentType(String contentType) {
    if (contentType == null) return "";
    
    switch (contentType) {
        case "image/jpeg":
            return ".jpeg";
        case "image/png":
            return ".png";
        case "image/gif":
            return ".gif";
        case "image/jpg":
            return ".jpg";
        default:
            return ".bin";
    }
}
    /**
     * Return the contents of a media file from Azure Blob Storage
     */
    @GET
    @Path("/{id}")
    public Response download(@PathParam("id") String id) {
        try {
            logger.info("Download from Azure: " + id);
            boolean cacheEnabled = Boolean.parseBoolean(System.getenv("CACHE_ENABLED"));
        
        // cache check
        if (cacheEnabled) {
            Map<String, String> cachedMedia = CacheService.getCachedMedia(id);
            if (cachedMedia != null) {
                // get content and contentType from cache
                byte[] content = Base64.getDecoder().decode(cachedMedia.get("content"));
                String contentType = cachedMedia.get("contentType");
                
                return Response.ok(content)
                        .type(contentType)
                        .header("Content-Disposition", "inline; filename=\"" + id + "\"")
                        .build();
            }
        }
            
            BlobClient blobClient = blobContainerClient.getBlobClient(id);
            
            if (!blobClient.exists()) {
                return Response.status(404).entity("Media not found: " + id).build();
            }

            // Lire depuis Azure Blob
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            blobClient.downloadStream(outputStream);
            byte[] contents = outputStream.toByteArray();

            String contentType = blobClient.getProperties().getContentType();
            if (contentType == null) {
                contentType = determineContentType(id);
            }
            // cache it
            if (cacheEnabled) {
                CacheService.cacheMedia(id, contents, contentType);
                logger.info("Media " + id + " served from Azure and CACHED");
            } else {
                logger.info(" Media " + id + " served from Azure (no cache)");
            }
            return Response.ok(contents)
                    .type(contentType)
                    .header("Content-Disposition", "inline; filename=\"" + id + "\"")
                    .build();

        } catch (Exception e) {
            logger.severe("Download error: " + e.getMessage());
            return Response.status(500).entity("Download failed: " + e.getMessage()).build();
        }
    }

    /**
     * Lists the ids of media files stored (version simplifiée)
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> list() {
        // Pour garder la simplicité, retournons une liste vide
        // ou comptons les blobs
        List<String> mediaList = new ArrayList<>();
        try {
            for (com.azure.storage.blob.models.BlobItem blob : blobContainerClient.listBlobs()) {
                mediaList.add(blob.getName());
            }
        } catch (Exception e) {
            logger.warning("Cannot list blobs: " + e.getMessage());
        }
        return mediaList;
    }

    /**
     * Test endpoint
     */
    @GET
    @Path("/test")
    @Produces(MediaType.TEXT_PLAIN)
    public String test() {
        try {
            int count = 0;
            for (com.azure.storage.blob.models.BlobItem blob : blobContainerClient.listBlobs()) {
                count++;
            }
            return "MediaResource with Azure Blob! Blobs count: " + count;
        } catch (Exception e) {
            return "MediaResource test error: " + e.getMessage();
        }
    }

   

    private String determineContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) return "image/jpeg";
        if (lowerName.endsWith(".png")) return "image/png";
        if (lowerName.endsWith(".gif")) return "image/gif";
        if (lowerName.endsWith(".mp4")) return "video/mp4";
        return "application/octet-stream";
    }
}