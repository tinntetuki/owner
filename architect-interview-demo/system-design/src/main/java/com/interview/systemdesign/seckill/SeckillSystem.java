package com.interview.systemdesign.seckill;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 秒杀系统设计
 * 
 * 核心挑战：
 * 1. 高并发（10万+ QPS）
 * 2. 库存准确（防止超卖）
 * 3. 防刷（同一用户重复下单）
 * 
 * 解决方案：
 * 1. 前端：按钮防抖、验证码
 * 2. 网关：限流
 * 3. Redis：库存预扣减
 * 4. MQ：削峰填谷
 * 5. DB：分库分表
 */
public class SeckillSystem {
    
    /**
     * 秒杀服务 - 核心流程
     */
    public static class SeckillService {
        // private RedisTemplate<String, Object> redisTemplate;
        // private RocketMQTemplate rocketMQTemplate;
        
        /**
         * 秒杀接口
         * 
         * @param userId 用户ID
         * @param productId 商品ID
         * @return 秒杀结果
         */
        public Result seckill(Long userId, Long productId) {
            // 1. 参数校验
            if (userId == null || productId == null) {
                return Result.fail("参数错误");
            }
            
            // 2. 检查用户是否已经秒杀过（防刷）
            String userKey = "seckill:user:" + productId + ":" + userId;
            // Boolean hasOrdered = redisTemplate.opsForValue().setIfAbsent(userKey, "1", 1, TimeUnit.HOURS);
            // if (!hasOrdered) {
            //     return Result.fail("您已参与过此活动");
            // }
            
            // 3. Redis预扣库存（Lua脚本保证原子性）
            String stockKey = "seckill:stock:" + productId;
            Long stock = decrStock(stockKey);
            if (stock == null || stock < 0) {
                return Result.fail("库存不足");
            }
            
            // 4. 发送MQ异步创建订单
            SeckillMessage message = new SeckillMessage(userId, productId);
            // rocketMQTemplate.asyncSend("seckill-topic", message);
            
            return Result.success("秒杀成功，正在生成订单");
        }
        
        /**
         * Lua脚本扣减库存
         */
        private Long decrStock(String stockKey) {
            String script = 
                "if redis.call('exists', KEYS[1]) == 1 then " +
                "    local stock = tonumber(redis.call('get', KEYS[1])); " +
                "    if stock > 0 then " +
                "        redis.call('decr', KEYS[1]); " +
                "        return stock - 1; " +
                "    else " +
                "        return -1; " +
                "    end " +
                "else " +
                "    return -1; " +
                "end";
            
            // Long result = redisTemplate.execute(
            //     new DefaultRedisScript<>(script, Long.class),
            //     Collections.singletonList(stockKey)
            // );
            
            return 100L;  // 模拟返回
        }
    }
    
    /**
     * 秒杀消息
     */
    static class SeckillMessage {
        private Long userId;
        private Long productId;
        
        public SeckillMessage(Long userId, Long productId) {
            this.userId = userId;
            this.productId = productId;
        }
        
        // getters and setters
    }
    
    /**
     * MQ消费者 - 创建订单
     */
    public static class SeckillOrderConsumer {
        /*
        @RocketMQMessageListener(
            topic = "seckill-topic",
            consumerGroup = "seckill-order-consumer"
        )
        public class OrderListener implements RocketMQListener<SeckillMessage> {
            
            @Override
            public void onMessage(SeckillMessage message) {
                try {
                    // 1. 创建订单
                    createOrder(message);
                    
                    // 2. 扣减真实库存
                    deductStock(message.getProductId());
                    
                } catch (Exception e) {
                    // 3. 失败回滚Redis库存
                    rollbackStock(message.getProductId());
                    throw e;
                }
            }
        }
        */
        
        private void createOrder(SeckillMessage message) {
            System.out.println("创建订单: userId=" + message.userId + ", productId=" + message.productId);
        }
        
        private void deductStock(Long productId) {
            System.out.println("扣减库存: productId=" + productId);
        }
        
        private void rollbackStock(Long productId) {
            System.out.println("回滚库存: productId=" + productId);
        }
    }
    
    /**
     * 限流器 - 令牌桶算法
     */
    public static class RateLimiter {
        private final int capacity;         // 桶容量
        private final int rate;             // 令牌生成速率（个/秒）
        private final AtomicInteger tokens; // 当前令牌数
        private long lastRefillTime;        // 上次填充时间
        
        public RateLimiter(int capacity, int rate) {
            this.capacity = capacity;
            this.rate = rate;
            this.tokens = new AtomicInteger(capacity);
            this.lastRefillTime = System.currentTimeMillis();
        }
        
        /**
         * 尝试获取令牌
         */
        public synchronized boolean tryAcquire() {
            refill();
            
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }
        
        /**
         * 填充令牌
         */
        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTime;
            
            if (elapsed > 0) {
                int newTokens = (int) (elapsed * rate / 1000);
                if (newTokens > 0) {
                    tokens.set(Math.min(capacity, tokens.get() + newTokens));
                    lastRefillTime = now;
                }
            }
        }
    }
    
    /**
     * 结果类
     */
    static class Result {
        private boolean success;
        private String message;
        
        public static Result success(String message) {
            Result result = new Result();
            result.success = true;
            result.message = message;
            return result;
        }
        
        public static Result fail(String message) {
            Result result = new Result();
            result.success = false;
            result.message = message;
            return result;
        }
        
        @Override
        public String toString() {
            return "Result{success=" + success + ", message='" + message + "'}";
        }
    }
    
    /**
     * 测试
     */
    public static void main(String[] args) throws InterruptedException {
        SeckillService service = new SeckillService();
        RateLimiter rateLimiter = new RateLimiter(100, 10);  // 容量100，每秒10个
        
        // 模拟100个用户秒杀
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(100);
        
        for (int i = 0; i < 100; i++) {
            final long userId = i;
            executor.submit(() -> {
                try {
                    // 限流
                    if (!rateLimiter.tryAcquire()) {
                        System.out.println("用户" + userId + " - 请求被限流");
                        return;
                    }
                    
                    // 秒杀
                    Result result = service.seckill(userId, 1001L);
                    System.out.println("用户" + userId + " - " + result);
                    
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
    }
}

