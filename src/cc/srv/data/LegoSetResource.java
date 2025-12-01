package cc.srv.data;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import cc.srv.db.MongoDBLayer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;
import java.util.UUID;
import cc.srv.cache.CacheService;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;


@Path("/legoset")
public class LegoSetResource {
    private MongoDBLayer dbLayer = MongoDBLayer.getInstance();


    
    // create  a new LegoSet
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createLegoSet(LegoSet legoSet) {
        try {
                // Generate an ID if not already provided 
            if (legoSet.getId() == null || legoSet.getId().trim().isEmpty()) {
                legoSet.setId(UUID.randomUUID().toString());  // Generate a new ID for the LegoSet
            }
            if (legoSet.getName() == null || legoSet.getName().trim().isEmpty()) {
                return Response.status(400).entity("LegoSet name is required").build();
            }
          
            // check for at least one photo
            if (legoSet.getPhotoMediaIds() == null || legoSet.getPhotoMediaIds().isEmpty()) {
                return Response.status(400).entity("At least one photo is required").build();
            }

            // check for existing LegoSet with same ID
            LegoSet existing = dbLayer.getLegoSetById(legoSet.getId());
            if (existing != null) {
                return Response.status(409).entity("LegoSet already exists with ID: " + legoSet.getId()).build();
            }

            dbLayer.putLegoSet(legoSet);
            // cache the new LegoSet
            boolean cacheEnabled = Boolean.parseBoolean(System.getenv("CACHE_ENABLED"));
            if (cacheEnabled) {
                CacheService.cacheLegoSet(legoSet);
                // invalidate most recent added LegoSets cache
                CacheService.invalidateRecentLegoSets();
                System.out.println("LegoSet " + legoSet.getId() + " CACHED after creation");
            }
            return Response.status(201).entity(legoSet).build();
            
        } catch (Exception e) {
            return Response.status(500).entity("Error creating LegoSet: " + e.getMessage()).build();
        }
    }

    
    // Get all LegoSets
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listLegoSets() {
        try {
            Iterator<LegoSet> iterator = dbLayer.getLegoSets().iterator();
            List<LegoSet> legoSetList = new ArrayList<>();
            
            while (iterator.hasNext()) {
                legoSetList.add(iterator.next());
            }

            return Response.ok(legoSetList).build();
        } catch (Exception e) {
            return Response.status(500).entity("Error retrieving LegoSets: " + e.getMessage()).build();
        }
    }

    // Get specific LegoSet by ID
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLegoSet(@PathParam("id") String id) {
        try {
            boolean cacheEnabled = Boolean.parseBoolean(System.getenv("CACHE_ENABLED"));
            if (cacheEnabled) {
                // try to get from cache first
                LegoSet cachedLegoSet = CacheService.getCachedLegoSet(id);
                if (cachedLegoSet != null) {
                    System.out.println("LegoSet " + id + " served from CACHE");
                    return Response.ok(cachedLegoSet).build();}}
                   
            LegoSet legoSet = dbLayer.getLegoSetById(id);

            if (legoSet != null) {
                // cache it
                if (cacheEnabled) {
                    CacheService.cacheLegoSet(legoSet);
                    System.out.println("LegoSet " + id + " CACHED after retrieval from DB");
                }
                return Response.ok(legoSet).build();
            } else {
                return Response.status(404).entity("LegoSet not found with ID: " + id).build();
            }
        } catch (Exception e) {
            return Response.status(500).entity("Error retrieving LegoSet: " + e.getMessage()).build();
        }
    }



    // update an existing LegoSet

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateLegoSet(@PathParam("id") String id, LegoSet legoSet) {
        try {

            if (!id.equals(legoSet.getId())) {
                return Response.status(400).entity("ID in URL does not match ID in body").build();
            }

            if (legoSet.getPhotoMediaIds() == null || legoSet.getPhotoMediaIds().isEmpty()) {
                return Response.status(400).entity("At least one photo is required").build();
            }

            dbLayer.updateLegoSet(legoSet);
            // update cache
            boolean cacheEnabled = Boolean.parseBoolean(System.getenv("CACHE_ENABLED"));
            if (cacheEnabled) {
                CacheService.cacheLegoSet(legoSet);
                // invalidate most recent added LegoSets cache
                CacheService.invalidateRecentLegoSets();
                System.out.println("LegoSet " + legoSet.getId() + " CACHED after update");
            }
            return Response.ok(legoSet).build();
            
        } catch (Exception e) {
            return Response.status(500).entity("Error updating LegoSet: " + e.getMessage()).build();
        }
    }

    // delete a LegoSet by ID

    @DELETE
    @Path("/{id}")
    public Response deleteLegoSet(@PathParam("id") String id) {
        try {
            dbLayer.delLegoSetById(id);
            return Response.status(204).build(); 
        } catch (Exception e) {
            return Response.status(500).entity("Error deleting LegoSet: " + e.getMessage()).build();
        }
    }

    // count total LegoSets
    @GET
    @Path("/count")
    @Produces(MediaType.TEXT_PLAIN)
    public String countLegoSets() {
        try {
            long count = dbLayer.countLegoSets();
            return "Total LegoSets: " + count;
        } catch (Exception e) {
            return "Error counting LegoSets: " + e.getMessage();
        }
    }

    // --------------------- Comments Endpoints ------------------- //

