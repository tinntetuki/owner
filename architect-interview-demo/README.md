# Java架构师面试代码示例

## 项目概述

本项目包含Java架构师/高级开发面试中常见的代码示例和最佳实践。涵盖从基础设计模式到复杂分布式系统的完整技术栈。

## 模块结构

```
architect-interview-demo/
├── design-patterns/      # 设计模式 (7种核心模式 + 适配器、装饰器、外观)
├── concurrent/           # 并发编程 (线程池、锁、原子类、并发集合、同步器)
├── distributed/          # 分布式组件 (分布式锁、ID生成、分布式事务)
├── architecture/         # 架构模式 (DDD、六边形架构、CQRS)
├── performance/          # 性能优化 (多级缓存、批处理、JVM调优)
├── system-design/        # 系统设计 (秒杀、IM、推荐系统、短URL)
└── middleware/           # 中间件集成 (Redis、Kafka、ElasticSearch)
```

## 模块详情

### 1. 设计模式（design-patterns/）

**内容**：
- 单例模式（4种实现：饿汉式、懒汉式、静态内部类、枚举）
- 工厂模式（简单工厂、工厂方法、抽象工厂）
- 建造者模式（Builder Pattern）
- 策略模式（消除if-else）
- 观察者模式（发布-订阅）
- 代理模式（静态代理、动态代理）
- 模板方法模式
- 适配器模式（接口适配）
- 装饰器模式（动态添加功能）
- 外观模式（简化复杂系统）

**运行示例**：
```bash
mvn exec:java -Dexec.mainClass="com.interview.patterns.singleton.SingletonDemo"
mvn exec:java -Dexec.mainClass="com.interview.patterns.adapter.AdapterPatternDemo"
mvn exec:java -Dexec.mainClass="com.interview.patterns.decorator.DecoratorPatternDemo"
```

### 2. 并发编程（concurrent/）

**内容**：
- 自定义线程池（标准、IO密集型、CPU密集型）
- 锁的使用（ReentrantLock、ReadWriteLock、StampedLock）
- 原子类（AtomicInteger、LongAdder、CAS操作）
- 并发集合（ConcurrentHashMap、CopyOnWriteArrayList、BlockingQueue）
- 同步器（CountDownLatch、CyclicBarrier、Semaphore、Exchanger、Phaser）
- 线程池监控和性能调优

**运行示例**：
```bash
mvn exec:java -Dexec.mainClass="com.interview.concurrent.threadpool.CustomThreadPoolDemo"
mvn exec:java -Dexec.mainClass="com.interview.concurrent.collections.ConcurrentCollectionsDemo"
mvn exec:java -Dexec.mainClass="com.interview.concurrent.synchronizers.SynchronizersDemo"
```

### 3. 分布式组件（distributed/）

**内容**：
- Redis分布式锁（Lua脚本保证原子性）
- 分布式ID生成器（Snowflake算法、Leaf号段模式）
- 分布式事务（2PC、TCC、Saga、最终一致性）
- 一致性算法和CAP理论实践

**运行示例**：
```bash
mvn exec:java -Dexec.mainClass="com.interview.distributed.id.DistributedIdGenerator"
mvn exec:java -Dexec.mainClass="com.interview.distributed.lock.RedisDistributedLock"
mvn exec:java -Dexec.mainClass="com.interview.distributed.transaction.DistributedTransactionDemo"
```

### 4. 架构模式（architecture/）

**内容**：
- DDD领域驱动设计（聚合根、实体、值对象、领域服务）
- 六边形架构（端口适配器模式）
- CQRS + EventSourcing
- 微服务拆分策略
- 事件驱动架构

**运行示例**：
```bash
mvn exec:java -Dexec.mainClass="com.interview.architecture.ddd.DDDExample"
```

### 5. 性能优化（performance/）

**内容**：
- 多级缓存（L1本地缓存 + L2Redis + L3数据库）
- 批处理优化（减少网络往返）
- 异步批处理框架
- JVM性能调优（内存监控、GC调优、内存泄漏检测）
- 性能分析工具使用

**运行示例**：
```bash
mvn exec:java -Dexec.mainClass="com.interview.performance.cache.MultiLevelCache"
mvn exec:java -Dexec.mainClass="com.interview.performance.batch.BatchProcessor"
mvn exec:java -Dexec.mainClass="com.interview.performance.jvm.JVMPerformanceDemo"
```

### 6. 系统设计（system-design/）

**内容**：
- 秒杀系统（限流、库存扣减、防刷、MQ削峰）
- 短URL服务（Base62编码、分布式ID）
- 即时通讯系统（消息队列、在线状态、群组管理）
- 推荐系统（协同过滤、内容推荐、混合推荐）
- 令牌桶限流器

