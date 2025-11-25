package cc.srv.cache;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisCache {
    // Configuration pour Redis local
    private static final String REDIS_HOST = System.getenv().getOrDefault("REDIS_HOST", "localhost");
    private static final int REDIS_PORT = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
    private static final String REDIS_PASSWORD = System.getenv().getOrDefault("REDIS_PASSWORD", "");
    private static final int REDIS_TIMEOUT = 1000;
    
    private static JedisPool instance;
    
    public synchronized static JedisPool getCachePool() {
        if( instance != null)
            return instance;
            
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        
       
        if (REDIS_PASSWORD != null && !REDIS_PASSWORD.isEmpty()) {
            // Avec authentification
            instance = new JedisPool(poolConfig, REDIS_HOST, REDIS_PORT, REDIS_TIMEOUT, REDIS_PASSWORD);
        } else {
            // Sans authentification (développement)
            instance = new JedisPool(poolConfig, REDIS_HOST, REDIS_PORT, REDIS_TIMEOUT);
        }
        
        return instance;
    }
    
  
}