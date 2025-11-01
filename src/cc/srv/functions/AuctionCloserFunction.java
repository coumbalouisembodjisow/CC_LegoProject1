package cc.srv.functions;

import java.util.*;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.util.CosmosPagedIterable;
import redis.clients.jedis.Jedis;
import cc.srv.db.CosmosDBLayer;
import cc.srv.cache.CacheService;
import cc.srv.cache.RedisCache;
import cc.srv.data.Auction;

public class AuctionCloserFunction {
    
private final CosmosDBLayer cosmosDBLayer;

    private final boolean cacheEnabled;

   

    public AuctionCloserFunction() {

        this.cosmosDBLayer = CosmosDBLayer.getInstance();

        this.cacheEnabled = "true".equals(System.getenv("CACHE_ENABLED"));

    }

   

    @FunctionName("CloseExpiredAuctions")

    public void run(

        @TimerTrigger(

            name = "timerInfo",

            schedule = "0 */5 * * * *"  // each 5 minutes

        ) String timerInfo,

        final ExecutionContext context) {

       

        context.getLogger().info("checking for expired auctions to close...");

       

        try {

            // get expired auctions

            List<Auction> expiredAuctions = findExpiredAuctions(context);

           

            context.getLogger().info(" " + expiredAuctions.size() + "expired auctions found.");

           

            // close each expired auction

            for (Auction auction : expiredAuctions) {

                closeAuction(auction, context);

            }

           

            context.getLogger().info(" " + expiredAuctions.size() + " auctions closed.");

           

        } catch (Exception e) {

            context.getLogger().severe(" Error : " + e.getMessage());

        }

    }

   

    private List<Auction> findExpiredAuctions(ExecutionContext context) {

        List<Auction> expiredAuctions = new ArrayList<>();

       

        try {

            CosmosContainer container = cosmosDBLayer.getAuctionContainer();

           

            // Requête pour trouver les enchères ACTIVES avec closeDate dépassée

            String query = "SELECT * FROM c  WHERE c.status = 'ACTIVE' AND c.closeDate <= GetCurrentDateTime()";

           

            CosmosPagedIterable<Auction> results = container.queryItems(

                query,

                new CosmosQueryRequestOptions(),

                Auction.class

            );

           

            results.forEach(expiredAuctions::add);

           

        } catch (Exception e) {

            context.getLogger().severe("Cosmos DB Error: " + e.getMessage());

        }

       

        return expiredAuctions;

    }

   

    private void closeAuction(Auction auction, ExecutionContext context) {

        try {

            // update auction status to ENDED

            auction.setStatus("ENDED");

            cosmosDBLayer.updateAuction(auction);

           

            context.getLogger().info("Closed Auction: " + auction.getId()

            + " - Bids: " + (auction.getBids() != null ? auction.getBids().size() : 0));

           

            // invalidate cache if enabled

            if (cacheEnabled) {

                invalidateAuctionCache(auction.getId(), context);

            }

           

        } catch (Exception e) {

            context.getLogger().severe(" Error when closing auction " + auction.getId() + ": " + e.getMessage());

        }

    }

   

    private void invalidateAuctionCache(String auctionId, ExecutionContext context) {

        try (Jedis jedis =RedisCache.getCachePool().getResource()) {

            // delete auction from cache

            jedis.del("auction:" + auctionId);

           

            context.getLogger().info(" Invalidated Cache: " + auctionId);

           

        } catch (Exception e) {

            context.getLogger().warning(" RediCache error: " + e.getMessage());

        }}
    
    @FunctionName("TestHealth")
    public HttpResponseMessage testHealth(
        @HttpTrigger(name = "req", methods = {HttpMethod.GET}, authLevel = AuthorizationLevel.ANONYMOUS) 
        HttpRequestMessage<Optional<String>> request,
        final ExecutionContext context) {
        
        context.getLogger().info("TestHealth function executed");
        
        return request.createResponseBuilder(HttpStatus.OK)
                      .body("Function App is working!")
                      .build();
    }
}