# Java架构师面试代码示例

## 项目概述

本项目包含Java架构师/高级开发面试中常见的代码示例和最佳实践。

## 模块结构

```
architect-interview-demo/
├── design-patterns/      # 设计模式
├── concurrent/           # 并发编程
├── distributed/          # 分布式组件
├── architecture/         # 架构模式
├── performance/          # 性能优化
├── system-design/        # 系统设计
└── middleware/           # 中间件集成
```

## 模块详情

### 1. 设计模式（design-patterns/）

**内容**：
- 单例模式（4种实现）
- 工厂模式（简单工厂、工厂方法、抽象工厂）
- 建造者模式（Builder）
- 策略模式（消除if-else）
- 观察者模式（发布-订阅）
- 代理模式（静态代理、动态代理）
- 模板方法模式

**运行示例**：
```bash
mvn exec:java -Dexec.mainClass="com.interview.patterns.singleton.SingletonDemo"
```

### 2. 并发编程（concurrent/）

**内容**：
- 自定义线程池
- 锁的使用（ReentrantLock、ReadWriteLock、StampedLock）
- 原子类（AtomicInteger、LongAdder）
- 线程池监控

**运行示例**：
```bash
mvn exec:java -Dexec.mainClass="com.interview.concurrent.threadpool.CustomThreadPoolDemo"
```

### 3. 分布式组件（distributed/）

**内容**：
- Redis分布式锁
- 分布式ID生成器（Snowflake算法）
- Redisson框架使用

**运行示例**：
```bash
mvn exec:java -Dexec.mainClass="com.interview.distributed.id.DistributedIdGenerator"
```

### 4. 架构模式（architecture/）

**内容**：
- DDD领域驱动设计
- 聚合根、实体、值对象
- 领域服务、仓储

**运行示例**：
```bash
mvn exec:java -Dexec.mainClass="com.interview.architecture.ddd.DDDExample"
```

### 5. 性能优化（performance/）

**内容**：
- 多级缓存（L1本地缓存 + L2Redis）
- 批处理优化
- 异步批处理

**运行示例**：
```bash
mvn exec:java -Dexec.mainClass="com.interview.performance.cache.MultiLevelCache"
```

### 6. 系统设计（system-design/）

**内容**：
- 秒杀系统（限流、库存扣减、防刷）
- 短URL服务（Base62编码）
- 令牌桶限流器

**运行示例**：
```bash
mvn exec:java -Dexec.mainClass="com.interview.systemdesign.seckill.SeckillSystem"
```

### 7. 中间件集成（middleware/）

**内容**：
- Redis 5种数据结构使用
- 排行榜、延迟队列实现
- 分布式Session

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

- **创建型**：单例、工厂、建造者
- **结构型**：代理、适配器、装饰器
- **行为型**：策略、观察者、模板方法

### 并发编程

- **线程池参数**：corePoolSize、maximumPoolSize、workQueue
- **锁机制**：synchronized、ReentrantLock、ReadWriteLock
- **原子类**：CAS、AtomicInteger、LongAdder

### 分布式

- **分布式锁**：Redis SET NX EX + Lua脚本
- **分布式ID**：Snowflake算法、号段模式
- **一致性**：CAP理论、最终一致性

### 性能优化

- **缓存**：多级缓存、缓存穿透/击穿/雪崩
- **批处理**：减少网络往返、提高吞吐量
- **异步**：解耦、削峰

## 面试准备建议

### 短期准备（1-2周）

1. **设计模式**：单例、工厂、代理、策略
2. **并发编程**：线程池、锁、原子类
3. **分布式**：分布式锁、分布式ID
4. **系统设计**：秒杀系统

### 中期准备（1-3月）

1. 完整学习所有模块
2. 实践所有代码示例
3. 理解底层原理
4. 总结项目经验

## 参考文档

每个模块都有独立的README，包含：
- 核心概念
- 代码示例
- 高频面试题
- 扩展阅读

## 技术栈

- Java 17
- Maven 3.x
- 设计模式、并发编程、分布式系统

## 许可证

MIT License

