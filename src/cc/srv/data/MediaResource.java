package cc.srv.data;

import cc.srv.storage.AzureBlobStorage;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public String upload(
            @HeaderParam("Content-Type") String contentType, 
            @HeaderParam("X-File-Name") String fileName,
            byte[] contents) {
        
        try {
            logger.info("Upload to Azure - Size: " + contents.length);
            
            // Générer un ID unique
            String mediaId = UUID.randomUUID().toString() + getFileExtension(fileName);

            // Upload vers Azure Blob Storage
            BlobClient blobClient = blobContainerClient.getBlobClient(mediaId);
            
            String actualContentType = (contentType != null) ? contentType : determineContentType(fileName);
            BlobHttpHeaders headers = new BlobHttpHeaders().setContentType(actualContentType);
            
            blobClient.upload(new java.io.ByteArrayInputStream(contents), contents.length, true);
            blobClient.setHttpHeaders(headers);

            // Réponse JSON simple (identique à votre version)
            return "{\"id\": \"" + mediaId + "\", " +
                   "\"fileName\": \"" + (fileName != null ? fileName : "unknown") + "\", " +
                   "\"fileSize\": " + contents.length + ", " +
                   "\"contentType\": \"" + actualContentType + "\"}";
            
        } catch (Exception e) {
            logger.severe("Upload error: " + e.getMessage());
            return "{\"error\": \"Upload failed: " + e.getMessage() + "\"}";
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

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
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