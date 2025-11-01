package cc.srv.data;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import cc.srv.cache.CacheService;
import cc.srv.db.CosmosDBLayer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
/**
 * Class with control endpoints.
 */
@Path("/user")
public class UserResource
{
	private CosmosDBLayer dbLayer = CosmosDBLayer.getInstance();
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
        int userCount = dbLayer.countUsers();
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
            Iterator<User> iterator = dbLayer.getUserById(id).iterator();

            if (iterator.hasNext()) {
                User user = iterator.next();
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
        try {
            System.out.println("Received: " + userData);
            
            // Conversion manuelle
            User user = new User();
            user.setId((String) userData.get("id"));
            user.setName((String) userData.get("name"));
            user.setPassword((String) userData.get("password"));
            user.setPhotoMediaId((String) userData.get("photoMediaId"));

            if (user.getId() == null || user.getId().trim().isEmpty()) {
                return Response.status(400).entity("{\"error\":\"ID required\"}").build();
            }

            dbLayer.putUser(user);
            // cache the new user
            boolean cacheEnabled = Boolean.parseBoolean(System.getenv("CACHE_ENABLED"));
            if (cacheEnabled) {
                CacheService.cacheUser(user);
                System.out.println("New user " + user.getId() + " CACHED after creation");
            }
        
            return Response.status(201).entity(user).build();
            
        } catch (Exception e) {
            return Response.status(500).entity("{\"error\":\"Server error\"}").build();
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
        Iterator<User> userIterator = dbLayer.getUserById(id).iterator();
        if (!userIterator.hasNext()) {
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
        
        Iterator<User> deletedUserIterator = dbLayer.getUserById("deleted-user").iterator();
        
        if (!deletedUserIterator.hasNext()) {
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
        Iterator<User> userIterator = dbLayer.getUserById(userId).iterator();
        if (!userIterator.hasNext()) {
            return Response.status(404).entity("User not found with ID: " + userId).build();
        }

        User user = userIterator.next();
        
        // get list of owned LegoSet IDs
        List<String> ownedLegoSetIds = user.getOwnedLegoSets();
        
        if (ownedLegoSetIds == null || ownedLegoSetIds.isEmpty()) {
            return Response.status(404).entity("No LegoSets found for user: " + userId).build();
        }

        // Récupérer les détails de chaque LegoSet
        List<LegoSet> userLegoSets = new ArrayList<>();
        for (String legoSetId : ownedLegoSetIds) {
            Iterator<LegoSet> legoSetIterator = dbLayer.getLegoSetById(legoSetId).iterator();
            if (legoSetIterator.hasNext()) {
                userLegoSets.add(legoSetIterator.next());
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
    @PathParam("id") String userId, 
    @PathParam("legoSetId") String legoSetId) {  
    
    try {
        Iterator<User> userIterator = dbLayer.getUserById(userId).iterator();
        if (!userIterator.hasNext()) {
            return Response.status(404).entity("User not found").build();
        }

        User user = userIterator.next();
        user.addOwnedLegoSet(legoSetId);
        dbLayer.updateUser(user);
        // invalidate cache
        boolean cacheEnabled = Boolean.parseBoolean(System.getenv("CACHE_ENABLED"));
        if (cacheEnabled) {
            CacheService.invalidateUser(userId);
            System.out.println("User LegoSets " + userId + " cache INVALIDATED after adding new LegoSet");
        }
        Map<String, String> response = new HashMap<>();
        response.put("message", "LegoSet added to user collection");
        response.put("userId", userId);
        response.put("legoSetId", legoSetId);

        return Response.ok(response).build();
        
    } catch (Exception e) {
        return Response.status(500).entity("Error adding LegoSet to user: " + e.getMessage()).build();
    }
}
}
