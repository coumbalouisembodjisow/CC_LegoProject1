package cc.srv.data;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import cc.srv.db.CosmosDBLayer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Date;

@Path("/auction")
public class AuctionResource {
    private CosmosDBLayer dbLayer = CosmosDBLayer.getInstance();

    // ==================== GET ENDPOINTS ====================

    /**
     * GET /rest/auction - List all auctions
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listAuctions() {
        try {
            Iterator<Auction> auctionsIterator = dbLayer.getAuctions().iterator();
            List<Auction> auctionList = new ArrayList<>();
            
            while (auctionsIterator.hasNext()) {
                Auction auction = auctionsIterator.next();
                auctionList.add(auction);
            }

            return Response.ok(auctionList).build();
        } catch (Exception e) {
            return Response.status(500).entity("Error retrieving auctions: " + e.getMessage()).build();
        }
    }

    /**
     * GET /rest/auction/{id} - Get specific auction
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAuction(@PathParam("id") String id) {
        try {
            Iterator<Auction> iterator = dbLayer.getAuctionById(id).iterator();

            if (iterator.hasNext()) {
                Auction auction = iterator.next();
                return Response.ok(auction).build();
            } else {
                return Response.status(404).entity("Auction not found with ID: " + id).build();
            }
        } catch (Exception e) {
            return Response.status(500).entity("Error retrieving auction: " + e.getMessage()).build();
        }
    }

    // ==================== POST ENDPOINTS ====================

    /**
     * POST /rest/auction - Create new auction
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAuction(Auction auction) {
        try {
            // Validation
            if (auction.getId() == null || auction.getId().trim().isEmpty()) {
                return Response.status(400).entity("Auction ID is required").build();
            }
            if (auction.getLegoSetId() == null || auction.getLegoSetId().trim().isEmpty()) {
                return Response.status(400).entity("LegoSet ID is required").build();
            }
            if (auction.getSellerId() == null || auction.getSellerId().trim().isEmpty()) {
                return Response.status(400).entity("Seller ID is required").build();
            }
            if (auction.getCloseDate() == null) {
                return Response.status(400).entity("Close date is required").build();
            }

            // S'assurer que le status est ACTIVE par défaut et initialiser les bids
            auction.setStatus("ACTIVE");
            if (auction.getBids() == null) {
                auction.setBids(new ArrayList<>());
            }
            
            dbLayer.putAuction(auction);
            return Response.status(201).entity(auction).build();
            
        } catch (Exception e) {
            return Response.status(500).entity("Error creating auction: " + e.getMessage()).build();
        }
    }

    /**
     * POST /rest/auction/{id}/bid - Place bid on auction
     */
    @POST
    @Path("/{id}/bid")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response placeBid(@PathParam("id") String auctionId, AuctionBid bid) {
        try {
            Iterator<Auction> iterator = dbLayer.getAuctionById(auctionId).iterator();
            
            if (!iterator.hasNext()) {
                return Response.status(404).entity("Auction not found").build();
            }
            
            Auction auction = iterator.next();
            
            // Validation de l'enchère
            if (!"ACTIVE".equals(auction.getStatus())) {
                return Response.status(400).entity("Auction is not active").build();
            }
            
            if (new Date().after(auction.getCloseDate())) {
                return Response.status(400).entity("Auction has ended").build();
            }
            
            if (bid.getBidderId() == null || bid.getBidderId().trim().isEmpty()) {
                return Response.status(400).entity("Bidder ID is required").build();
            }
            
            if (bid.getAmount() <= auction.getBasePrice()) {
                return Response.status(400).entity("Bid must be higher than base price: " + auction.getBasePrice()).build();
            }
            
            // Vérifier si c'est plus haut que la meilleure enchère actuelle
            AuctionBid currentWinningBid = auction.getCurrentWinningBid();
            if (currentWinningBid != null && bid.getAmount() <= currentWinningBid.getAmount()) {
                return Response.status(400).entity("Bid must be higher than current winning bid: " + currentWinningBid.getAmount()).build();
            }
            
            // Ajouter l'enchère
            bid.setBidTime(new Date());
            auction.addBid(bid);
            
            // Mettre à jour l'enchère dans la base
            dbLayer.updateAuction(auction);
            return Response.ok(auction).build();
            
        } catch (Exception e) {
            return Response.status(500).entity("Error placing bid: " + e.getMessage()).build();
        }
    }

    // ==================== UTILITY ENDPOINTS ====================

    @GET
    @Path("/test")
    @Produces(MediaType.TEXT_PLAIN)
    public String test() {
        return "AuctionResource is working!";
    }

    @GET
    @Path("/active")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getActiveAuctions() {
        try {
            Iterator<Auction> auctionsIterator = dbLayer.getActiveAuctions().iterator();
            List<Auction> auctionList = new ArrayList<>();
            
            while (auctionsIterator.hasNext()) {
                Auction auction = auctionsIterator.next();
                auctionList.add(auction);
            }

            return Response.ok(auctionList).build();
        } catch (Exception e) {
            return Response.status(500).entity("Error retrieving active auctions: " + e.getMessage()).build();
        }
    }
}