    @POST
    @Path("/{id}/comment")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createComment(@PathParam("id") String legoSetId, Comment comment) {
        try {
            if (comment.getId() == null || comment.getId().trim().isEmpty()) {

                comment.setId(UUID.randomUUID().toString());  // Generate a new ID for the Comment
            }
            if (comment.getUserId() == null || comment.getUserId().trim().isEmpty()) {
                return Response.status(400).entity("User ID is required").build();
            }
            if (comment.getContent() == null || comment.getContent().trim().isEmpty()) {
                return Response.status(400).entity("Comment content is required").build();
            }
            

            // check that the LegoSet exists
           LegoSet legoSet = dbLayer.getLegoSetById(legoSetId);
            if (legoSet == null) {
                return Response.status(404).entity("LegoSet not found with ID: " + legoSetId).build();
            }

            // set the LegoSet ID in the comment
            comment.setLegoSetId(legoSetId);

            // Save the comment
            dbLayer.putComment(comment);
            return Response.status(201).entity(comment).build();

        } catch (Exception e) {
            return Response.status(500).entity("Error creating comment: " + e.getMessage()).build();
        }
    }

// Get comments for a specific LegoSet
@GET
@Path("/{id}/comments")
@Produces(MediaType.APPLICATION_JSON)
public Response getComments(
        @PathParam("id") String legoSetId,
        @QueryParam("st") @DefaultValue("0") int start,
        @QueryParam("len") @DefaultValue("20") int length) {
    try {
        // Check that the LegoSet exists
       LegoSet legoSet = dbLayer.getLegoSetById(legoSetId);
        if (legoSet == null) {
            return Response.status(404).entity("LegoSet not found with ID: " + legoSetId).build();
        }
        Iterator<Comment> commentsIterator = dbLayer.getCommentsByLegoSetId(legoSetId).iterator();
        List<Comment> comments = new ArrayList<>();
        while (commentsIterator.hasNext()) {
            comments.add(commentsIterator.next());
        }

        return Response.ok(comments).build();
    } catch (Exception e) {
        return Response.status(500).entity("Error retrieving comments: " + e.getMessage()).build();
    }
}
   
@GET
@Path("/any/recent")
@Produces(MediaType.APPLICATION_JSON)
public Response getRecentLegoSets(@QueryParam("st") int start, 
                                  @QueryParam("len") int length) {
    try {
        boolean cacheEnabled = Boolean.parseBoolean(System.getenv("CACHE_ENABLED"));
        
        //  limite par défaut si non spécifiée
        int limit = (length > 0) ? length : 20;
        
        // Try to get from cache first (vous devrez peut-être adapter le cache pour gérer les limites)
        if (cacheEnabled) {
            List<LegoSet> cachedRecentSets = CacheService.getCachedRecentLegoSets();
            if (cachedRecentSets != null) {
                System.out.println("Recent LegoSets served from CACHE");
                // Appliquer la limite sur les données en cache
                List<LegoSet> limitedSets = cachedRecentSets.stream()
                    .skip(start)
                    .limit(limit)
                    .collect(Collectors.toList());
                return Response.ok(limitedSets).build();
            }
        }   
        Iterator<LegoSet> recentLegoSets = dbLayer.getRecentLegoSets(start, length);
        
        List<LegoSet> recentSets = new ArrayList<>();
        while (recentLegoSets.hasNext()) {
            recentSets.add(recentLegoSets.next());
        }
        
        // Cache the recent LegoSets (vous pourriez cacher le jeu complet)
        if (cacheEnabled) {
            CacheService.cacheRecentLegoSets(recentSets);
            System.out.println("Recent LegoSets CACHED after retrieval from DB");
        }

        return Response.ok(recentSets).build();
        
    } catch (Exception e) {
        return Response.status(500)
            .entity("Error retrieving recent LegoSets: " + e.getMessage())
            .build();
    }
}
// Get most liked LegoSets
@GET
@Path("/most-liked")
@Produces(MediaType.APPLICATION_JSON)
public Response getMostLikedLegoSets(
    @QueryParam("limit") @DefaultValue("10") int limit) {
    
    try {
        // Récupérer les LegoSets avec les meilleurs scores
        List<LegoSet> results = dbLayer.getMostLikedLegoSets(limit);
        List<LegoSet> mostLiked = new ArrayList<>();
        
        while (results != null ) {
        for (LegoSet legoSet : results) {  
            mostLiked.add(legoSet);
        }
        }
        
        return Response.ok(mostLiked).build();
        
    } catch (Exception e) {
        return Response.status(500).entity("Error retrieving most liked sets: " + e.getMessage()).build();
    }
}

@GET
@Path("/debug/database")
@Produces(MediaType.APPLICATION_JSON)
public Response debugDatabase() {
    try {
        Map<String, Object> debug = new HashMap<>();
        
        // 1. Informations de connexion
        debug.put("MONGODB_URI", System.getenv("MONGODB_URI"));
        debug.put("MONGODB_DB", System.getenv("MONGODB_DB"));
        debug.put("dbLayer_hash", dbLayer.hashCode());
        
        // 2. Comptes depuis MongoDB
        debug.put("legoSets_count_db", dbLayer.countLegoSets());
        debug.put("users_count_db", dbLayer.countUsers());
        
        // 3. Test de récupération spécifique
        LegoSet specificSet = dbLayer.getLegoSetById("3c4e142a-d72e-4eed-8e61-2c97bc0f0b40");
        debug.put("specific_lego_found", specificSet != null);
        if (specificSet != null) {
            debug.put("specific_lego_name", specificSet.getName());
        }
        
        // 4. Liste de tous les LegoSets
        List<String> allLegoIds = new ArrayList<>();
        Iterator<LegoSet> allSets = dbLayer.getLegoSets().iterator();
        int count = 0;
        while (allSets.hasNext()) {
            LegoSet set = allSets.next();
            count++;
            allLegoIds.add(set.getId());
        }
        debug.put("legoSets_from_iterator", count);
        debug.put("legoSets_ids", allLegoIds);
        
        return Response.ok(debug).build();
        
    } catch (Exception e) {
        return Response.status(500).entity("Error: " + e.getMessage()).build();
    }
}
}