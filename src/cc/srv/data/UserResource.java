package cc.srv.data;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import cc.srv.cache.CacheService;
import cc.srv.db.MongoDBLayer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

/**
 * Class with control endpoints.
 */
@Path("/user")
public class UserResource
{
	private MongoDBLayer dbLayer = MongoDBLayer.getInstance();
     public UserResource() {
        // ensure Deleted User exists
        createDeletedUserIfNeeded();
    }
	/**
	 * This methods just prints a string to test our endpoind is working.
	 */
	@Path("/test")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String hello() {
		return "UserController is running.";
	}

@GET
@Path("/db-test")
@Produces(MediaType.TEXT_PLAIN)
public String testDatabase() {
    try {
        
        String connectionStatus = dbLayer.testConnection();
        long userCount = dbLayer.countUsers();
        return connectionStatus + " | Users in database: " + userCount;
    } catch (Exception e) {
        return "Database test FAILED: " + e.getMessage();
    }
}

 /**
     * GET /user - Get all users
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllUsers() {
        
        try {
            Iterator<User> usersIterator = dbLayer.getUsers().iterator();
            List<User> userList = new ArrayList<>();
            
            while (usersIterator.hasNext()) {
                User user = usersIterator.next();
                userList.add(user);
            }

            if (!userList.isEmpty()) {
                return Response.ok(userList).build();
            } else {
                return Response.status(404).entity("No users found").build();
            }
        } catch (Exception e) {
              e.printStackTrace(); 
            return Response.status(500).entity("Error retrieving users: " + e.getMessage()).build();
        }
    }

    /**
     * GET /user/{id} - Get user by ID
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserById(@PathParam("id") String id) {
        
        try {   
        // // try to get user from cache
        boolean cacheEnabled = Boolean.parseBoolean(System.getenv("CACHE_ENABLED"));
        if (cacheEnabled) {
            User cachedUser = CacheService.getCachedUser(id);
            if (cachedUser != null) {
                System.out.println("User " + id + " served from CACHE");
                return Response.ok(cachedUser).build();
            }
        }
            // if not in cache, get from database
           User user = dbLayer.getUserById(id);

            if (user != null) {
                
                // put in cache 
                if (cacheEnabled) {
                CacheService.cacheUser(user);
                System.out.println("User " + id + " served from DB and CACHED");
            } else {
                System.out.println("User " + id + " served from DB (no cache)");
            }
                return Response.ok(user).build();
            } else {
                return Response.status(404).entity("User not found with ID: " + id).build();
            }
      
            }
        catch (Exception e) {
            return Response.status(500).entity("Error retrieving user: " + e.getMessage()).build();
        }
    }


  /**
 * POST /user - Create a new user
 */
@POST
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public Response createUser(Map<String, Object> userData) {
    long startTime = System.currentTimeMillis();
    String userId = null;
    
    try {
       
        if (userData == null || userData.isEmpty()) {
            return Response.status(400).entity("{\"error\":\"User data required\"}").build();
        }
        
        userId = (String) userData.get("id");
        String name = (String) userData.get("name");
        String password = (String) userData.get("password");
        String photoMediaId = (String) userData.get("photoMediaId");
        
        if (userId == null || userId.trim().isEmpty()) {
            return Response.status(400).entity("{\"error\":\"User ID is required\"}").build();
        }
        if (name == null || name.trim().isEmpty()) {
            return Response.status(400).entity("{\"error\":\"User name is required\"}").build();
        }
        User  existingUser = dbLayer.getUserById(userId);
        if (existingUser != null) {
            System.out.println(" User already exists: " + userId);
            return Response.status(409).entity("{\"error\":\"User already exists\"}").build();
        }
        
        User user = new User();
        user.setId(userId);
        user.setName(name);
        user.setPassword(password != null ? password : "");
        user.setPhotoMediaId(photoMediaId);
        user.setOwnedLegoSets(new HashSet<>()); 
        
       
        dbLayer.putUser(user);
        
       //  CACHE ASYNCHRONE
        final boolean cacheEnabled = Boolean.parseBoolean(System.getenv("CACHE_ENABLED"));
        if (cacheEnabled) {
            final User finalUser = user; 
            final String finalUserId = userId;
            CompletableFuture.runAsync(() -> {
                try {
                    CacheService.cacheUser(finalUser);
                    System.out.println(" User cached asynchronously: " + finalUserId);
                } catch (Exception e) {
                    System.err.println(" Async cache error for user " + finalUserId + ": " + e.getMessage());
                }
            });
        }
        
        long duration = System.currentTimeMillis() - startTime;
        System.out.println(" User created successfully: " + userId + " (" + duration + "ms)");
        
        return Response.status(201).entity(user).build();
        
    } catch (ClassCastException e) {
        System.err.println(" Invalid data format for user " + userId + ": " + e.getMessage());
        return Response.status(400).entity("{\"error\":\"Invalid data format\"}").build();
        
    } catch (Exception e) {
        System.err.println(" Server error creating user " + userId + ": " + e.getMessage());
        e.printStackTrace(); 
        return Response.status(500).entity("{\"error\":\"Server error: " + e.getMessage() + "\"}").build();
    }
}

