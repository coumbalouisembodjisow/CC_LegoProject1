package cc.srv.data;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import cc.srv.db.CosmosDBLayer;
import cc.srv.data.User;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
/**
 * Class with control endpoints.
 */
@Path("/user")
public class UserResource
{
	private CosmosDBLayer dbLayer = CosmosDBLayer.getInstance();
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
     * DELETE /user/{id} - Delete user by ID
     */
    @DELETE
    @Path("/{id}")
    public Response deleteUser(@PathParam("id") String id) {
        try {
            dbLayer.delUserById(id);
            return Response.status(204).build(); // No content
        } catch (Exception e) {
            return Response.status(500).entity("Error deleting user: " + e.getMessage()).build();
        }
    }

}
