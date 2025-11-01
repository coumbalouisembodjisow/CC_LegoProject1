package cc.srv.cache;

import cc.srv.data.User;
import java.util.Base64;
import cc.srv.data.LegoSet;
import cc.srv.data.Auction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CacheService {
    private static final ObjectMapper mapper = new ObjectMapper();
    
    // TTL en minutes
    private static final int TTL_SHORT = 5;    // Données volatiles (auctions)
    private static final int TTL_MEDIUM = 30;  // Données semi-stables  
    private static final int TTL_LONG = 60;    // Données stables (legosets)
    
    // Préfixes des clés
    private static final String USER_PREFIX = "user:";
    private static final String LEGOSET_PREFIX = "legoset:";
    private static final String AUCTION_PREFIX = "auction:";
    private static final String AUCTION_SEARCH_PREFIX = "auction_search:";
    private static final String RECENT_LEGOSETS = "recent_legosets";
    private static final String USER_LEGOSETS_PREFIX = "user_legosets:";
    private static final String ACTIVE_AUCTIONS = "active_auctions";
    
    // === USER CACHE ===
    
    public static void cacheUser(User user) {
        if (user == null || user.getId() == null) {
            System.err.println("Cannot cache null user or user without ID");
            return;
        }
        
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            String key = USER_PREFIX + user.getId();
            String userJson = mapper.writeValueAsString(user);
            jedis.setex(key, TTL_MEDIUM * 60, userJson);
            System.out.println("User cached: " + user.getId());
        } catch (Exception e) {
            System.err.println("Error caching user " + user.getId() + ": " + e.getMessage());
        }
    }
    
    public static User getCachedUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            String key = USER_PREFIX + userId;
            String userJson = jedis.get(key);
            if (userJson != null && !userJson.trim().isEmpty()) {
                System.out.println("User from cache: " + userId);
                return mapper.readValue(userJson, User.class);
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error getting cached user " + userId + ": " + e.getMessage());
            return null;
        }
    }
    
    public static void invalidateUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }
        
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            String userKey = USER_PREFIX + userId;
            String userLegoSetsKey = USER_LEGOSETS_PREFIX + userId;
            
            jedis.del(userKey, userLegoSetsKey);
            System.out.println("User cache invalidated: " + userId);
        } catch (Exception e) {
            System.err.println("Error invalidating user cache " + userId + ": " + e.getMessage());
        }
    }
    
    // === LEGOSET CACHE ===
    
    public static void cacheLegoSet(LegoSet legoSet) {
        if (legoSet == null || legoSet.getId() == null) {
            System.err.println("Cannot cache null legoSet or legoSet without ID");
            return;
        }
        
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            String key = LEGOSET_PREFIX + legoSet.getId();
            String legoSetJson = mapper.writeValueAsString(legoSet);
            jedis.setex(key, TTL_LONG * 60, legoSetJson);
            System.out.println("LegoSet cached: " + legoSet.getId());
        } catch (Exception e) {
            System.err.println("Error caching legoSet " + legoSet.getId() + ": " + e.getMessage());
        }
    }
    
    public static LegoSet getCachedLegoSet(String legoSetId) {
        if (legoSetId == null || legoSetId.trim().isEmpty()) {
            return null;
        }
        
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            String key = LEGOSET_PREFIX + legoSetId;
            String legoSetJson = jedis.get(key);
            if (legoSetJson != null && !legoSetJson.trim().isEmpty()) {
                System.out.println("LegoSet from cache: " + legoSetId);
                return mapper.readValue(legoSetJson, LegoSet.class);
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error getting cached legoSet " + legoSetId + ": " + e.getMessage());
            return null;
        }
    }
    
    public static void invalidateLegoSet(String legoSetId) {
        if (legoSetId == null || legoSetId.trim().isEmpty()) {
            return;
        }
        
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            String legoSetKey = LEGOSET_PREFIX + legoSetId;
            jedis.del(legoSetKey);
            // Invalider les listes globales
            jedis.del(RECENT_LEGOSETS);
            System.out.println("LegoSet cache invalidated: " + legoSetId);
        } catch (Exception e) {
            System.err.println("Error invalidating legoSet cache " + legoSetId + ": " + e.getMessage());
        }
    }
    
    // === AUCTION CACHE ===
    
    public static void cacheAuction(Auction auction) {
        if (auction == null || auction.getId() == null) {
            System.err.println("Cannot cache null auction or auction without ID");
            return;
        }
        
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            String key = AUCTION_PREFIX + auction.getId();
            String auctionJson = mapper.writeValueAsString(auction);
            jedis.setex(key, TTL_SHORT * 60, auctionJson); // Court TTL car données changeantes
            System.out.println("Auction cached: " + auction.getId());
        } catch (Exception e) {
            System.err.println("Error caching auction " + auction.getId() + ": " + e.getMessage());
        }
    }
    
    public static Auction getCachedAuction(String auctionId) {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            return null;
        }
        
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            String key = AUCTION_PREFIX + auctionId;
            String auctionJson = jedis.get(key);
            if (auctionJson != null && !auctionJson.trim().isEmpty()) {
                System.out.println("Auction from cache: " + auctionId);
                return mapper.readValue(auctionJson, Auction.class);
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error getting cached auction " + auctionId + ": " + e.getMessage());
            return null;
        }
    }
    
    public static void invalidateAuction(String auctionId) {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            return;
        }
        
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            String auctionKey = AUCTION_PREFIX + auctionId;
            jedis.del(auctionKey);
            // Invalider les recherches qui pourraient contenir cette auction
            jedis.del(ACTIVE_AUCTIONS);
            System.out.println("Auction cache invalidated: " + auctionId);
        } catch (Exception e) {
            System.err.println("Error invalidating auction cache " + auctionId + ": " + e.getMessage());
        }
    }
    
    // === AUCTION SEARCH CACHE ===
    
    public static void cacheAuctionSearch(String legoSetId, List<Auction> auctions) {
        if (legoSetId == null || auctions == null) {
            System.err.println("Cannot cache null legoSetId or auctions list");
            return;
        }
        
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            String key = AUCTION_SEARCH_PREFIX + legoSetId;
            String auctionsJson = mapper.writeValueAsString(auctions);
            jedis.setex(key, TTL_SHORT * 60, auctionsJson);
            System.out.println("Auction search cached for legoSet: " + legoSetId);
        } catch (Exception e) {
            System.err.println("Error caching auction search for " + legoSetId + ": " + e.getMessage());
        }
    }
    
    public static List<Auction> getCachedAuctionSearch(String legoSetId) {
        if (legoSetId == null || legoSetId.trim().isEmpty()) {
            return null;
        }
        
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            String key = AUCTION_SEARCH_PREFIX + legoSetId;
            String auctionsJson = jedis.get(key);
            if (auctionsJson != null && !auctionsJson.trim().isEmpty()) {
                System.out.println("Auction search from cache: " + legoSetId);
                return mapper.readValue(auctionsJson, new TypeReference<List<Auction>>(){});
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error getting cached auction search for " + legoSetId + ": " + e.getMessage());
            return null;
        }
    }
    
  
    public static List<LegoSet> getCachedRecentLegoSets() {
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            String legoSetsJson = jedis.get(RECENT_LEGOSETS);
            if (legoSetsJson != null && !legoSetsJson.trim().isEmpty()) {
                System.out.println("Recent LegoSets from cache");
                return mapper.readValue(legoSetsJson, new TypeReference<List<LegoSet>>(){});
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error getting cached recent legosets: " + e.getMessage());
            return null;
        }
    }

    public static void cacheAuctionsByLegoSet(String legoSetId, List<Auction> auctions) {
    if (legoSetId == null || legoSetId.trim().isEmpty()) {
        System.err.println("Cannot cache auctions for null or empty legoSetId");
        return;
    }
    
    if (auctions == null) {
        System.err.println("Cannot cache null auctions list for legoSet: " + legoSetId);
        return;
    }
    
    try (Jedis jedis = RedisCache.getCachePool().getResource()) {
        String key = "auctions_by_legoset:" + legoSetId;
        String auctionsJson = mapper.writeValueAsString(auctions);
        
        // TTL court car les données d'auctions changent fréquemment
        jedis.setex(key, TTL_SHORT * 60, auctionsJson);
        
        System.out.println("Auctions cached for LegoSet: " + legoSetId + " (" + auctions.size() + " items)");
        
    } catch (Exception e) {
        System.err.println(" Error caching auctions for LegoSet " + legoSetId + ": " + e.getMessage());
    }
}
public static void cacheRecentLegoSets(List<LegoSet> legoSets) {
    if (legoSets == null) {
        System.err.println("Cannot cache null recent legosets list");
        return;
    }
    
    try (Jedis jedis = RedisCache.getCachePool().getResource()) {
        String legoSetsJson = mapper.writeValueAsString(legoSets);
        
        // TTL court car la liste des récents change fréquemment
        jedis.setex("recent_legosets", TTL_SHORT * 60, legoSetsJson);
        
        System.out.println("Recent LegoSets cached (" + legoSets.size() + " items)");
        
    } catch (Exception e) {
        System.err.println(" Error caching recent legosets: " + e.getMessage());
    }
}
    
    // === USER LEGOSETS COLLECTION ===
    
    public static void cacheUserLegoSets(String userId, List<LegoSet> legoSets) {
        if (userId == null || legoSets == null) {
            System.err.println("Cannot cache null user ID or legoSets list");
            return;
        }
        
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            String key = USER_LEGOSETS_PREFIX + userId;
            String legoSetsJson = mapper.writeValueAsString(legoSets);
            jedis.setex(key, TTL_MEDIUM * 60, legoSetsJson);
            System.out.println("User LegoSets cached for: " + userId);
        } catch (Exception e) {
            System.err.println("Error caching user legosets for " + userId + ": " + e.getMessage());
        }
    }
    
    public static List<LegoSet> getCachedUserLegoSets(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            String key = USER_LEGOSETS_PREFIX + userId;
            String legoSetsJson = jedis.get(key);
            if (legoSetsJson != null && !legoSetsJson.trim().isEmpty()) {
                System.out.println("User LegoSets from cache: " + userId);
                return mapper.readValue(legoSetsJson, new TypeReference<List<LegoSet>>(){});
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error getting cached user legosets for " + userId + ": " + e.getMessage());
            return null;
        }
    }
    
    // === UTILITY METHODS ===
    
    public static void clearAllCache() {
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            jedis.flushDB();
            System.out.println("All cache cleared");
        } catch (Exception e) {
            System.err.println("Error clearing all cache: " + e.getMessage());
        }
    }
    
    public static boolean isAvailable() {
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            String result = jedis.ping();
            if (!"PONG".equals(result)) {
                throw new RuntimeException("Unexpected ping response: " + result);
            }
            System.out.println("Redis cache is available");
            return true;
        } catch (Exception e) {
            System.err.println("Redis not available: " + e.getMessage());
            return false;
        }
    }
    
    // Methode de test
    public static void testCacheConnection() {
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            System.out.println("Testing Redis cache connection...");
            
            String testKey = "test:connection";
            String testValue = "Hello Redis " + System.currentTimeMillis();
            
            jedis.setex(testKey, 60, testValue);
            String retrieved = jedis.get(testKey);
            
            if (testValue.equals(retrieved)) {
                System.out.println("Redis test successful - data stored and retrieved correctly");
            } else {
                System.err.println("Redis test failed - data mismatch");
            }
            
            jedis.del(testKey);
            
        } catch (Exception e) {
            System.err.println("Redis cache test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

   /**
 *get auctions for a specific LegoSet from cache
 */
public static List<Auction> getCachedAuctionsByLegoSet(String legoSetId) {
    if (legoSetId == null || legoSetId.trim().isEmpty()) {
        return null;
    }
    
    try (Jedis jedis = RedisCache.getCachePool().getResource()) {
        String key = "auctions_by_legoset:" + legoSetId;
        String auctionsJson = jedis.get(key);
        if (auctionsJson != null && !auctionsJson.trim().isEmpty()) {
            System.out.println("Auctions for LegoSet " + legoSetId + " served from CACHE");
            return mapper.readValue(auctionsJson, new TypeReference<List<Auction>>(){});
        }
        return null;
    } catch (Exception e) {
        System.err.println("Error getting cached auctions for LegoSet " + legoSetId + ": " + e.getMessage());
        return null;
    }
}

public static void invalidateRecentLegoSets() {
    try (Jedis jedis = RedisCache.getCachePool().getResource()) {
        jedis.del("recent_legosets");
        System.out.println("Recent LegoSets cache invalidated");
    } catch (Exception e) {
        System.err.println("Error invalidating recent legosets cache: " + e.getMessage());
    }
}


/**
 * Invalidate auctions cache for a specific LegoSet
 */
public static void invalidateAuctionsByLegoSet(String legoSetId) {
    if (legoSetId == null || legoSetId.trim().isEmpty()) {
        return;
    }
    
    try (Jedis jedis = RedisCache.getCachePool().getResource()) {
        String key = "auctions_by_legoset:" + legoSetId;
        jedis.del(key);
        System.out.println("Auctions cache invalidated for LegoSet: " + legoSetId);
    } catch (Exception e) {
        System.err.println("Error invalidating auctions cache for LegoSet " + legoSetId + ": " + e.getMessage());
    }
}

// === MEDIA CACHE ===

/**
 * Cache un média (contenu binaire)
 */
public static void cacheMedia(String mediaId, byte[] content, String contentType) {
    if (mediaId == null || content == null) {
        System.err.println("Cannot cache null mediaId or content");
        return;
    }
    
    try (Jedis jedis = RedisCache.getCachePool().getResource()) {
        // Stocker le contenu et le contentType
        String key = "media:" + mediaId;
        Map<String, String> mediaData = new HashMap<>();
        mediaData.put("content", Base64.getEncoder().encodeToString(content));
        mediaData.put("contentType", contentType != null ? contentType : "application/octet-stream");
        
        // TTL long car les médias ne changent pas
        jedis.hset(key, mediaData);
        jedis.expire(key, TTL_LONG * 60);
        
        System.out.println("Media cached: " + mediaId + " (" + content.length + " bytes)");
        
    } catch (Exception e) {
        System.err.println(" Error caching media " + mediaId + ": " + e.getMessage());
    }
}

/**
 * Récupère un média en cache
 */
public static Map<String, String> getCachedMedia(String mediaId) {
    if (mediaId == null || mediaId.trim().isEmpty()) {
        return null;
    }
    
    try (Jedis jedis = RedisCache.getCachePool().getResource()) {
        String key = "media:" + mediaId;
        Map<String, String> mediaData = jedis.hgetAll(key);
        
        if (mediaData != null && !mediaData.isEmpty() && mediaData.containsKey("content")) {
            System.out.println(" Media " + mediaId + " served from CACHE");
            return mediaData;
        }
        return null;
        
    } catch (Exception e) {
        System.err.println(" Error getting cached media " + mediaId + ": " + e.getMessage());
        return null;
    }
}

/**
 * Invalide le cache d'un média
 */
public static void invalidateMedia(String mediaId) {
    if (mediaId == null || mediaId.trim().isEmpty()) {
        return;
    }
    
    try (Jedis jedis = RedisCache.getCachePool().getResource()) {
        String key = "media:" + mediaId;
        Long deleted = jedis.del(key);
        
        if (deleted > 0) {
            System.out.println(" Media cache invalidated: " + mediaId);
        }
        
    } catch (Exception e) {
        System.err.println(" Error invalidating media cache " + mediaId + ": " + e.getMessage());
    }
}
}