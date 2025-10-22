# 并发编程模块

## 概述

本模块包含Java并发编程的核心内容和实战示例。

## 已实现的内容

### 1. 线程池（threadpool/）

- **CustomThreadPoolDemo.java** - 自定义线程池
  - 核心参数详解
  - 自定义ThreadFactory
  - 自定义拒绝策略
  - IO密集型/CPU密集型线程池
  - 线程池监控

**核心参数**：
```java
ThreadPoolExecutor(
    int corePoolSize,              // 核心线程数
    int maximumPoolSize,           // 最大线程数
    long keepAliveTime,            // 非核心线程存活时间
    TimeUnit unit,                 // 时间单位
    BlockingQueue<Runnable> queue, // 任务队列
    ThreadFactory threadFactory,   // 线程工厂
    RejectedExecutionHandler handler  // 拒绝策略
)
```

**线程数设置**：
- **CPU密集型**：N核心 → N+1个线程
- **IO密集型**：N核心 → 2N 到 4N个线程

### 2. 锁（lock/）

- **LockDemo.java** - 各种锁的使用
  - ReentrantLock - 可重入锁
  - ReadWriteLock - 读写锁
  - StampedLock - 乐观读锁

**锁对比**：
| 锁类型 | 特点 | 使用场景 |
|--------|------|---------|
| synchronized | 隐式锁、不可中断 | 简单同步 |
| ReentrantLock | 显式锁、可中断、可超时 | 复杂同步 |
| ReadWriteLock | 读写分离 | 读多写少 |
| StampedLock | 乐观读 | 极高性能要求 |

### 3. 原子类（atomic/）

- **AtomicDemo.java** - 原子操作类
  - AtomicInteger - 基本类型
  - AtomicReference - 引用类型
  - AtomicStampedReference - 解决ABA问题
  - LongAdder - 高性能计数器

**性能对比**：
- LongAdder在高并发下比AtomicLong快5-10倍
- 原理：分段累加，减少CAS竞争

## 高频面试题

### 1. 线程池核心参数？

**corePoolSize → queue → maximumPoolSize → reject**

执行流程：
1. 线程数 < corePoolSize → 创建线程
2. 线程数 >= corePoolSize → 加入队列
3. 队列满 && 线程数 < maximumPoolSize → 创建线程
4. 队列满 && 线程数 >= maximumPoolSize → 拒绝

### 2. 四种拒绝策略？

1. **AbortPolicy**：抛异常（默认）
2. **CallerRunsPolicy**：调用者运行
3. **DiscardPolicy**：直接丢弃
4. **DiscardOldestPolicy**：丢弃最老的任务

### 3. synchronized vs ReentrantLock？

| 特性 | synchronized | ReentrantLock |
|------|--------------|---------------|
| 锁类型 | 隐式锁 | 显式锁 |
| 可中断 | 不支持 | 支持 |
| 公平锁 | 不支持 | 支持 |
| 条件变量 | 1个 | 多个 |

### 4. CAS原理？

Compare and Swap（比较并交换）：
```java
do {
    expect = value;
    update = expect + 1;
} while (!compareAndSet(expect, update));
```

**ABA问题**：
- 使用版本号（AtomicStampedReference）

### 5. LongAdder为什么快？

- AtomicLong: 单个变量，所有线程竞争
- LongAdder: 分段累加Cell[]，减少竞争

## 如何运行

```bash
# 编译
mvn clean compile

# 运行线程池示例
mvn exec:java -Dexec.mainClass="com.interview.concurrent.threadpool.CustomThreadPoolDemo"

# 运行锁示例
mvn exec:java -Dexec.mainClass="com.interview.concurrent.lock.LockDemo"

# 运行原子类示例
mvn exec:java -Dexec.mainClass="com.interview.concurrent.atomic.AtomicDemo"
```

## 扩展阅读

- 《Java并发编程实战》
- 《Java并发编程的艺术》
- Doug Lea的AQS论文

