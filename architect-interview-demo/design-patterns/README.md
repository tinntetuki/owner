# 设计模式模块

## 概述

本模块包含23种设计模式的实现和实际应用场景。

## 已实现的设计模式

### 创建型模式（Creational Patterns）

1. **单例模式 (Singleton)** - `singleton/SingletonDemo.java`
   - 饿汉式
   - 懒汉式（双重检查锁）
   - 静态内部类
   - 枚举单例
   - **应用**: 数据库连接池、线程池、配置管理器

2. **工厂模式 (Factory)** - `factory/FactoryPatternDemo.java`
   - 简单工厂
   - 工厂方法
   - 抽象工厂
   - **应用**: Spring BeanFactory、日志框架

3. **建造者模式 (Builder)** - `builder/BuilderPatternDemo.java`
   - 链式调用
   - 参数校验
   - **应用**: StringBuilder、Lombok @Builder、HTTP客户端

### 结构型模式（Structural Patterns）

4. **代理模式 (Proxy)** - `proxy/ProxyPatternDemo.java`
   - 静态代理
   - JDK动态代理
   - **应用**: Spring AOP、RPC框架、MyBatis

### 行为型模式（Behavioral Patterns）

5. **策略模式 (Strategy)** - `strategy/StrategyPatternDemo.java`
   - 消除if-else
   - 策略工厂
   - **应用**: 支付方式、促销策略、排序算法

6. **观察者模式 (Observer)** - `observer/ObserverPatternDemo.java`
   - 发布-订阅
   - **应用**: 事件驱动、消息通知、MVC

7. **模板方法模式 (Template Method)** - `template/TemplateMethodDemo.java`
   - 算法骨架
   - 钩子方法
   - **应用**: Spring JdbcTemplate、数据导入流程

## 如何运行

每个类都包含 `main` 方法，可以直接运行查看效果：

```bash
# 编译
mvn clean compile

# 运行示例
mvn exec:java -Dexec.mainClass="com.interview.patterns.singleton.SingletonDemo"
```

## 设计模式速查表

| 模式 | 类型 | 核心思想 | 使用场景 |
|------|------|---------|---------|
| 单例 | 创建型 | 全局唯一实例 | 连接池、配置 |
| 工厂 | 创建型 | 解耦创建和使用 | 对象创建复杂 |
| 建造者 | 创建型 | 分步骤构建对象 | 参数多、可选参数 |
| 代理 | 结构型 | 控制访问 | AOP、权限、日志 |
| 策略 | 行为型 | 可替换算法 | 消除if-else |
| 观察者 | 行为型 | 一对多依赖 | 事件通知 |
| 模板方法 | 行为型 | 算法骨架 | 流程固定、步骤可变 |

## 面试重点

### 1. 单例模式如何保证线程安全？
- 双重检查锁 + volatile
- 静态内部类（推荐）
- 枚举（最简单）

### 2. 代理模式在Spring中的应用？
- JDK动态代理（接口）
- CGLIB代理（类）
- AOP实现原理

### 3. 策略模式 vs 简单工厂？
- 策略模式：关注行为（算法）
- 简单工厂：关注对象创建

### 4. 观察者模式的应用？
- Spring事件机制
- 消息队列
- 响应式编程（Reactor）

## 扩展阅读

- 《设计模式：可复用面向对象软件的基础》- GoF
- 《Head First 设计模式》
- Spring源码中的设计模式应用

