package com.interview.middleware.redis;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis使用示例
 * 
 * 数据结构：
 * 1. String - 字符串
 * 2. Hash - 哈希
 * 3. List - 列表
 * 4. Set - 集合
 * 5. ZSet - 有序集合
 */
public class RedisUsageDemo {
    
    // private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * String - 缓存对象
     */
    public void stringExample() {
        /*
        // 1. 设置值
        redisTemplate.opsForValue().set("user:1", "张三");
        
        // 2. 设置过期时间
        redisTemplate.opsForValue().set("user:2", "李四", 60, TimeUnit.SECONDS);
        
        // 3. 只在key不存在时设置
        Boolean success = redisTemplate.opsForValue().setIfAbsent("user:3", "王五");
        
        // 4. 获取值
        String user = (String) redisTemplate.opsForValue().get("user:1");
        
        // 5. 自增
        Long count = redisTemplate.opsForValue().increment("page:views", 1);
        */
    }
    
    /**
     * Hash - 存储对象
     */
    public void hashExample() {
        /*
        String key = "user:1001";
        
        // 1. 设置单个字段
        redisTemplate.opsForHash().put(key, "name", "张三");
        redisTemplate.opsForHash().put(key, "age", "25");
        
        // 2. 批量设置
        Map<String, Object> user = new HashMap<>();
        user.put("name", "李四");
        user.put("age", "30");
        user.put("city", "北京");
        redisTemplate.opsForHash().putAll(key, user);
        
        // 3. 获取单个字段
        Object name = redisTemplate.opsForHash().get(key, "name");
        
        // 4. 获取所有字段
        Map<Object, Object> allFields = redisTemplate.opsForHash().entries(key);
        
        // 5. 字段自增
        redisTemplate.opsForHash().increment(key, "age", 1);
        */
    }
    
    /**
     * List - 队列、栈、时间线
     */
    public void listExample() {
        /*
        String key = "message:queue";
        
        // 1. 左侧插入（头部）
        redisTemplate.opsForList().leftPush(key, "msg1");
        redisTemplate.opsForList().leftPush(key, "msg2");
        
        // 2. 右侧插入（尾部）
        redisTemplate.opsForList().rightPush(key, "msg3");
        
        // 3. 批量插入
        redisTemplate.opsForList().rightPushAll(key, "msg4", "msg5", "msg6");
        
        // 4. 获取范围（分页）
        List<Object> messages = redisTemplate.opsForList().range(key, 0, 9);
        
        // 5. 弹出元素
        Object first = redisTemplate.opsForList().leftPop(key);  // 队列
        Object last = redisTemplate.opsForList().rightPop(key);  // 栈
        
        // 6. 阻塞弹出（用于消息队列）
        Object msg = redisTemplate.opsForList().leftPop(key, 10, TimeUnit.SECONDS);
        */
    }
    
    /**
     * Set - 去重、交集、并集
     */
    public void setExample() {
        /*
        String key1 = "tags:user:1";
        String key2 = "tags:user:2";
        
        // 1. 添加元素
        redisTemplate.opsForSet().add(key1, "Java", "Python", "Go");
        redisTemplate.opsForSet().add(key2, "Python", "JavaScript", "Go");
        
        // 2. 获取所有元素
        Set<Object> tags = redisTemplate.opsForSet().members(key1);
        
        // 3. 判断是否存在
        Boolean exists = redisTemplate.opsForSet().isMember(key1, "Java");
        
        // 4. 交集（共同关注）
        Set<Object> intersection = redisTemplate.opsForSet().intersect(key1, key2);
        
        // 5. 并集
        Set<Object> union = redisTemplate.opsForSet().union(key1, key2);
        
        // 6. 差集
        Set<Object> diff = redisTemplate.opsForSet().difference(key1, key2);
        
        // 7. 随机弹出（抽奖）
        Object winner = redisTemplate.opsForSet().pop(key1);
        */
    }
    
    /**
     * ZSet - 排行榜、延迟队列
     */
    public void zsetExample() {
        /*
        String key = "ranking:score";
        
        // 1. 添加元素（score, value）
        redisTemplate.opsForZSet().add(key, "张三", 95);
        redisTemplate.opsForZSet().add(key, "李四", 88);
        redisTemplate.opsForZSet().add(key, "王五", 92);
        
        // 2. 增加分数
        redisTemplate.opsForZSet().incrementScore(key, "张三", 5);
        
        // 3. 获取排名（从0开始）
        Long rank = redisTemplate.opsForZSet().reverseRank(key, "张三");
        
        // 4. 获取Top N（排行榜）
        Set<Object> top10 = redisTemplate.opsForZSet().reverseRange(key, 0, 9);
        
        // 5. 获取分数
        Double score = redisTemplate.opsForZSet().score(key, "张三");
        
        // 6. 按分数范围查询
        Set<Object> range = redisTemplate.opsForZSet().rangeByScore(key, 90, 100);
        
        // 7. 统计数量
        Long count = redisTemplate.opsForZSet().count(key, 90, 100);
        */
    }
    
    /**
     * 实战场景1：分布式Session
     */
    public static class SessionDemo {
        /*
        public void saveSession(String sessionId, Map<String, Object> sessionData) {
            String key = "session:" + sessionId;
            redisTemplate.opsForHash().putAll(key, sessionData);
            redisTemplate.expire(key, 30, TimeUnit.MINUTES);
        }
        
        public Map<Object, Object> getSession(String sessionId) {
            String key = "session:" + sessionId;
            return redisTemplate.opsForHash().entries(key);
        }
        */
    }
    
    /**
     * 实战场景2：排行榜
     */
    public static class RankingDemo {
        /*
        // 更新分数
        public void updateScore(String userId, double score) {
            redisTemplate.opsForZSet().add("ranking:score", userId, score);
        }
        
        // 获取Top N
        public List<Object> getTopN(int n) {
            Set<Object> top = redisTemplate.opsForZSet().reverseRange("ranking:score", 0, n - 1);
            return new ArrayList<>(top);
        }
        
        // 获取用户排名
        public long getUserRank(String userId) {
            Long rank = redisTemplate.opsForZSet().reverseRank("ranking:score", userId);
            return rank != null ? rank + 1 : 0;
        }
        */
    }
    
    /**
     * 实战场景3：延迟队列
     */
    public static class DelayQueueDemo {
        /*
        // 添加延迟任务
        public void addDelayTask(String taskId, long delaySeconds) {
            long executeTime = System.currentTimeMillis() + delaySeconds * 1000;
            redisTemplate.opsForZSet().add("delay:queue", taskId, executeTime);
        }
        
        // 获取到期任务
        public Set<Object> getReadyTasks() {
            long now = System.currentTimeMillis();
            return redisTemplate.opsForZSet().rangeByScore("delay:queue", 0, now);
        }
        
        // 删除任务
        public void removeTask(String taskId) {
            redisTemplate.opsForZSet().remove("delay:queue", taskId);
        }
        */
    }
    
    public static void main(String[] args) {
        System.out.println("Redis使用示例");
        System.out.println("请查看代码注释，需要RedisTemplate依赖");
    }
}

