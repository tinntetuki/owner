# Redis实战深度解析

## 目录
- [一、数据结构](#一数据结构)
- [二、持久化](#二持久化)
- [三、集群方案](#三集群方案)
- [四、缓存策略](#四缓存策略)
- [五、实战场景](#五实战场景)
- [六、性能优化](#六性能优化)
- [七、高频面试题](#七高频面试题)

## 一、数据结构

### 1.1 String

**底层实现**：SDS（Simple Dynamic String）

**常用命令**：
```redis
SET key value [EX seconds] [NX|XX]
GET key
INCR key
DECR key
MGET key1 key2
MSET key1 value1 key2 value2
```

**应用场景**：
```java
// 1. 计数器
jedis.incr("article:1001:view_count");

// 2. 分布式锁
String lockKey = "lock:order:1001";
String lockValue = UUID.randomUUID().toString();
Boolean result = jedis.set(lockKey, lockValue, "NX", "EX", 30);

// 3. 缓存对象
User user = userService.getUser(1001L);
jedis.setex("user:1001", 3600, JSON.toJSONString(user));
```

### 1.2 Hash

**常用命令**：
```redis
HSET key field value
HGET key field
HMGET key field1 field2
HGETALL key
HINCRBY key field increment
```

**应用场景**：
```java
// 存储对象
Map<String, String> user = new HashMap<>();
user.put("name", "John");
user.put("age", "30");
jedis.hmset("user:1001", user);

// 购物车
jedis.hincrby("cart:user:1001", "product:2001", 1);  // 增加商品数量
```

### 1.3 List

**底层实现**：QuickList（ziplist + linkedlist）

**常用命令**：
```redis
LPUSH key value
RPUSH key value
LPOP key
RPOP key
LRANGE key start stop
LTRIM key start stop
```

**应用场景**：
```java
// 1. 消息队列
jedis.lpush("queue:task", task);
String task = jedis.rpop("queue:task");

// 2. 最新列表
jedis.lpush("news:latest", newsId);
jedis.ltrim("news:latest", 0, 99);  // 保留最新100条
List<String> latest = jedis.lrange("news:latest", 0, 9);  // 取前10条
```

### 1.4 Set

**常用命令**：
```redis
SADD key member
SREM key member
SISMEMBER key member
SCARD key
SINTER key1 key2        # 交集
SUNION key1 key2        # 并集
SDIFF key1 key2         # 差集
```

**应用场景**：
```java
// 1. 标签系统
jedis.sadd("user:1001:tags", "java", "redis", "mysql");

// 2. 共同好友
jedis.sinter("user:1001:friends", "user:1002:friends");

// 3. 去重
jedis.sadd("article:1001:read_users", "user:1001");
```

### 1.5 Sorted Set

**底层实现**：skiplist + hashtable

**常用命令**：
```redis
ZADD key score member
ZRANGE key start stop [WITHSCORES]
ZREVRANGE key start stop
ZINCRBY key increment member
ZRANK key member
```

**应用场景**：
```java
// 1. 排行榜
jedis.zincrby("rank:game:score", 100, "user:1001");
Set<String> top10 = jedis.zrevrange("rank:game:score", 0, 9);

// 2. 延迟队列
long delayTime = System.currentTimeMillis() + 60000;
jedis.zadd("delay:queue", delayTime, taskId);

// 获取到期任务
Set<String> tasks = jedis.zrangeByScore("delay:queue", 0, System.currentTimeMillis());

// 3. 限流（滑动窗口）
String key = "rate:limit:user:1001";
long now = System.currentTimeMillis();
jedis.zadd(key, now, UUID.randomUUID().toString());
jedis.zremrangeByScore(key, 0, now - 60000);  // 删除1分钟前的
long count = jedis.zcard(key);
if (count > 100) {
    // 限流
}
```

## 二、持久化

### 2.1 RDB（快照）

**配置**：
```redis
save 900 1      # 900秒内至少1次修改
save 300 10     # 300秒内至少10次修改
save 60 10000   # 60秒内至少10000次修改
```

**手动触发**：
```redis
SAVE      # 阻塞
BGSAVE    # 后台保存
```

**优缺点**：
- **优点**：恢复快、文件紧凑
- **缺点**：可能丢失数据、fork子进程消耗资源

### 2.2 AOF（追加日志）

**配置**：
```redis
appendonly yes
appendfsync always      # 每次写入都sync
appendfsync everysec    # 每秒sync（推荐）
appendfsync no          # 由OS决定
```

**AOF重写**：
```redis
AUTO-AOF-REWRITE-PERCENTAGE 100
AUTO-AOF-REWRITE-MIN-SIZE 64mb

# 手动触发
BGREWRITEAOF
```

**优缺点**：
- **优点**：数据安全、可读性好
- **缺点**：文件大、恢复慢

### 2.3 混合持久化（Redis 4.0+）

```redis
aof-use-rdb-preamble yes
```

**原理**：
- AOF文件前半部分是RDB格式
- 后半部分是AOF格式
- 结合两者优点

## 三、集群方案

### 3.1 主从复制

**配置**：
```redis
# Slave配置
replicaof 192.168.1.10 6379
masterauth password
```

**复制流程**：
```
1. Slave发送PSYNC命令
2. Master执行BGSAVE生成RDB
3. Master发送RDB给Slave
4. Master发送缓冲区的写命令
5. Slave加载RDB并执行命令
```

### 3.2 Sentinel（哨兵）

**配置**：
```redis
# sentinel.conf
sentinel monitor mymaster 192.168.1.10 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel parallel-syncs mymaster 1
sentinel failover-timeout mymaster 180000
```

**工作原理**：
1. **监控**：检测Master和Slave是否正常
2. **通知**：通知管理员故障
3. **故障转移**：选举新Master
4. **配置提供**：提供Master地址

### 3.3 Cluster（集群）

**特点**：
- 数据分片（16384个slot）
- 去中心化
- 支持水平扩展

**创建集群**：
```bash
redis-cli --cluster create \
    192.168.1.10:6379 192.168.1.11:6379 192.168.1.12:6379 \
    192.168.1.10:6380 192.168.1.11:6380 192.168.1.12:6380 \
    --cluster-replicas 1
```

**Slot计算**：
```
slot = CRC16(key) % 16384
```

**Java客户端**：
```java
Set<HostAndPort> nodes = new HashSet<>();
nodes.add(new HostAndPort("192.168.1.10", 6379));
nodes.add(new HostAndPort("192.168.1.11", 6379));
nodes.add(new HostAndPort("192.168.1.12", 6379));

JedisCluster cluster = new JedisCluster(nodes);
cluster.set("key", "value");
```

## 四、缓存策略

### 4.1 缓存更新策略

**Cache Aside**（推荐）：
```java
// 读
public User getUser(Long id) {
    String key = "user:" + id;
    String cached = jedis.get(key);
    if (cached != null) {
        return JSON.parseObject(cached, User.class);
    }
    
    User user = userMapper.selectById(id);
    if (user != null) {
        jedis.setex(key, 3600, JSON.toJSONString(user));
    }
    return user;
}

// 写
public void updateUser(User user) {
    userMapper.updateById(user);
    jedis.del("user:" + user.getId());  // 删除缓存
}
```

### 4.2 缓存问题

**1. 缓存穿透**：
```java
// 布隆过滤器
@Component
public class BloomFilterService {
    private BloomFilter<Long> bloomFilter = BloomFilter.create(
        Funnels.longFunnel(),
        100000000,
        0.01
    );
    
    public User getUser(Long id) {
        if (!bloomFilter.mightContain(id)) {
            return null;  // 一定不存在
        }
        // 查询缓存和数据库
    }
}

// 缓存空值
public User getUser(Long id) {
    String key = "user:" + id;
    String cached = jedis.get(key);
    
    if (cached != null) {
        return "null".equals(cached) ? null : JSON.parseObject(cached, User.class);
    }
    
    User user = userMapper.selectById(id);
    if (user != null) {
        jedis.setex(key, 3600, JSON.toJSONString(user));
    } else {
        jedis.setex(key, 60, "null");  // 缓存空值，短过期
    }
    return user;
}
```

**2. 缓存击穿**：
```java
// 互斥锁
public User getUser(Long id) {
    String key = "user:" + id;
    String cached = jedis.get(key);
    if (cached != null) {
        return JSON.parseObject(cached, User.class);
    }
    
    String lockKey = "lock:user:" + id;
    try {
        if (jedis.setnx(lockKey, "1") == 1) {
            jedis.expire(lockKey, 10);
            
            User user = userMapper.selectById(id);
            jedis.setex(key, 3600, JSON.toJSONString(user));
            return user;
        } else {
            Thread.sleep(100);
            return getUser(id);  // 重试
        }
    } finally {
        jedis.del(lockKey);
    }
}
```

**3. 缓存雪崩**：
```java
// 随机过期时间
int expireTime = 3600 + new Random().nextInt(600);  // 3600~4200秒
jedis.setex(key, expireTime, value);

// 永不过期 + 异步更新
jedis.set(key, value);  // 不设置过期时间

// 定时任务更新
@Scheduled(fixedRate = 3000000)
public void refreshCache() {
    // 更新热点数据
}
```

## 五、实战场景

### 5.1 分布式锁

**Redisson实现**：
```java
@Component
public class RedisLockService {
    
    @Autowired
    private RedissonClient redissonClient;
    
    public void doSomething() {
        RLock lock = redissonClient.getLock("myLock");
        
        try {
            if (lock.tryLock(100, 10, TimeUnit.SECONDS)) {
                // 业务逻辑
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### 5.2 限流

**滑动窗口限流**：
```java
public boolean checkLimit(String userId, int limit, int window) {
    String key = "rate:limit:" + userId;
    long now = System.currentTimeMillis();
    
    // 添加当前时间戳
    jedis.zadd(key, now, UUID.randomUUID().toString());
    
    // 删除窗口外的数据
    jedis.zremrangeByScore(key, 0, now - window * 1000);
    
    // 统计窗口内的请求数
    long count = jedis.zcard(key);
    
    // 设置过期时间
    jedis.expire(key, window);
    
    return count <= limit;
}
```

### 5.3 排行榜

```java
@Service
public class RankService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    // 增加分数
    public void addScore(Long userId, double score) {
        String key = "rank:game";
        redisTemplate.opsForZSet().incrementScore(key, String.valueOf(userId), score);
    }
    
    // 获取排名
    public Long getRank(Long userId) {
        String key = "rank:game";
        return redisTemplate.opsForZSet().reverseRank(key, String.valueOf(userId));
    }
    
    // 获取Top N
    public Set<String> getTopN(int n) {
        String key = "rank:game";
        return redisTemplate.opsForZSet().reverseRange(key, 0, n - 1);
    }
}
```

## 六、性能优化

### 6.1 慢查询

**配置**：
```redis
CONFIG SET slowlog-log-slower-than 10000  # 10ms
CONFIG SET slowlog-max-len 128

# 查看慢查询
SLOWLOG GET 10
```

### 6.2 Pipeline

```java
// 普通方式
for (int i = 0; i < 10000; i++) {
    jedis.set("key:" + i, "value:" + i);  // 10000次网络往返
}

// Pipeline方式
Pipeline pipeline = jedis.pipelined();
for (int i = 0; i < 10000; i++) {
    pipeline.set("key:" + i, "value:" + i);
}
pipeline.sync();  // 1次网络往返
```

### 6.3 内存优化

**1. 选择合适的数据结构**：
```redis
# Hash比String节省内存
HSET user:1001 name John
HSET user:1001 age 30

# 比
SET user:1001:name John
SET user:1001:age 30
```

**2. 设置过期时间**：
```java
jedis.setex(key, 3600, value);
```

**3. 内存淘汰策略**：
```redis
maxmemory 2gb
maxmemory-policy allkeys-lru  # LRU淘汰
```

## 七、高频面试题

### Q1：Redis为什么快？

1. **内存存储**：内存读写速度快
2. **单线程**：避免上下文切换和竞争
3. **IO多路复用**：高效处理并发连接
4. **优化的数据结构**：SDS、ziplist等

### Q2：Redis单线程如何处理并发？

- **IO多路复用**（epoll/select）
- 单线程处理请求，避免锁竞争
- Redis 6.0引入多线程处理网络IO

### Q3：RDB和AOF的区别？

| 特性 | RDB | AOF |
|------|-----|-----|
| 持久化方式 | 快照 | 追加日志 |
| 恢复速度 | 快 | 慢 |
| 数据安全 | 可能丢失 | 更安全 |
| 文件大小 | 小 | 大 |

### Q4：如何保证Redis和MySQL数据一致性？

**方案1：延迟双删**：
```java
public void updateUser(User user) {
    jedis.del("user:" + user.getId());  // 删除缓存
    userMapper.updateById(user);
    Thread.sleep(500);
    jedis.del("user:" + user.getId());  // 再次删除
}
```

**方案2：Canal监听binlog**：
```
MySQL binlog → Canal → MQ → 更新Redis
```

### Q5：Redis集群如何扩容？

**Cluster扩容**：
```bash
# 1. 启动新节点
redis-server --port 6383

# 2. 加入集群
redis-cli --cluster add-node 192.168.1.13:6383 192.168.1.10:6379

# 3. 重新分配slot
redis-cli --cluster reshard 192.168.1.10:6379
```

---

**关键字**：Redis、数据结构、持久化、集群、缓存、分布式锁、Redisson

