package cc.srv.functions;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.azure.ai.textanalytics.TextAnalyticsClient;
import com.azure.ai.textanalytics.TextAnalyticsClientBuilder;
import com.azure.ai.textanalytics.models.DocumentSentiment;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.util.CosmosPagedIterable;
import cc.srv.db.CosmosDBLayer;

import java.util.*;

public class CommentAnalysisFunction {

    private final CosmosDBLayer cosmosDBLayer;
    private TextAnalyticsClient textAnalyticsClient;

    public CommentAnalysisFunction() {
        this.cosmosDBLayer = CosmosDBLayer.getInstance();
        
        String endpoint = System.getenv("AZURE_TEXT_ANALYTICS_ENDPOINT");
        String apiKey = System.getenv("AZURE_TEXT_ANALYTICS_KEY");
        
        if (endpoint != null && apiKey != null) {
            this.textAnalyticsClient = new TextAnalyticsClientBuilder()
                .credential(new AzureKeyCredential(apiKey))
                .endpoint(endpoint)
                .buildClient();
        }
    }

    /**
     * Timer - Analyse tous les LegoSets toutes les 5 minutes
     */
    @FunctionName("AnalyzeRecentLegoSets")
    public void analyzeAllLegoSets(
        @TimerTrigger(
            name = "timerInfo",
            schedule = "0 0 2 * * *"  // Tous les jours à 2h du matin
        ) String timerInfo,
        final ExecutionContext context) {

        context.getLogger().info("Starting sentiment analysis for all Lego Sets");

        try {
            if (textAnalyticsClient == null) {
                context.getLogger().info("Azure Cognitive Services not configured");
                return;
            }

            CosmosContainer legoSetContainer = cosmosDBLayer.getLegoSetContainer();
            // Analyzing only recently created LegoSets (last 5 days) for efficiency
            String query = "SELECT c.id FROM c WHERE c.createdAt > DateTimeAdd('dd', -5, GetCurrentDateTime())";
        
            CosmosPagedIterable<Map> allLegoSets = legoSetContainer.queryItems(
                query, new CosmosQueryRequestOptions(), Map.class);

            int count = 0;
            for (Map legoSet : allLegoSets) {
                String legoSetId = (String) legoSet.get("id");
                try {
                    analyzeAndUpdateLegoSet(legoSetId, context);
                    count++;
                } catch (Exception e) {
                    context.getLogger().info("Failed to analyze " + legoSetId + ": " + e.getMessage());
                }
            }

            context.getLogger().info("Analysis completed. Processed: " + count + " LegoSets");

        } catch (Exception e) {
            context.getLogger().info("Error in analysis: " + e.getMessage());
        }
    }

    /**
     * Endpoint HTTP pour analyser un LegoSet spécifique
     */
    @FunctionName("AnalyzeLegoSet")
    public HttpResponseMessage analyzeLegoSet(
        @HttpTrigger(
            name = "req",
            methods = {HttpMethod.GET},
            authLevel = AuthorizationLevel.ANONYMOUS
        ) HttpRequestMessage<Optional<String>> request,
        final ExecutionContext context) {

        try {
            String legoSetId = request.getQueryParameters().get("legoSetId");
            if (legoSetId == null) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"legoSetId parameter is required\"}")
                    .build();
            }

            Map<String, Object> result = analyzeAndUpdateLegoSet(legoSetId, context);
            return createJsonResponse(request, result);

        } catch (Exception e) {
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    private Map<String, Object> analyzeAndUpdateLegoSet(String legoSetId, ExecutionContext context) {
        Map<String, Object> result = new HashMap<>();
        result.put("legoSetId", legoSetId);
        
        try {
            // Récupérer les commentaires
            CosmosContainer commentContainer = cosmosDBLayer.getCommentContainer();
            String query = "SELECT c.content FROM c WHERE c.legoSetId = '" + legoSetId + "'";
            
            CosmosPagedIterable<Map> comments = commentContainer.queryItems(
                query, new CosmosQueryRequestOptions(), Map.class);

            int totalComments = 0;
            int positiveComments = 0;
            double totalScore = 0;

            // Analyser chaque commentaire
            for (Map comment : comments) {
                String content = (String) comment.get("content");
                if (content != null && !content.trim().isEmpty()) {
                    totalComments++;
                    
                    DocumentSentiment sentiment = textAnalyticsClient.analyzeSentiment(content);
                    double score = sentiment.getConfidenceScores().getPositive();
                    totalScore += score;
                    
                    if (score > 0.6) {
                        positiveComments++;
                    }
                }
            }

            // Calculer le résultat
            double averageScore = totalComments > 0 ? totalScore / totalComments : 0;
            boolean isLiked = averageScore > 0.6;

            result.put("isLiked", isLiked);
            result.put("averageScore", averageScore);
            result.put("positiveComments", positiveComments);
            result.put("totalComments", totalComments);

            // Mettre à jour la base
            updateLegoSet(legoSetId, result);

            context.getLogger().info("LegoSet " + legoSetId + " - Liked: " + isLiked + ", Score: " + averageScore);

        } catch (Exception e) {
            context.getLogger().info("Error analyzing " + legoSetId + ": " + e.getMessage());
            throw e;
        }
        
        return result;
    }

    private void updateLegoSet(String legoSetId, Map<String, Object> sentiment) {
        try {
            CosmosContainer legoSetContainer = cosmosDBLayer.getLegoSetContainer();
            
            String query = "SELECT * FROM c WHERE c.id = '" + legoSetId + "'";
            CosmosPagedIterable<Map> results = legoSetContainer.queryItems(
                query, new CosmosQueryRequestOptions(), Map.class);
            
            Map<String, Object> legoSet = results.stream().findFirst().orElse(null);
            if (legoSet != null) {
                legoSet.put("sentimentScore", sentiment.get("averageScore"));
                legoSet.put("isLiked", sentiment.get("isLiked"));
                legoSet.put("lastAnalysis", new Date().toString());
                legoSetContainer.upsertItem(legoSet);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to update LegoSet " + legoSetId + ": " + e.getMessage());
        }
    }

    private HttpResponseMessage createJsonResponse(HttpRequestMessage<?> request, Map<String, Object> data) {
        try {
            String json = String.format(
                "{\"legoSetId\":\"%s\", \"isLiked\":%s, \"averageScore\":%.2f, \"totalComments\":%d}",
                data.get("legoSetId"), data.get("isLiked"), data.get("averageScore"), data.get("totalComments")
            );
            return request.createResponseBuilder(HttpStatus.OK)
                .body(json)
                .header("Content-Type", "application/json")
                .build();
        } catch (Exception e) {
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("{\"error\":\"Response error\"}")
                .build();
        }
    }
}