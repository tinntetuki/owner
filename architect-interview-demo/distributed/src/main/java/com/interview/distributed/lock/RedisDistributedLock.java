package com.interview.distributed.lock;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis分布式锁实现
 * 
 * 核心要点：
 * 1. 互斥性：任何时刻只有一个客户端持有锁
 * 2. 不死锁：即使持有锁的客户端崩溃，锁最终也能释放
 * 3. 容错性：只要大多数Redis节点正常，就能正常工作
 * 
 * 实现方案：
 * - SET key value NX EX 30
 * - Lua脚本保证原子性
 * - Redisson框架（推荐生产使用）
 */
public class RedisDistributedLock {
    
    /**
     * 简单版本 - 使用SET NX EX
     */
    public static class SimpleRedisLock {
        // private RedisTemplate<String, String> redisTemplate;
        
        private static final String LOCK_PREFIX = "lock:";
        private static final long DEFAULT_EXPIRE_TIME = 30000;  // 30秒
        
        /**
         * 获取锁
         */
        public boolean tryLock(String key, String requestId, long expireTime) {
            String lockKey = LOCK_PREFIX + key;
            
            // SET key value NX EX 30
            // 伪代码：实际需要RedisTemplate
            // Boolean result = redisTemplate.opsForValue()
            //     .setIfAbsent(lockKey, requestId, expireTime, TimeUnit.MILLISECONDS);
            
            // 模拟返回
            return true;
        }
        
        /**
         * 释放锁 - 使用Lua脚本保证原子性
         */
        public boolean unlock(String key, String requestId) {
            String lockKey = LOCK_PREFIX + key;
            
            // Lua脚本：检查锁的持有者是否是自己
            String script = 
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "    return redis.call('del', KEYS[1]) " +
                "else " +
                "    return 0 " +
                "end";
            
            // 执行Lua脚本
            // Long result = redisTemplate.execute(
            //     new DefaultRedisScript<>(script, Long.class),
            //     Collections.singletonList(lockKey),
            //     requestId
            // );
            
            return true;  // 模拟返回
        }
    }
    
    /**
     * 完整版本 - 可重入锁
     * 
     * 原理：使用Hash存储
     * key: lock:resource
     * field: thread_id
     * value: reentrant_count
     */
    public static class ReentrantRedisLock {
        
        /**
         * 加锁 - Lua脚本
         */
        private static final String LOCK_SCRIPT = 
            "if redis.call('exists', KEYS[1]) == 0 then " +
            "    redis.call('hincrby', KEYS[1], ARGV[1], 1); " +
            "    redis.call('pexpire', KEYS[1], ARGV[2]); " +
            "    return 1; " +
            "end; " +
            "if redis.call('hexists', KEYS[1], ARGV[1]) == 1 then " +
            "    redis.call('hincrby', KEYS[1], ARGV[1], 1); " +
            "    redis.call('pexpire', KEYS[1], ARGV[2]); " +
            "    return 1; " +
            "end; " +
            "return 0;";
        
        /**
         * 解锁 - Lua脚本
         */
        private static final String UNLOCK_SCRIPT = 
            "if redis.call('hexists', KEYS[1], ARGV[1]) == 0 then " +
            "    return nil; " +
            "end; " +
            "local counter = redis.call('hincrby', KEYS[1], ARGV[1], -1); " +
            "if counter > 0 then " +
            "    return 0; " +
            "else " +
            "    redis.call('del', KEYS[1]); " +
            "    return 1; " +
            "end; " +
            "return nil;";
        
        public boolean lock(String key, long expireTime) {
            String threadId = getThreadId();
            // 执行Lua脚本
            return true;
        }
        
        public boolean unlock(String key) {
            String threadId = getThreadId();
            // 执行Lua脚本
            return true;
        }
        
        private String getThreadId() {
            return Thread.currentThread().getId() + "";
        }
    }
    
    /**
     * 使用示例
     */
    public static class LockUsageExample {
        
        public void processOrder(String orderId) {
            String lockKey = "order:" + orderId;
            String requestId = UUID.randomUUID().toString();
            
            SimpleRedisLock lock = new SimpleRedisLock();
            
            try {
                // 1. 尝试获取锁
                if (!lock.tryLock(lockKey, requestId, 30000)) {
                    System.out.println("获取锁失败，订单正在处理中");
                    return;
                }
                
                // 2. 业务逻辑
                System.out.println("处理订单: " + orderId);
                Thread.sleep(1000);
                
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // 3. 释放锁
                lock.unlock(lockKey, requestId);
            }
        }
    }
    
    /**
     * Redisson框架使用（推荐）
     * 
     * <dependency>
     *     <groupId>org.redisson</groupId>
     *     <artifactId>redisson</artifactId>
     * </dependency>
     */
    public static class RedissonLockExample {
        /*
        @Autowired
        private RedissonClient redissonClient;
        
        public void useRedissonLock() {
            RLock lock = redissonClient.getLock("myLock");
            
            try {
                // 1. 尝试加锁，最多等待10秒，锁30秒后自动释放
                boolean locked = lock.tryLock(10, 30, TimeUnit.SECONDS);
                
                if (locked) {
                    // 2. 业务逻辑
                    System.out.println("处理业务");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                // 3. 释放锁
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
        */
    }
    
    public static void main(String[] args) {
        LockUsageExample example = new LockUsageExample();
        example.processOrder("ORDER001");
    }
}

