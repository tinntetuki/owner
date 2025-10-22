# 中间件集成模块

## 概述

本模块展示常用中间件的使用示例。

## 已实现的内容

### 1. Redis使用（redis/）

**RedisUsageDemo.java** - Redis 5种数据结构

#### 数据结构选择

| 数据结构 | 使用场景 | 示例 |
|---------|---------|------|
| String | 缓存对象、计数器 | 用户信息、页面访问量 |
| Hash | 存储对象 | 用户属性、商品详情 |
| List | 队列、时间线 | 消息队列、微博动态 |
| Set | 去重、交集 | 标签、共同关注 |
| ZSet | 排行榜、延迟队列 | 积分排名、定时任务 |

#### 实战场景

**1. 分布式Session**
```java
// 保存Session
String key = "session:" + sessionId;
redisTemplate.opsForHash().putAll(key, sessionData);
redisTemplate.expire(key, 30, TimeUnit.MINUTES);
```

**2. 排行榜**
```java
// 更新分数
redisTemplate.opsForZSet().add("ranking:score", userId, score);

// 获取Top 10
Set<Object> top10 = redisTemplate.opsForZSet().reverseRange("ranking:score", 0, 9);
```

**3. 延迟队列**
```java
// 添加任务
long executeTime = System.currentTimeMillis() + delaySeconds * 1000;
redisTemplate.opsForZSet().add("delay:queue", taskId, executeTime);

// 获取到期任务
Set<Object> tasks = redisTemplate.opsForZSet().rangeByScore("delay:queue", 0, now);
```

## 高频面试题

### Q1：Redis数据结构的底层实现？

| 数据结构 | 底层实现 |
|---------|---------|
| String | SDS（Simple Dynamic String） |
| List | quicklist（双向链表+ziplist） |
| Hash | ziplist或hashtable |
| Set | intset或hashtable |
| ZSet | ziplist或skiplist+hashtable |

### Q2：Redis持久化方式？

**RDB**：
- 全量快照
- 适合备份、恢复快
- 可能丢失数据

**AOF**：
- 追加日志
- 数据更安全
- 文件较大

**混合持久化**（推荐）：
- RDB + AOF
- 兼顾性能和安全

### Q3：Redis过期策略？

1. **定期删除**：每100ms随机抽查
2. **惰性删除**：访问时检查
3. **内存淘汰**：maxmemory-policy
   - noeviction：拒绝写入
   - allkeys-lru：淘汰最少使用
   - volatile-lru：淘汰设置过期的
   - allkeys-random：随机淘汰

### Q4：Redis集群方案？

1. **主从复制**：读写分离
2. **Sentinel**：自动故障转移
3. **Cluster**：分片存储（16384个槽）
4. **Proxy**：Codis、Twemproxy

## 依赖配置

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

## 扩展阅读

- 《Redis设计与实现》
- 《Redis开发与运维》
- Redis官方文档