    /**
     * PUT /user/{id} - Update user by ID
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUser(@PathParam("id") String id, User user) {
        try {
            // Ensure the ID in path matches the user object
            user.setId(id);
            
            // Update user in database
            dbLayer.updateUser(user);
            // update cache
            boolean cacheEnabled = Boolean.parseBoolean(System.getenv("CACHE_ENABLED"));
            if (cacheEnabled) {
                CacheService.invalidateUser(id); // Invalider l'ancienne version
                CacheService.cacheUser(user);    // Recacher la nouvelle version
                System.out.println("User " + id + " cache UPDATED after modification");
            }
            return Response.ok(user).build();
            
        } catch (Exception e) {
            return Response.status(500).entity("Error updating user: " + e.getMessage()).build();
        }
    }
   /**
 * DELETE /user/{id} - Delete user and transfer auctions/comments to Deleted User
 */
@DELETE
@Path("/{id}")
@Produces(MediaType.APPLICATION_JSON)
public Response deleteUser(@PathParam("id") String id) {
    try {
        // check that the user exists
        User  user = dbLayer.getUserById(id);
        if (user != null) {
            return Response.status(404).entity("User not found with ID: " + id).build();
        }
        
        // check and create Deleted User if needed
        createDeletedUserIfNeeded();
        
        // transfer auctions and comments to Deleted User
        List<Auction> userAuctions = dbLayer.getAuctionsByUser(id);
        for (Auction auction : userAuctions) {
            auction.setSellerId("deleted-user");  // ← change sellerId
            dbLayer.updateAuction(auction);
        }
          
        List<Comment> userComments = dbLayer.getCommentsByUser(id);
        for (Comment comment : userComments) {
            comment.setUserId("deleted-user");    // ← change userId
            dbLayer.updateComment(comment);
        }
        
        // delete the user
        dbLayer.delUserById(id);
        // invalidate cache
        boolean cacheEnabled = Boolean.parseBoolean(System.getenv("CACHE_ENABLED"));
        if (cacheEnabled) {
            CacheService.invalidateUser(id);
            System.out.println("User " + id + " cache INVALIDATED after deletion");
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User deleted successfully");
        response.put("deletedUserId", id);
        //response.put("auctionsTransferred", userAuctions.size());
        //response.put("commentsTransferred", userComments.size());
        
        Logger.getLogger(UserResource.class.getName()).info("User " + id + " deleted. Auctions transferred: " + userAuctions.size() + ", Comments transferred: " + userComments.size());
        
        return Response.ok(response).build();
        
    } catch (Exception e) {
       Logger.getLogger(UserResource.class.getName()).severe("Error deleting user: " + e.getMessage());
        return Response.status(500).entity("Error deleting user: " + e.getMessage()).build();
    }
}

    /**
 * Create a "deleted-user" entry if it does not already exist.
 */
private void createDeletedUserIfNeeded() {
    try {
        
        User  deletedUserIterator = dbLayer.getUserById("deleted-user") ;
        
        if (deletedUserIterator != null) {
            // create the Deleted User
            User deletedUser = new User();
            deletedUser.setId("deleted-user");
            deletedUser.setName("Deleted User");
            deletedUser.setPassword(""); // Pas de mot de passe
            
            // Sauvegarder dans la base
            dbLayer.putUser(deletedUser);
            Logger.getLogger(UserResource.class.getName()).info("Deleted User created in database.");
        }
    } catch (Exception e) {
        Logger.getLogger(UserResource.class.getName()).severe("Error creating Deleted User: " + e.getMessage());
    }
}
/**
 * GET /rest/user/{userId}/legosets - Get all LegoSets owned by a specific user
 */
@GET
@Path("/{id}/legosets")
@Produces(MediaType.APPLICATION_JSON)
public Response getUserLegoSets(@PathParam("id") String userId) {
    try {
        // try to get from cache first
        boolean cacheEnabled = Boolean.parseBoolean(System.getenv("CACHE_ENABLED"));
        if (cacheEnabled) {
            List<LegoSet> cachedLegoSets = CacheService.getCachedUserLegoSets(userId);
            if (cachedLegoSets != null) {
                System.out.println("User LegoSets " + userId + " served from CACHE");
                return Response.ok(cachedLegoSets).build();
            }
        }
        // if not in cache, get from database
        User user = dbLayer.getUserById(userId);
        if (user != null) {
            return Response.status(404).entity("User not found with ID: " + userId).build();
        }        
        // get list of owned LegoSet IDs
        Set<String> ownedLegoSetIds = user.getOwnedLegoSets();
        
        if (ownedLegoSetIds == null || ownedLegoSetIds.isEmpty()) {
            return Response.status(404).entity("No LegoSets found for user: " + userId).build();
        }

        // Récupérer les détails de chaque LegoSet
        List<LegoSet> userLegoSets = new ArrayList<>();
        for (String legoSetId : ownedLegoSetIds) {
            LegoSet legoSet = dbLayer.getLegoSetById(legoSetId);
            if (legoSet != null) {
                userLegoSets.add(legoSet);
            }
        }
        // cache the result
        if (cacheEnabled) {
            CacheService.cacheUserLegoSets(userId, userLegoSets);
            System.out.println("User LegoSets " + userId + " served from DB and CACHED");
        } else {
            System.out.println("User LegoSets " + userId + " served from DB (no cache)");
        }
        return Response.ok(userLegoSets).build();
        
    } catch (Exception e) {
        return Response.status(500).entity("Error retrieving user LegoSets: " + e.getMessage()).build();
    }
}

/**
 * POST /rest/user/{id}/legosets/{legoSetId} - Add a LegoSet to user's collection
 */
@POST
@Path("/{id}/legosets/{legoSetId}")  
@Produces(MediaType.APPLICATION_JSON)
public Response addLegoSetToUser(
    @PathParam("id") final String userId, 
    @PathParam("legoSetId") final String legoSetId) {  
    
    long startTime = System.currentTimeMillis();
    final boolean cacheEnabled = Boolean.parseBoolean(System.getenv("CACHE_ENABLED"));
    
    try {
        System.out.println(" Adding LegoSet " + legoSetId + " to user " + userId);
        
        //   le cache d'abord
        User user = null;
        if (cacheEnabled) {
            user = CacheService.getCachedUser(userId);
        }
        
        //  Récupérer depuis DB si pas en cache
        if (user == null) {
            User userIterator = dbLayer.getUserById(userId);
            if (userIterator== null) {
                System.out.println(" User not found: " + userId);
                return Response.status(404).entity("User not found").build();
            }
            user = userIterator;
        }
        
        // si le LegoSet existe déjà (O(1) avec Set)
        if (user.getOwnedLegoSets() != null && user.getOwnedLegoSets().contains(legoSetId)) {
            System.out.println(" LegoSet already in collection: " + legoSetId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "LegoSet already in collection");
            response.put("userId", userId);
            response.put("legoSetId", legoSetId);
            response.put("optimized", "true");
            return Response.ok(response).build();
        }
        
        //  Ajouter le LegoSet
        user.addOwnedLegoSet(legoSetId);
        
        // Update ASYNCHRONE de la DB
        final User finalUser = user;
        
        CompletableFuture.runAsync(() -> {
            try {
                dbLayer.updateUser(finalUser);
                System.out.println(" DB updated for user: " + userId);
            } catch (Exception e) {
                System.err.println(" DB update error for user " + userId + ": " + e.getMessage());
            }
        });
        
        //  Cache ASYNCHRONE
        if (cacheEnabled) {
            final User cacheUser = user;
            CompletableFuture.runAsync(() -> {
                try {
                    CacheService.cacheUser(cacheUser); // Mettre à jour le cache avec les nouvelles données
                    System.out.println("User cache UPDATED: " + userId);
                } catch (Exception e) {
                    System.err.println(" Cache update error for user " + userId + ": " + e.getMessage());
                }
            });
        }
        
        //  Réponse IMMÉDIATE
        long duration = System.currentTimeMillis() - startTime;
        Map<String, Object> response = new HashMap<>();
        response.put("message", "LegoSet added to user collection");
        response.put("userId", userId);
        response.put("legoSetId", legoSetId);
        response.put("processingTime", duration + "ms");
        response.put("async", "true");
        
        System.out.println(" LegoSet added successfully: " + legoSetId + " to " + userId + " (" + duration + "ms)");
        
        return Response.ok(response).build();
        
    } catch (Exception e) {
        System.err.println("Error adding LegoSet " + legoSetId + " to user " + userId + ": " + e.getMessage());
        e.printStackTrace();
        return Response.status(500).entity("Error adding LegoSet to user: " + e.getMessage()).build();
    }
}
 @GET
    @Path("/{id}/auctions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserAuctions(@PathParam("id") String userId) {
        try {
            Iterator<Auction> userAuctions = dbLayer.getAuctionsByUser(userId).iterator();
            
            List<Auction> auctionsList = new ArrayList<>();
            while (userAuctions.hasNext()) {
                auctionsList.add(userAuctions.next());
            }

            return Response.ok(auctionsList).build();
            
        } catch (Exception e) {
            return Response.status(500)
                .entity("Error retrieving user auctions: " + e.getMessage())
                .build();
        }
    }
}
