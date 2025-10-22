# 分布式组件模块

## 概述

本模块包含分布式系统中常用的组件实现。

## 已实现的内容

### 1. 分布式锁（lock/）

**RedisDistributedLock.java** - Redis分布式锁

实现方案：
- **简单版本**：SET NX EX + Lua脚本
- **可重入版本**：HASH结构
- **Redisson框架**：生产推荐

**核心要点**：
```java
// 1. 加锁
SET lock:resource unique_id NX EX 30

// 2. 解锁（Lua脚本保证原子性）
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
else
    return 0
end
```

**常见问题**：
1. **锁超时**：使用WatchDog自动续期（Redisson）
2. **误删锁**：释放锁时验证持有者
3. **主从切换**：使用RedLock算法

### 2. 分布式ID生成器（id/）

**DistributedIdGenerator.java** - 多种ID生成方案

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|---------|
| 数据库自增 | 简单、有序 | 性能差、单点 | 小规模 |
| UUID | 简单、无冲突 | 无序、太长 | 不要求有序 |
| Redis INCR | 高性能 | 依赖Redis | 中等规模 |
| Snowflake | 高性能、趋势递增 | 时钟回拨问题 | 大规模（推荐） |
| Leaf号段 | 高性能、简单 | 依赖DB | 大规模 |

**Snowflake算法**：
```
0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 00000 - 000000000000
1位 |        41位时间戳        | 5位机房 | 5位机器 |    12位序列号
```

特点：
- 趋势递增
- 每秒可生成400万ID
- 41位时间戳可用69年

## 高频面试题

### 1. Redis分布式锁如何实现？

**加锁**：
```
SET lock:resource unique_id NX EX 30
```

**解锁**：
```lua
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
else
    return 0
end
```

### 2. 分布式锁如何防止死锁？

1. **设置过期时间**：EX 30
2. **自动续期**：WatchDog机制（Redisson）
3. **主从切换**：RedLock算法

### 3. Snowflake时钟回拨怎么处理？

1. **拒绝服务**：抛异常
2. **等待**：等到时间追上
3. **回拨位**：增加回拨标识位

### 4. 如何选择分布式ID方案？

| 场景 | 方案 |
|------|------|
| 小规模、简单 | 数据库自增 |
| 不要求有序 | UUID |
| 高性能、趋势递增 | Snowflake |
| 简单可靠 | Leaf号段 |

### 5. RedLock算法是什么？

在多个独立的Redis实例上获取锁，超过半数成功才算加锁成功。

**步骤**：
1. 获取当前时间
2. 依次向N个Redis实例请求锁
3. 超过N/2+1个成功，且时间<锁的有效期，加锁成功
4. 加锁失败，向所有实例释放锁

## 如何运行

```bash
# 编译
mvn clean compile

# 运行分布式ID生成器
mvn exec:java -Dexec.mainClass="com.interview.distributed.id.DistributedIdGenerator"
```

## 扩展阅读

- Redis官方文档：Distributed Locks
- 《从Paxos到Zookeeper》
- 美团Leaf开源项目
- Redisson官方文档

