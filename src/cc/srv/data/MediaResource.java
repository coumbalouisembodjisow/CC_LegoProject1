package cc.srv.data;

import cc.srv.cache.CacheService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths ;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Logger;



@Path("/media")
public class MediaResource {
    private static final Logger logger = Logger.getLogger(MediaResource.class.getName());
    private final String UPLOAD_DIR = "/usr/local/tomcat/uploads";
    private final String BASE_URL = System.getenv().getOrDefault("UPLOAD_BASE_URL", "http://localhost:8080/LegoProject-1.0/media");
    public MediaResource() {
        try {
           java.nio.file.Path  uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            logger.info("MediaResource with Local Storage initialized. Upload dir: " + UPLOAD_DIR);
        } catch (Exception e) {
           logger.severe("Local storage init failed: " + e.getMessage());
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

        // Sauvegarder dans le volume local
        java.nio.file.Path  filePath = Paths.get(UPLOAD_DIR, mediaId);
        Files.copy(new ByteArrayInputStream(contents), filePath, StandardCopyOption.REPLACE_EXISTING);


        logger.info("File saved to local storage: " + mediaId);

        
        return mediaId;
        
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
            

             // Lire depuis le stockage local
            java.nio.file.Path  filePath = Paths.get(UPLOAD_DIR, id);
            if (!Files.exists(filePath)) {
                return Response.status(404).entity("Media not found: " + id).build();
            }

            byte[] contents = Files.readAllBytes(filePath);
            String contentType = determineContentType(id);
            // cache it
            if (cacheEnabled) {
                CacheService.cacheMedia(id, contents, contentType);
                logger.info("Media " + id + " served from local storage and CACHED");
            } else {
                logger.info(" Media " + id + " served from local storage(no cache)");
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
            java.nio.file.Path  uploadPath = Paths.get(UPLOAD_DIR);
            if (Files.exists(uploadPath)) {
                Files.list(uploadPath)
                     .filter(Files::isRegularFile)
                     .forEach(path -> mediaList.add(path.getFileName().toString()));
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
            java.nio.file.Path  uploadPath = Paths.get(UPLOAD_DIR);
            long count = 0;
            if (Files.exists(uploadPath)) {
                count = Files.list(uploadPath)
                            .filter(Files::isRegularFile)
                            .count();
            }
            return "MediaResource with Local Storage! Files count: " + count + " in " + UPLOAD_DIR;
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