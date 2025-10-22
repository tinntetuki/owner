package com.interview.distributed.id;

import java.net.InetAddress;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 分布式ID生成器
 * 
 * 常见方案：
 * 1. 数据库自增ID
 * 2. UUID
 * 3. Redis INCR
 * 4. Snowflake算法
 * 5. 美团Leaf
 * 6. 百度UidGenerator
 */
public class DistributedIdGenerator {
    
    /**
     * Snowflake算法（雪花算法）
     * 
     * 结构：
     * 0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 00000 - 000000000000
     * 1位符号位 | 41位时间戳 | 5位机房ID | 5位机器ID | 12位序列号
     * 
     * 特点：
     * - 趋势递增
     * - 不依赖数据库
     * - 高性能（每秒可生成400万ID）
     * - 41位时间戳可用69年
     */
    public static class SnowflakeIdGenerator {
        
        // 起始时间戳（2024-01-01 00:00:00）
        private static final long START_TIMESTAMP = 1704038400000L;
        
        // 各部分占用位数
        private static final long DATACENTER_ID_BITS = 5L;
        private static final long WORKER_ID_BITS = 5L;
        private static final long SEQUENCE_BITS = 12L;
        
        // 最大值
        private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
        private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
        private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);
        
        // 位移
        private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
        private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
        private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
        
        private final long datacenterId;
        private final long workerId;
        private long sequence = 0L;
        private long lastTimestamp = -1L;
        
        public SnowflakeIdGenerator(long datacenterId, long workerId) {
            if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
                throw new IllegalArgumentException("datacenter Id can't be greater than " + MAX_DATACENTER_ID + " or less than 0");
            }
            if (workerId > MAX_WORKER_ID || workerId < 0) {
                throw new IllegalArgumentException("worker Id can't be greater than " + MAX_WORKER_ID + " or less than 0");
            }
            this.datacenterId = datacenterId;
            this.workerId = workerId;
        }
        
        /**
         * 生成ID（线程安全）
         */
        public synchronized long nextId() {
            long timestamp = System.currentTimeMillis();
            
            // 时钟回拨检测
            if (timestamp < lastTimestamp) {
                throw new RuntimeException("Clock moved backwards. Refusing to generate id");
            }
            
            // 同一毫秒内
            if (timestamp == lastTimestamp) {
                sequence = (sequence + 1) & MAX_SEQUENCE;
                // 序列号用完，等待下一毫秒
                if (sequence == 0) {
                    timestamp = waitNextMillis(lastTimestamp);
                }
            } else {
                sequence = 0L;
            }
            
            lastTimestamp = timestamp;
            
            // 组装ID
            return ((timestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT) |
                   (datacenterId << DATACENTER_ID_SHIFT) |
                   (workerId << WORKER_ID_SHIFT) |
                   sequence;
        }
        
        private long waitNextMillis(long lastTimestamp) {
            long timestamp = System.currentTimeMillis();
            while (timestamp <= lastTimestamp) {
                timestamp = System.currentTimeMillis();
            }
            return timestamp;
        }
    }
    
    /**
     * 美团Leaf - 号段模式
     * 
     * 原理：
     * 1. 从数据库批量获取号段（如1-1000）
     * 2. 内存中分配ID
     * 3. 用完后再获取下一个号段
     * 
     * 优点：
     * - 减少数据库访问
     * - 高性能
     * - 简单可靠
     */
    public static class LeafSegmentGenerator {
        private long currentId;
        private long maxId;
        private final Object lock = new Object();
        
        public long nextId() {
            synchronized (lock) {
                if (currentId >= maxId) {
                    // 获取新号段
                    allocateSegment();
                }
                return ++currentId;
            }
        }
        
        private void allocateSegment() {
            // 从数据库获取新号段
            // UPDATE id_generator SET current_value = current_value + step 
            // WHERE biz_type = 'order' RETURNING current_value, step
            
            // 模拟获取号段 [1000, 2000)
            currentId = 1000;
            maxId = 2000;
        }
    }
    
    /**
     * UUID - 简单但无序
     */
    public static class UUIDGenerator {
        public static String nextId() {
            return java.util.UUID.randomUUID().toString().replace("-", "");
        }
    }
    
    /**
     * Redis INCR - 简单高效
     */
    public static class RedisIdGenerator {
        /*
        @Autowired
        private RedisTemplate<String, String> redisTemplate;
        
        public long nextId(String bizType) {
            String key = "id:" + bizType + ":" + LocalDate.now();
            Long id = redisTemplate.opsForValue().increment(key, 1);
            redisTemplate.expire(key, 7, TimeUnit.DAYS);
            return id;
        }
        */
    }
    
    /**
     * 性能测试
     */
    public static void performanceTest() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        
        long start = System.currentTimeMillis();
        int count = 1_000_000;
        
        for (int i = 0; i < count; i++) {
            generator.nextId();
        }
        
        long end = System.currentTimeMillis();
        System.out.println("生成 " + count + " 个ID，耗时: " + (end - start) + "ms");
        System.out.println("TPS: " + (count * 1000 / (end - start)));
    }
    
    public static void main(String[] args) {
        // Snowflake示例
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        
        System.out.println("========== Snowflake ID ==========");
        for (int i = 0; i < 5; i++) {
            long id = generator.nextId();
            System.out.println("ID: " + id);
        }
        
        // UUID示例
        System.out.println("\n========== UUID ==========");
        for (int i = 0; i < 3; i++) {
            System.out.println("UUID: " + UUIDGenerator.nextId());
        }
        
        // 性能测试
        System.out.println("\n========== 性能测试 ==========");
        performanceTest();
    }
}

