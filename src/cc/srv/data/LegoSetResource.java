package cc.srv.data;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import cc.srv.db.CosmosDBLayer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;

@Path("/legoset")
public class LegoSetResource {
    private CosmosDBLayer dbLayer = CosmosDBLayer.getInstance();


    
    // create  a new LegoSet
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createLegoSet(LegoSet legoSet) {
        try {
            // requirements validation
            if (legoSet.getId() == null || legoSet.getId().trim().isEmpty()) {
                return Response.status(400).entity("LegoSet ID is required").build();
            }
            if (legoSet.getName() == null || legoSet.getName().trim().isEmpty()) {
                return Response.status(400).entity("LegoSet name is required").build();
            }
            if (legoSet.getCodeNumber() == null || legoSet.getCodeNumber().trim().isEmpty()) {
                return Response.status(400).entity("LegoSet code number is required").build();
            }
            
            // check for at least one photo
            if (legoSet.getPhotoUrls() == null || legoSet.getPhotoUrls().isEmpty()) {
                return Response.status(400).entity("At least one photo is required").build();
            }

            // check for existing LegoSet with same ID
            Iterator<LegoSet> existing = dbLayer.getLegoSetById(legoSet.getId()).iterator();
            if (existing.hasNext()) {
                return Response.status(409).entity("LegoSet already exists with ID: " + legoSet.getId()).build();
            }

            dbLayer.putLegoSet(legoSet);
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
            Iterator<LegoSet> iterator = dbLayer.getLegoSetById(id).iterator();

            if (iterator.hasNext()) {
                LegoSet legoSet = iterator.next();
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

            if (legoSet.getPhotoUrls() == null || legoSet.getPhotoUrls().isEmpty()) {
                return Response.status(400).entity("At least one photo is required").build();
            }

            dbLayer.updateLegoSet(legoSet);
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
            int count = dbLayer.countLegoSets();
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
                return Response.status(400).entity("Comment ID is required").build();
            }
            if (comment.getUserId() == null || comment.getUserId().trim().isEmpty()) {
                return Response.status(400).entity("User ID is required").build();
            }
            if (comment.getContent() == null || comment.getContent().trim().isEmpty()) {
                return Response.status(400).entity("Comment content is required").build();
            }
            

            // check that the LegoSet exists
            Iterator<LegoSet> legoSetIterator = dbLayer.getLegoSetById(legoSetId).iterator();
            if (!legoSetIterator.hasNext()) {
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
    @Path("/{id}/comment")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getComments(@PathParam("id") String legoSetId) {
        try {
            // check that the LegoSet exists
            Iterator<LegoSet> legoSetIterator = dbLayer.getLegoSetById(legoSetId).iterator();
            if (!legoSetIterator.hasNext()) {
                return Response.status(404).entity("LegoSet not found with ID: " + legoSetId).build();
            }

            // retrieve comments
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

}