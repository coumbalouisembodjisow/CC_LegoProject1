package cc.srv.data;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import cc.srv.db.CosmosDBLayer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import cc.srv.cache.CacheService;
import java.util.Date;
import java.util.stream.Collectors;

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
            // try cache
            boolean cacheEnabled = Boolean.parseBoolean(System.getenv("CACHE_ENABLED"));
            if (cacheEnabled) {
                Auction cachedAuction = CacheService.getCachedAuction(id);
                if (cachedAuction != null) {
                    System.out.println("Auction " + id + " served from CACHE");
                    return Response.ok(cachedAuction).build();
                }}
            // if not in cache, get from database
            Iterator<Auction> iterator = dbLayer.getAuctionById(id).iterator();

            if (iterator.hasNext()) {
                Auction auction = iterator.next();
                // cache it
                if (cacheEnabled) {
                    CacheService.cacheAuction(auction);
                    System.out.println("Auction " + id + " CACHED after retrieval from DB");
                }
                return Response.ok(auction).build();

            } else {
                return Response.status(404).entity("Auction not found with ID: " + id).build();
            }
        } catch (Exception e) {
            return Response.status(500).entity("Error retrieving auction: " + e.getMessage()).build();
        }
    }

    // get auctions' bids
@GET
@Path("/{id}/bids")
@Produces(MediaType.APPLICATION_JSON)
public Response getAuctionBids(@PathParam("id") String auctionId) {
    try {
        // Récupérer l'auction depuis la base de données
        Iterator<Auction> iterator = dbLayer.getAuctionById(auctionId).iterator();
        
        if (!iterator.hasNext()) {
            return Response.status(404).entity("Auction not found").build();
        }
        
        Auction auction = iterator.next();
        
        // Retourner uniquement la liste des bids
        List<AuctionBid> bids = auction.getBids();
        
        return Response.ok(bids).build();
        
    } catch (Exception e) {
        return Response.status(500).entity("Error retrieving auction bids: " + e.getMessage()).build();
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
                auction.setId(java.util.UUID.randomUUID().toString());
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
            // cache the new auction
            boolean cacheEnabled = Boolean.parseBoolean(System.getenv("CACHE_ENABLED"));
            if (cacheEnabled) {
                CacheService.cacheAuction(auction);
                System.out.println("New auction " + auction.getId() + " CACHED after creation");
            }
            // invalidate auctions list cache for this LegoSet
                CacheService.invalidateAuctionsByLegoSet(auction.getLegoSetId());
            
            System.out.println("New auction " + auction.getId() + " CACHED and lists INVALIDATED after creation");
        
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
       
        if (bid.getId() == null || bid.getId().trim().isEmpty()) {
            bid.setId(UUID.randomUUID().toString()); // Génération ID automatique
             }
        
        bid.setAuctionId(auctionId); // Associer l'ID de l'enchère
       
        
        // Ajouter l'enchère
        auction.addBid(bid);
        
        // Mettre à jour l'enchère dans la base
        dbLayer.updateAuction(auction);
        
        // Invalider le cache
        boolean cacheEnabled = Boolean.parseBoolean(System.getenv("CACHE_ENABLED"));
        if (cacheEnabled) {
            CacheService.cacheAuction(auction); 
            CacheService.invalidateAuctionsByLegoSet(auction.getLegoSetId()); 
            System.out.println("Auction cache UPDATED and list cache INVALIDATED after new bid");
        }
        
        return Response.ok(auction).build();
        
    } catch (Exception e) {
        return Response.status(500).entity("Error placing bid: " + e.getMessage()).build();
    }
}

    @GET
    @Path("/legoset/{legoSetId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getActiveAuctionsByLegoSet(@PathParam("legoSetId") String legoSetId) {
        try {
            // try to get from cache first
            boolean cacheEnabled = Boolean.parseBoolean(System.getenv("CACHE_ENABLED"));
            if (cacheEnabled) {
                List<Auction> cachedAuctions = CacheService.getCachedAuctionsByLegoSet(legoSetId);
                if (cachedAuctions != null) {
                    System.out.println("Auctions for LegoSet " + legoSetId + " served from CACHE");
                    return Response.ok(cachedAuctions).build();
                }}
            CosmosContainer auctionContainer = dbLayer.getAuctionContainer();
            String query = "SELECT * FROM c WHERE c.legoSetId = '" + legoSetId + "' ";
            Iterator<Auction> auctionIterator = auctionContainer.queryItems(query, new CosmosQueryRequestOptions(), Auction.class).iterator();
            
            List<Auction> activeAuctions = new ArrayList<>();
            while (auctionIterator.hasNext()) {
                activeAuctions.add(auctionIterator.next());
            }

            // cache the result
            if (cacheEnabled) {
            CacheService.cacheAuctionsByLegoSet(legoSetId, activeAuctions);
            System.out.println("Auctions for LegoSet " + legoSetId + " served from DB and CACHED");
        } else {
            System.out.println(" Auctions for LegoSet " + legoSetId + " served from DB (no cache)");
        }
            
            return Response.ok(activeAuctions).build();
            
        } catch (Exception e) {
            return Response.status(500).entity("Error retrieving active auctions: " + e.getMessage()).build();
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

@GET
@Path("/any/recent")
@Produces(MediaType.APPLICATION_JSON)
public Response getRecentAuctions(@QueryParam("st") int start, 
                                  @QueryParam("len") int length) {
    try {
        boolean cacheEnabled = Boolean.parseBoolean(System.getenv("CACHE_ENABLED"));
        
        int limit = (length > 0) ? length : 20;
        
        if (cacheEnabled) {
            List<Auction> cachedRecentAuctions = CacheService.getCachedRecentAuctions();
            if (cachedRecentAuctions != null) {
                System.out.println("Recent Auctions served from CACHE");
                List<Auction> limitedAuctions = cachedRecentAuctions.stream()
                    .skip(start)
                    .limit(limit)
                    .collect(Collectors.toList());
                return Response.ok(limitedAuctions).build();
            }
        }
        
        String query = "SELECT * FROM c ORDER BY c.startDate DESC OFFSET " + start + " LIMIT " + limit;
        Iterator<Auction> recentAuctions = dbLayer.getAuctionContainer()
            .queryItems(query, new CosmosQueryRequestOptions(), Auction.class)
            .iterator();
        
        List<Auction> recentAuctionList = new ArrayList<>();
        while (recentAuctions.hasNext()) {
            recentAuctionList.add(recentAuctions.next());
        }
        
        if (cacheEnabled) {
            CacheService.cacheRecentAuctions(recentAuctionList);
            System.out.println("Recent Auctions CACHED after retrieval from DB");
        }

        return Response.ok(recentAuctionList).build();
        
    } catch (Exception e) {
        return Response.status(500)
            .entity("Error retrieving recent Auctions: " + e.getMessage())
            .build();
    }
}
}