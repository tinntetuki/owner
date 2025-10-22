# 架构模式模块

## 概述

本模块展示常见架构模式的实现。

## 已实现的内容

### 1. DDD（领域驱动设计）

**DDDExample.java** - 电商订单领域模型

#### 核心概念

**战术设计**：

| 概念 | 说明 | 示例 |
|------|------|------|
| Entity | 有唯一标识的对象 | Order、User |
| Value Object | 无标识、不可变 | Money、Address |
| Aggregate | 相关对象的集合 | Order + OrderItem |
| Aggregate Root | 聚合的入口 | Order |
| Domain Service | 跨实体的业务逻辑 | OrderPricingService |
| Repository | 持久化接口 | OrderRepository |
| Domain Event | 领域事件 | OrderCreatedEvent |

**战略设计**：
- Bounded Context（限界上下文）
- Context Map（上下文映射）
- Ubiquitous Language（统一语言）

#### 代码示例

**值对象**：
```java
class Money {
    private final BigDecimal amount;
    private final String currency;
    
    // 不可变
    public Money add(Money other) {
        return new Money(this.amount.add(other.amount), this.currency);
    }
}
```

**聚合根**：
```java
class Order {  // 聚合根
    private List<OrderItem> items;  // 聚合内的实体
    
    public void addItem(OrderItem item) {
        // 业务规则
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException();
        }
        items.add(item);
    }
}
```

#### 分层架构

```
展现层（Controller）
    ↓
应用层（Application Service）
    ↓
领域层（Domain Model, Domain Service）
    ↓
基础设施层（Repository, External Service）
```

## DDD设计步骤

### 1. 战略设计

1. **划分限界上下文**
   - 订单上下文
   - 商品上下文
   - 用户上下文

2. **识别聚合**
   - 订单聚合（Order + OrderItem）
   - 用户聚合（User + UserProfile）

### 2. 战术设计

1. **识别实体和值对象**
2. **定义聚合边界**
3. **设计领域服务**
4. **定义仓储接口**

### 3. 实现

1. **领域层**：纯业务逻辑
2. **应用层**：编排领域对象
3. **基础设施层**：技术实现

## 高频面试题

### Q1：实体和值对象的区别？

| 特性 | Entity | Value Object |
|------|--------|--------------|
| 标识 | 有唯一标识 | 无标识 |
| 可变性 | 可变 | 不可变 |
| 生命周期 | 长 | 短 |
| 示例 | User、Order | Money、Address |

### Q2：聚合根的作用？

1. **保证一致性**：聚合内的业务规则
2. **统一入口**：外部只能通过聚合根访问
3. **事务边界**：一个聚合一个事务

### Q3：贫血模型 vs 充血模型？

**贫血模型**：
```java
class Order {
    private Long id;
    // 只有getter/setter，无业务逻辑
}

class OrderService {
    public void pay(Order order) {
        // 业务逻辑在Service
    }
}
```

**充血模型**（DDD推荐）：
```java
class Order {
    public void pay() {
        // 业务逻辑在领域对象
        if (status != SUBMITTED) {
            throw new IllegalStateException();
        }
        status = PAID;
    }
}
```

### Q4：DDD的优缺点？

**优点**：
- 业务逻辑清晰
- 易于维护和扩展
- 适合复杂业务

**缺点**：
- 学习曲线陡峭
- 设计成本高
- 不适合简单CRUD

### Q5：什么时候使用DDD？

**适用**：
- 复杂业务逻辑
- 长期维护的系统
- 业务变化频繁

**不适用**：
- 简单CRUD
- 数据驱动的系统
- 短期项目

## 最佳实践

1. **聚合不宜过大**：一般不超过3层
2. **使用工厂创建复杂对象**
3. **值对象优先于实体**
4. **不要跨聚合引用实体**：使用ID引用
5. **领域事件解耦聚合**

## 扩展阅读

- 《领域驱动设计》- Eric Evans
- 《实现领域驱动设计》- Vaughn Vernon
- 《DDD实战课》- 欧创新

