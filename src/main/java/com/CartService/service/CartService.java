package com.CartService.service;

import com.CartService.model.Cart;
import com.CartService.model.CartItem;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.CacheStatistics;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class CartService {

    private static final String HASH_KEY_PREFIX = "cart:";
    private static final int TTL_MINUTES = 30;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MeterRegistry meterRegistry;

    /**
     * Adds an item to the session's shopping cart.
     * If the item already exists, its quantity is updated (summed).
     * TTL is reset to 30 minutes, and the local Spring cache is evicted.
     */
    public Cart addItem(String sessionId, CartItem item) {
        String redisKey = HASH_KEY_PREFIX + sessionId;
        
        // Retrieve the existing item in the hash by productId
        CartItem existingItem = (CartItem) redisTemplate.opsForHash().get(redisKey, item.getProductId());
        
        if (existingItem != null) {
            // Update quantity by summing it
            existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
            redisTemplate.opsForHash().put(redisKey, item.getProductId(), existingItem);
        } else {
            // Add new entry
            redisTemplate.opsForHash().put(redisKey, item.getProductId(), item);
        }

        // Reset TTL to 30 minutes (sliding expiration)
        redisTemplate.expire(redisKey, TTL_MINUTES, TimeUnit.MINUTES);

        // Evict Spring Cache to ensure subsequent reads fetch the updated data from Redis Hash
        evictCache(sessionId);

        // Return the fully updated Cart
        return getCart(sessionId);
    }

    /**
     * Retrieves the current state of a shopping cart.
     * Uses Spring's CacheManager to record hit/miss statistics.
     */
    public Cart getCart(String sessionId) {
        Cache cache = cacheManager.getCache("cart");
        if (cache != null) {
            // This explicit get operation triggers the CacheManager's hit/miss recording
            Cache.ValueWrapper wrapper = cache.get(sessionId);
            if (wrapper != null) {
                Cart cachedCart = (Cart) wrapper.get();
                if (cachedCart != null) {
                    return cachedCart;
                }
            }
        }

        // Miss! Retrieve items directly from Redis Hash
        String redisKey = HASH_KEY_PREFIX + sessionId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(redisKey);

        List<CartItem> items = new ArrayList<>();
        if (entries != null) {
            for (Object val : entries.values()) {
                if (val instanceof CartItem) {
                    items.add((CartItem) val);
                }
            }
        }

        Cart cart = new Cart(sessionId, items);

        // Put the constructed cart in the Spring Cache if we have items
        if (cache != null && !items.isEmpty()) {
            cache.put(sessionId, cart);
        }

        return cart;
    }

    /**
     * Removes a single product item from the shopping cart.
     * Resets TTL to 30 minutes, and evicts the cached Cart.
     */
    public Cart removeItem(String sessionId, String productId) {
        String redisKey = HASH_KEY_PREFIX + sessionId;
        
        redisTemplate.opsForHash().delete(redisKey, productId);
        
        // Reset TTL to 30 minutes
        redisTemplate.expire(redisKey, TTL_MINUTES, TimeUnit.MINUTES);

        // Evict Spring Cache
        evictCache(sessionId);

        return getCart(sessionId);
    }

    /**
     * Clears the entire shopping cart (deletes the entire Redis Hash key).
     * Evicts the cached Cart.
     */
    public void clearCart(String sessionId) {
        String redisKey = HASH_KEY_PREFIX + sessionId;
        
        redisTemplate.delete(redisKey);
        
        // Evict Spring Cache
        evictCache(sessionId);
    }

    /**
     * Retrieves basic statistics about the cache.
     * 1. totalCarts: Count of active cart sessions in Redis.
     * 2. hitRate: Performance metric between 0.0 and 1.0, or -1 if not available.
     */
    public Map<String, Object> getCacheStats() {
        // totalCarts can be obtained by scanning for keys with the "cart:" pattern
        Set<String> keys = redisTemplate.keys(HASH_KEY_PREFIX + "*");
        int totalCarts = keys != null ? keys.size() : 0;

        double hitRate = -1.0;
        Cache cache = cacheManager.getCache("cart");
        if (cache instanceof RedisCache redisCache) {
            CacheStatistics cacheStats = redisCache.getStatistics();
            long hits = cacheStats.getHits();
            long misses = cacheStats.getMisses();
            if (hits + misses > 0) {
                hitRate = (double) hits / (hits + misses);
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCarts", totalCarts);
        stats.put("hitRate", hitRate);
        return stats;
    }

    /**
     * Helper to evict a session's entry from the Spring Cache.
     */
    private void evictCache(String sessionId) {
        Cache cache = cacheManager.getCache("cart");
        if (cache != null) {
            cache.evict(sessionId);
        }
    }

    /**
     * Queries the Micrometer MeterRegistry for the cache.gets counter matching the result tag.
     */
    private double getMeterCount(String result) {
        try {
            var search = meterRegistry.find("cache.gets")
                    .tag("cache", "cart")
                    .tag("result", result);

            var functionCounter = search.functionCounter();
            if (functionCounter != null) {
                return functionCounter.count();
            }

            var counter = search.counter();
            if (counter != null) {
                return counter.count();
            }
        } catch (Exception e) {
            // Ignore/Suppress
        }
        return 0.0;
    }
}
