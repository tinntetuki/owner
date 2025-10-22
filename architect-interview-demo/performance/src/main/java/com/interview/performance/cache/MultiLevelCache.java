package com.interview.performance.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存实现
 * 
 * L1: 本地缓存（Caffeine/Guava）
 * L2: Redis缓存
 * L3: 数据库
 * 
 * 优点：
 * - 减少网络IO
 * - 提高命中率
 * - 降低延迟
 */
public class MultiLevelCache {
    
    /**
     * 本地LRU缓存
     */
    public static class LocalCache<K, V> {
        private final int maxSize;
        private final Map<K, CacheEntry<V>> cache;
        
        static class CacheEntry<V> {
            private final V value;
            private final long expireTime;
            
            public CacheEntry(V value, long ttl) {
                this.value = value;
                this.expireTime = System.currentTimeMillis() + ttl;
            }
            
            public boolean isExpired() {
                return System.currentTimeMillis() > expireTime;
            }
            
            public V getValue() {
                return value;
            }
        }
        
        public LocalCache(int maxSize) {
            this.maxSize = maxSize;
            // LRU缓存
            this.cache = new LinkedHashMap<K, CacheEntry<V>>(maxSize, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<K, CacheEntry<V>> eldest) {
                    return size() > maxSize;
                }
            };
        }
        
        public V get(K key) {
            CacheEntry<V> entry = cache.get(key);
            if (entry != null && !entry.isExpired()) {
                return entry.getValue();
            }
            cache.remove(key);
            return null;
        }
        
        public void put(K key, V value, long ttl) {
            cache.put(key, new CacheEntry<>(value, ttl));
        }
        
        public void evict(K key) {
            cache.remove(key);
        }
        
        public void clear() {
            cache.clear();
        }
    }
    
    /**
     * 多级缓存管理器
     */
    public static class CacheManager<K, V> {
        private final LocalCache<K, V> l1Cache;
        // private final RedisTemplate<String, V> l2Cache;
        
        public CacheManager(int l1MaxSize) {
            this.l1Cache = new LocalCache<>(l1MaxSize);
        }
        
        /**
         * 获取数据
         */
        public V get(K key) {
            // 1. 从L1缓存获取
            V value = l1Cache.get(key);
            if (value != null) {
                System.out.println("L1 Cache Hit: " + key);
                return value;
            }
            
            // 2. 从L2缓存获取
            value = getFromL2Cache(key);
            if (value != null) {
                System.out.println("L2 Cache Hit: " + key);
                // 回写L1
                l1Cache.put(key, value, TimeUnit.MINUTES.toMillis(5));
                return value;
            }
            
            // 3. 从数据库获取
            value = getFromDatabase(key);
            if (value != null) {
                System.out.println("Database Hit: " + key);
                // 回写L1和L2
                l1Cache.put(key, value, TimeUnit.MINUTES.toMillis(5));
                putToL2Cache(key, value, TimeUnit.HOURS.toMillis(1));
            }
            
            return value;
        }
        
        /**
         * 写入数据
         */
        public void put(K key, V value) {
            // 1. 写数据库
            saveToDatabase(key, value);
            
            // 2. 更新缓存
            l1Cache.put(key, value, TimeUnit.MINUTES.toMillis(5));
            putToL2Cache(key, value, TimeUnit.HOURS.toMillis(1));
        }
        
        /**
         * 删除数据
         */
        public void evict(K key) {
            l1Cache.evict(key);
            evictFromL2Cache(key);
            deleteFromDatabase(key);
        }
        
        // ========== L2缓存操作（Redis） ==========
        
        private V getFromL2Cache(K key) {
            // return l2Cache.opsForValue().get(key.toString());
            return null;  // 模拟
        }
        
        private void putToL2Cache(K key, V value, long ttl) {
            // l2Cache.opsForValue().set(key.toString(), value, ttl, TimeUnit.MILLISECONDS);
        }
        
        private void evictFromL2Cache(K key) {
            // l2Cache.delete(key.toString());
        }
        
        // ========== 数据库操作 ==========
        
        private V getFromDatabase(K key) {
            // 模拟数据库查询
            return null;
        }
        
        private void saveToDatabase(K key, V value) {
            // 模拟数据库保存
            System.out.println("Save to DB: " + key + " = " + value);
        }
        
        private void deleteFromDatabase(K key) {
            // 模拟数据库删除
            System.out.println("Delete from DB: " + key);
        }
    }
    
    /**
     * 缓存预热
     */
    public static class CacheWarmer {
        private final CacheManager<String, Object> cacheManager;
        
        public CacheWarmer(CacheManager<String, Object> cacheManager) {
            this.cacheManager = cacheManager;
        }
        
        /**
         * 预热热点数据
         */
        public void warmUp() {
            // 1. 从数据库加载热点数据
            // 2. 写入缓存
            System.out.println("Warming up cache...");
            
            // 示例：加载TOP 100商品
            for (int i = 1; i <= 100; i++) {
                String key = "product:" + i;
                Object value = loadFromDatabase(key);
                if (value != null) {
                    cacheManager.put(key, value);
                }
            }
            
            System.out.println("Cache warmed up");
        }
        
        private Object loadFromDatabase(String key) {
            return "Product-" + key;  // 模拟
        }
    }
    
    public static void main(String[] args) {
        // 创建缓存管理器
        CacheManager<String, String> cacheManager = new CacheManager<>(100);
        
        // 写入数据
        cacheManager.put("user:1", "张三");
        cacheManager.put("user:2", "李四");
        
        // 读取数据
        System.out.println("\n第一次读取:");
        String user1 = cacheManager.get("user:1");
        System.out.println("Result: " + user1);
        
        System.out.println("\n第二次读取:");
        String user2 = cacheManager.get("user:1");
        System.out.println("Result: " + user2);
        
        // 删除数据
        System.out.println("\n删除数据:");
        cacheManager.evict("user:1");
    }
}

