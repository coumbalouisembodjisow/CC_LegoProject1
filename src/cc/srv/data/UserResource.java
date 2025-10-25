package cc.srv.data;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
            Iterator<User> iterator = dbLayer.getUserById(id).iterator();

            if (iterator.hasNext()) {
                User user = iterator.next();
                return Response.ok(user).build();
            } else {
                return Response.status(404).entity("User not found with ID: " + id).build();
            }
        } catch (Exception e) {
            return Response.status(500).entity("Error retrieving user: " + e.getMessage()).build();
        }
    }

  
    /**
     * POST /user - Create a new user
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createUser(User user) {
        try {
            // Validate required fields
            if (user.getId() == null || user.getId().trim().isEmpty()) {
                return Response.status(400).entity("User ID is required").build();
            }
            if (user.getNickname() == null || user.getNickname().trim().isEmpty()) {
                return Response.status(400).entity("Nickname is required").build();
            }

            // Create user in database
            dbLayer.putUser(user);
            return Response.status(201).entity(user).build();
            
        } catch (Exception e) {
            return Response.status(500).entity("Error creating user: " + e.getMessage()).build();
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
            deletedUser.setNickname("Deleted User");
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
}
