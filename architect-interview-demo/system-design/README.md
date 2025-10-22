# 系统设计模块

## 概述

本模块包含经典系统设计案例的实现。

## 已实现的内容

### 1. 秒杀系统（seckill/）

**SeckillSystem.java** - 高并发秒杀系统

**架构设计**：
```
用户 → CDN → 网关(限流) → 应用服务器
                ↓
        Redis(库存预扣)
                ↓
             MQ(削峰)
                ↓
          数据库(订单)
```

**核心要点**：

1. **防刷**
   - 用户维度限流
   - Redis SET NX防止重复下单
   - 验证码

2. **高并发**
   - 前端防抖
   - 网关限流（令牌桶）
   - Redis预扣库存
   - 异步下单

3. **防超卖**
   - Lua脚本保证原子性
   - 数据库乐观锁
   - 最终一致性

**代码示例**：
```java
// Lua脚本扣减库存
String script = 
    "if redis.call('get', KEYS[1]) > 0 then " +
    "    redis.call('decr', KEYS[1]); " +
    "    return 1; " +
    "else " +
    "    return 0; " +
    "end";
```

### 2. 短URL服务（urlshortener/）

**UrlShortener.java** - 短链接生成服务

**方案对比**：

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|---------|
| 自增ID+Base62 | 简单、无冲突、短 | 可预测 | 内部系统 |
| Hash | 不可预测 | 可能冲突 | 对外服务 |
| 预生成号段 | 高性能 | 复杂 | 大规模 |

**Base62编码**：
```java
// ID: 10000000 → Base62: "2Bi0U"
String encode(long num) {
    String BASE62 = "0-9a-zA-Z";
    while (num > 0) {
        result = BASE62[num % 62] + result;
        num /= 62;
    }
}
```

**功能**：
- 长URL → 短URL
- 短URL → 长URL（重定向）
- 访问统计

## 系统设计通用思路

### 1. 需求分析
- 功能需求
- 非功能需求（QPS、存储、延迟）
- 约束条件

### 2. 容量估算
- 用户量
- QPS
- 存储空间
- 带宽

### 3. 系统接口
- API设计
- 数据模型

### 4. 架构设计
- 整体架构
- 核心流程
- 数据流向

### 5. 详细设计
- 算法选择
- 数据存储
- 缓存策略
- 扩展性

### 6. 优化与权衡
- 性能优化
- 可用性
- 一致性
- CAP权衡

## 高频面试题

### Q1：秒杀系统如何防止超卖？

**方案1：Redis + Lua脚本**（推荐）
- 原子性扣减库存
- 性能高

**方案2：数据库乐观锁**
```sql
UPDATE product 
SET stock = stock - 1, version = version + 1
WHERE id = ? AND version = ? AND stock > 0
```

**方案3：MQ + 数据库**
- MQ消费保证顺序
- 数据库最终扣减

### Q2：短URL如何设计？

**核心问题**：
1. 如何生成短码？自增ID + Base62
2. 如何保证唯一？数据库自增/分布式ID
3. 如何处理冲突？布隆过滤器预判

**数据量估算**：
- 假设日活1000万，每人生成1个
- 1年 = 1000万 × 365 = 36.5亿
- Base62编码，6位可表示62^6 = 568亿

### Q3：秒杀系统如何设计限流？

**多层限流**：
1. **前端限流**：按钮防抖
2. **网关限流**：Nginx限流模块
3. **应用限流**：令牌桶/漏桶算法
4. **用户限流**：Redis限制用户请求频率

### Q4：如何评估系统容量？

**示例：设计Instagram**
- 5亿用户，日活5000万
- 每人每天发1张照片
- 照片平均2MB

**存储**：
- 每日：5000万 × 2MB = 100TB/天
- 10年：100TB × 365 × 10 = 365PB

**QPS**：
- 读：5000万 × 100次/天 = 50亿/天 = 58000 QPS
- 写：5000万/天 = 578 QPS

## 如何运行

```bash
# 编译
mvn clean compile

# 运行秒杀系统
mvn exec:java -Dexec.mainClass="com.interview.systemdesign.seckill.SeckillSystem"

# 运行短URL服务
mvn exec:java -Dexec.mainClass="com.interview.systemdesign.urlshortener.UrlShortener"
```

## 扩展阅读

- 《系统设计面试》- Alex Xu
- 《大规模分布式存储系统》
- 极客时间《从0开始学架构》
- LeetCode系统设计题

