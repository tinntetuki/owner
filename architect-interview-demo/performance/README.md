# 性能优化模块

## 概述

本模块包含常见的性能优化技术和最佳实践。

## 已实现的内容

### 1. 多级缓存（cache/）

**MultiLevelCache.java** - 三级缓存架构

**架构**：
```
L1: 本地缓存 (Caffeine/Guava)
    ↓ Miss
L2: Redis缓存
    ↓ Miss
L3: 数据库
```

**优点**：
- L1命中：延迟<1ms
- L2命中：延迟1-5ms
- 减少网络IO
- 提高并发能力

**缓存策略**：
| 策略 | 适用场景 |
|------|---------|
| Cache Aside | 通用（推荐） |
| Read/Write Through | 缓存与DB强一致 |
| Write Behind | 高性能写入 |

**缓存问题**：
1. **缓存穿透**：布隆过滤器 + 空值缓存
2. **缓存击穿**：互斥锁 + 热点数据永不过期
3. **缓存雪崩**：随机过期时间 + 限流降级

### 2. 批处理（batch/）

**BatchProcessor.java** - 批量处理优化

**方案对比**：
| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|---------|
| 简单批处理 | 简单 | 需手动分批 | 离线任务 |
| 异步批处理 | 自动攒批 | 复杂 | 实时写入 |

**异步批处理触发条件**：
1. 达到批量大小（如1000条）
2. 达到等待时间（如100ms）

**代码示例**：
```java
// 批量插入
for (int i = 0; i < total; i += batchSize) {
    List<User> batch = users.subList(i, i + batchSize);
    batchInsert(batch);
}
```

**性能提升**：
- 单条插入：100条/秒
- 批量插入：10000条/秒（提升100倍）

## 性能优化通用思路

### 1. 发现问题
- **监控指标**：QPS、延迟、CPU、内存
- **性能测试**：压测、基准测试
- **性能分析**：jProfiler、Arthas、火焰图

### 2. 定位瓶颈
- **CPU密集**：算法优化
- **IO密集**：异步、缓存
- **网络密集**：批处理、本地化
- **内存密集**：对象复用、流式处理

### 3. 优化方案
| 层次 | 优化手段 |
|------|---------|
| 代码 | 算法、数据结构 |
| JVM | GC调优、线程池 |
| 框架 | 连接池、批处理 |
| 架构 | 缓存、异步、分布式 |
| 基础设施 | SSD、网络升级 |

## 高频面试题

### Q1：如何解决缓存穿透？

**问题**：查询不存在的数据，缓存和DB都没有

**方案**：
1. **布隆过滤器**：快速判断key是否存在
2. **空值缓存**：缓存null值，TTL短

```java
// 布隆过滤器
if (!bloomFilter.contains(key)) {
    return null;
}

// 空值缓存
if (value == null) {
    cache.put(key, NULL_VALUE, 5 * 60);  // 5分钟
}
```

### Q2：如何解决缓存击穿？

**问题**：热点key过期，大量请求打到DB

**方案**：
1. **互斥锁**：只有一个请求去DB
2. **热点数据不过期**：异步更新

```java
String value = cache.get(key);
if (value == null) {
    synchronized (key.intern()) {
        value = cache.get(key);  // 双重检查
        if (value == null) {
            value = db.get(key);
            cache.put(key, value);
        }
    }
}
```

### Q3：如何解决缓存雪崩？

**问题**：大量key同时过期

**方案**：
1. **随机过期时间**：TTL + random(0, 300)
2. **缓存预热**：提前加载
3. **限流降级**：保护DB

### Q4：批处理有哪些注意事项？

1. **批量大小**：过大OOM，过小性能差（建议500-1000）
2. **事务管理**：批量失败如何处理
3. **内存控制**：分批加载，避免一次性加载全部
4. **异常处理**：记录失败数据

### Q5：如何评估性能优化效果？

**关键指标**：
- **QPS**：吞吐量
- **RT**：响应时间（P50/P95/P99）
- **资源使用**：CPU、内存、网络
- **错误率**：成功率

**示例**：
- 优化前：QPS=1000, P99=500ms
- 优化后：QPS=5000, P99=100ms
- 提升：5倍吞吐，5倍延迟降低

## 如何运行

```bash
# 编译
mvn clean compile

# 运行多级缓存示例
mvn exec:java -Dexec.mainClass="com.interview.performance.cache.MultiLevelCache"

# 运行批处理示例
mvn exec:java -Dexec.mainClass="com.interview.performance.batch.BatchProcessor"
```

## 扩展阅读

- 《Java性能权威指南》
- 《高性能MySQL》
- 《Redis设计与实现》
- Caffeine官方文档