**运行示例**：
```bash
mvn exec:java -Dexec.mainClass="com.interview.systemdesign.seckill.SeckillSystem"
mvn exec:java -Dexec.mainClass="com.interview.systemdesign.urlshortener.UrlShortener"
mvn exec:java -Dexec.mainClass="com.interview.systemdesign.im.IMSystemDesign"
mvn exec:java -Dexec.mainClass="com.interview.systemdesign.recommendation.RecommendationSystemDesign"
```

### 7. 中间件集成（middleware/）

**内容**：
- Redis 5种数据结构使用（String、Hash、List、Set、ZSet）
- 排行榜、延迟队列、分布式Session实现
- Kafka生产者/消费者、分区策略、消息确认
- ElasticSearch索引管理、搜索查询、聚合分析
- 中间件性能优化和最佳实践

**运行示例**：
```bash
mvn exec:java -Dexec.mainClass="com.interview.middleware.redis.RedisUsageDemo"
mvn exec:java -Dexec.mainClass="com.interview.middleware.kafka.KafkaUsageDemo"
mvn exec:java -Dexec.mainClass="com.interview.middleware.elasticsearch.ElasticSearchUsageDemo"
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+

### 编译项目

```bash
cd architect-interview-demo
mvn clean compile
```

### 运行示例

```bash
# 运行指定类
mvn exec:java -Dexec.mainClass="完整类名"

# 例如：
mvn exec:java -Dexec.mainClass="com.interview.patterns.singleton.SingletonDemo"
```

## 核心知识点

### 设计模式

- **创建型**：单例（4种实现）、工厂（3种变体）、建造者
- **结构型**：代理（静态/动态）、适配器、装饰器、外观
- **行为型**：策略、观察者、模板方法

### 并发编程

- **线程池**：参数调优、监控、拒绝策略、自定义线程工厂
- **锁机制**：synchronized、ReentrantLock、ReadWriteLock、StampedLock
- **原子类**：CAS原理、AtomicInteger、LongAdder、ABA问题
- **并发集合**：ConcurrentHashMap、CopyOnWriteArrayList、BlockingQueue
- **同步器**：CountDownLatch、CyclicBarrier、Semaphore、Exchanger、Phaser

### 分布式

- **分布式锁**：Redis SET NX EX + Lua脚本、Redisson
- **分布式ID**：Snowflake算法、Leaf号段模式、UUID、Redis INCR
- **分布式事务**：2PC、TCC、Saga、最终一致性
- **一致性**：CAP理论、BASE理论、Raft算法

### 性能优化

- **缓存策略**：多级缓存、缓存穿透/击穿/雪崩、缓存预热
- **批处理**：减少网络往返、提高吞吐量、异步批处理
- **JVM调优**：内存监控、GC调优、内存泄漏检测、性能分析
- **异步处理**：解耦、削峰、提高响应速度

## 面试准备建议

### 短期准备（1-2周）

1. **设计模式**：单例、工厂、代理、策略、观察者
2. **并发编程**：线程池、锁、原子类、并发集合
3. **分布式**：分布式锁、分布式ID、分布式事务
4. **系统设计**：秒杀系统、IM系统、推荐系统

### 中期准备（1-3月）

1. **深入学习**：所有模块的代码示例和原理
2. **实践项目**：基于示例代码构建完整项目
3. **源码阅读**：Spring、MyBatis、Redis等框架源码
4. **性能调优**：JVM调优、数据库优化、缓存优化
5. **架构设计**：微服务架构、DDD实践、事件驱动

### 长期准备（3-6月）

1. **技术深度**：深入理解底层原理和实现机制
2. **项目经验**：总结实际项目中的技术难点和解决方案
3. **技术广度**：大数据、AI/ML、DevOps、云原生
4. **软技能**：技术选型、团队管理、架构评审

## 参考文档

每个模块都有独立的README，包含：
- 核心概念
- 代码示例
- 高频面试题
- 扩展阅读

## 技术栈

### 核心框架
- **Java 17** - 最新LTS版本
- **Spring Boot 3.x** - 微服务框架
- **Spring Cloud Alibaba** - 微服务治理
- **MyBatis Plus** - ORM框架

### 中间件
- **Redis 6.x/7.x** - 缓存和分布式锁
- **Kafka** - 消息队列
- **ElasticSearch** - 搜索引擎
- **MySQL 8.0** - 关系型数据库

### 工具库
- **Guava** - Google核心库
- **Hutool** - Java工具库
- **Lombok** - 代码简化
- **JUnit 5** - 单元测试

## 项目特色

- ✅ **23个技术文档** - 涵盖JVM、并发、分布式、大数据、AI等
- ✅ **7个Java模块** - 从设计模式到系统设计的完整代码示例
- ✅ **3000+行代码** - 可运行的示例代码
- ✅ **200+面试题** - 高频面试题和详细答案
- ✅ **架构图示** - 使用Mermaid绘制的架构图
- ✅ **最佳实践** - 生产环境经验总结

## 许可证

MIT License

## 贡献

欢迎提交Issue和Pull Request来完善这个项目！

## 联系方式

如有问题或建议，请通过GitHub Issues联系。

