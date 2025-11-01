package cc.srv.functions;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.util.CosmosPagedIterable;
import cc.srv.db.CosmosDBLayer;

import java.util.*;

public class GarbageCollectorFunction {

    private final CosmosDBLayer cosmosDBLayer;

    public GarbageCollectorFunction() {
        this.cosmosDBLayer = CosmosDBLayer.getInstance();
    }

    /**
     * Simple garbage collector - runs daily at 2 AM
     * Only cleans old auctions (most valuable cleanup)
     */
    @FunctionName("SimpleGarbageCollector")
    public void run(
        @TimerTrigger(
            name = "timerInfo",
            schedule = "0 0 2 * * *"  // Daily at 2 AM
        ) String timerInfo,
        final ExecutionContext context) {

        context.getLogger().info("Starting garbage collection - cleaning old auctions only");

        try {
            int deletedCount = cleanupOldAuctions(context);
            context.getLogger().info("Garbage collection completed. Deleted old auctions: " + deletedCount);

        } catch (Exception e) {
            context.getLogger().severe("Garbage collection failed: " + e.getMessage());
        }
    }

    /**
     * Clean auctions that ended more than 30 days ago
     */
    private int cleanupOldAuctions(ExecutionContext context) {
        int deletedCount = 0;
        
        try {
            CosmosContainer auctionContainer = cosmosDBLayer.getAuctionContainer();
            
            // Find auctions that ended more than 30 days ago
            String query = "SELECT c.id FROM c WHERE c.status = 'ENDED' " +
                          "AND c.closeDate < DateTimeAdd('dd', -30, GetCurrentDateTime())";
            
            CosmosPagedIterable<Map> oldAuctions = auctionContainer.queryItems(
                query, new CosmosQueryRequestOptions(), Map.class);
            
            // Delete old auction records - use null for request options
            for (Map auction : oldAuctions) {
                String auctionId = (String) auction.get("id");
                try {
                    auctionContainer.deleteItem(auctionId, null);
                    deletedCount++;
                    context.getLogger().info("Deleted old auction: " + auctionId);
                } catch (Exception e) {
                    context.getLogger().warning("Failed to delete auction " + auctionId);
                }
            }
            
        } catch (Exception e) {
            context.getLogger().severe("Error cleaning old auctions: " + e.getMessage());
        }
        
        return deletedCount;
    }

    /**
     * Simple test endpoint
     */
    @FunctionName("TestGarbageCollector")
    public HttpResponseMessage test(
        @HttpTrigger(
            name = "req",
            methods = {HttpMethod.GET},
            authLevel = AuthorizationLevel.ANONYMOUS
        ) HttpRequestMessage<Optional<String>> request,
        final ExecutionContext context) {

        context.getLogger().info("Manual garbage collection test");

        try {
            int deletedCount = cleanupOldAuctions(context);
            
            Map<String, Object> response = new HashMap<>();
            response.put("deletedAuctions", deletedCount);
            response.put("message", "Cleanup completed successfully");
            
            return request.createResponseBuilder(HttpStatus.OK)
                .body(response)
                .build();

        } catch (Exception e) {
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage())
                .build();
        }
    }
}