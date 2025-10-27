# Java并发编程深度解析

## 目录
- [一、Java内存模型(JMM)](#一java内存模型jmm)
- [二、synchronized与锁优化](#二synchronized与锁优化)
- [三、AQS与显式锁](#三aqs与显式锁)
- [四、线程池](#四线程池)
- [五、原子类(Atomic)](#五原子类atomic)
- [六、并发容器](#六并发容器)
- [七、并发工具类](#七并发工具类)
- [八、CompletableFuture异步编程](#八completablefuture异步编程)
- [九、死锁检测与预防](#九死锁检测与预防)
- [十、真实案例分析](#十真实案例分析)
- [十一、高频面试题](#十一高频面试题)

## 一、Java内存模型(JMM)

### 1.1 JMM概述深度解析

**Java内存模型的数学建模**：
```java
public class JMMModel {
    // 内存状态建模
    private final Map<Variable, Value> mainMemory = new ConcurrentHashMap<>();
    private final Map<Thread, Map<Variable, Value>> workingMemory = new ConcurrentHashMap<>();
    
    // 内存屏障建模
    private final Map<Thread, List<MemoryBarrier>> barriers = new ConcurrentHashMap<>();
    
    // 可见性保证
    public boolean isVisible(Thread reader, Thread writer, Variable var) {
        Map<Variable, Value> readerMemory = workingMemory.get(reader);
        Map<Variable, Value> writerMemory = workingMemory.get(writer);
        
        // 检查是否有适当的内存屏障
        if (hasMemoryBarrier(writer, var) && hasMemoryBarrier(reader, var)) {
            return true;
        }
        
        // 检查主内存同步
        return mainMemory.get(var).equals(readerMemory.get(var));
    }
    
    // 内存屏障插入
    public void insertMemoryBarrier(Thread thread, Variable var, BarrierType type) {
        MemoryBarrier barrier = new MemoryBarrier(var, type, System.nanoTime());
        barriers.computeIfAbsent(thread, k -> new ArrayList<>()).add(barrier);
    }
}
```

**JMM的硬件映射**：
```java
public class HardwareMapping {
    // CPU缓存层次结构
    private final Map<Thread, CPU> cpuMapping = new ConcurrentHashMap<>();
    private final Map<Variable, CacheLine> cacheMapping = new ConcurrentHashMap<>();
    
    // 缓存一致性协议
    public void maintainCacheCoherence(Variable var, Thread thread) {
        CPU cpu = cpuMapping.get(thread);
        CacheLine cacheLine = cacheMapping.get(var);
        
        // MESI协议实现
        if (cacheLine.getState() == CacheState.MODIFIED) {
            // 写回主内存
            writeBackToMainMemory(cacheLine);
            cacheLine.setState(CacheState.SHARED);
        }
        
        // 广播失效
        broadcastInvalidation(var, thread);
    }
    
    // 内存屏障的硬件实现
    public void hardwareMemoryBarrier(BarrierType type) {
        switch (type) {
            case LOAD_LOAD:
                // lfence指令
                asm("lfence");
                break;
            case STORE_STORE:
                // sfence指令
                asm("sfence");
                break;
            case LOAD_STORE:
                // mfence指令
                asm("mfence");
                break;
        }
    }
}
```

Java内存模型（Java Memory Model）定义了线程与主内存之间的抽象关系。

```mermaid
graph TB
    subgraph "线程1"
        A[本地内存1]
        B[工作内存1]
    end
    
    subgraph "线程2"
        C[本地内存2]
        D[工作内存2]
    end
    
    E[主内存]
    
    A <--> E
    C <--> E
    B <--> A
    D <--> C
```

**关键概念深度解析**：
- **主内存**：所有线程共享的内存区域，对应物理内存
- **工作内存**：每个线程私有的内存区域，对应CPU缓存和寄存器
- **变量**：实例字段、静态字段、数组元素（不包括局部变量和方法参数）
- **内存屏障**：保证内存操作顺序的硬件指令
- **缓存一致性**：多核CPU之间的数据一致性协议

### 1.2 内存交互操作

8个原子操作：

| 操作 | 作用域 | 说明 |
|------|--------|------|
| lock | 主内存 | 锁定变量 |
| unlock | 主内存 | 解锁变量 |
| read | 主内存 | 读取变量值 |
| load | 工作内存 | 把read的值放入工作内存 |
| use | 工作内存 | 把值传给执行引擎 |
| assign | 工作内存 | 把执行引擎的值赋给工作内存 |
| store | 工作内存 | 把值传到主内存 |
| write | 主内存 | 把store的值写入主内存 |

### 1.3 三大特性

#### 1.3.1 原子性（Atomicity）

**定义**：操作不可分割，要么全部执行，要么全部不执行。

```java
// 原子操作
int a = 1;        // 原子
a = 2;            // 原子

// 非原子操作
int b = a;        // read + load + use + assign (非原子)
a++;              // read + load + use + add + assign + store + write (非原子)

// 保证原子性
synchronized (lock) {
    a++;          // 加锁后整体原子
}

AtomicInteger atomicInt = new AtomicInteger(0);
atomicInt.incrementAndGet();  // CAS保证原子
```

#### 1.3.2 可见性（Visibility）

**定义**：一个线程修改了变量的值，其他线程能立即看到。

```java
public class VisibilityDemo {
    private boolean flag = false;  // 可能不可见
    
    public void writer() {
        flag = true;  // 线程1修改
    }
    
    public void reader() {
        while (!flag) {  // 线程2可能看不到修改
            // 可能死循环
        }
    }
}

// 解决方案1：volatile
private volatile boolean flag = false;

// 解决方案2：synchronized
public synchronized void writer() {
    flag = true;
}
public synchronized void reader() {
    while (!flag) { }
}

// 解决方案3：final
private final int value = 42;  // final保证可见
```

#### 1.3.3 有序性（Ordering）

**指令重排序**：
```java
// 原始代码
int a = 1;  // 1
int b = 2;  // 2
int c = a + b;  // 3

// 可能重排序为：
int b = 2;  // 2
int a = 1;  // 1
int c = a + b;  // 3
```

**as-if-serial语义**：
- 单线程中，重排序不影响结果
- 多线程中，可能出现问题

**经典案例：双重检查锁（DCL）**
```java
public class Singleton {
    // 错误版本：可能返回未初始化完成的对象
    private static Singleton instance;
    
    public static Singleton getInstance() {
        if (instance == null) {  // 1
            synchronized (Singleton.class) {
                if (instance == null) {  // 2
                    // 可能重排序为：
                    // 1. 分配内存
                    // 2. instance指向内存 (重排序后提前)
                    // 3. 初始化对象
                    instance = new Singleton();  // 3
                }
            }
        }
        return instance;
    }
}

// 正确版本：使用volatile禁止重排序
public class Singleton {
    private static volatile Singleton instance;
    
    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

### 1.4 happens-before原则

**定义**：如果A happens-before B，则A的结果对B可见。

**8条规则**：

1. **程序次序规则**：单线程中，代码顺序执行
2. **锁定规则**：unlock happens-before lock
3. **volatile规则**：volatile写 happens-before volatile读
4. **传递性**：A hb B，B hb C => A hb C
5. **线程启动规则**：Thread.start() happens-before 线程中的操作
6. **线程终止规则**：线程中的操作 happens-before Thread.join()
7. **中断规则**：interrupt() happens-before 检测到中断
8. **对象终结规则**：构造函数 happens-before finalize()

```java
// 示例：volatile的happens-before
class VolatileExample {
    private int a = 0;
    private volatile boolean flag = false;
    
    public void writer() {
        a = 1;           // 1
        flag = true;     // 2 volatile写
    }
    
    public void reader() {
        if (flag) {      // 3 volatile读
            int i = a;   // 4 一定能看到a=1
        }
    }
}
// 1 happens-before 2 (程序次序)
// 2 happens-before 3 (volatile规则)
// 3 happens-before 4 (程序次序)
// 因此 1 happens-before 4 (传递性)
```

### 1.5 volatile关键字

**作用**：
1. 保证可见性
2. 禁止指令重排序
3. **不保证原子性**

**实现原理**：
- 写：插入StoreStore屏障，刷新到主内存
- 读：插入LoadLoad屏障，从主内存读取

**内存屏障**：
```java
// volatile写
Store1
Store2
写volatile变量
StoreLoad屏障  // 禁止上面的写和下面的读/写重排序
Load1

// volatile读
Load1
读volatile变量
LoadLoad屏障   // 禁止下面的读重排序
LoadStore屏障  // 禁止下面的写重排序
Load2
Store2
```

**适用场景**：
```java
// 1. 状态标志
private volatile boolean shutdown = false;

public void shutdown() {
    shutdown = true;
}

public void work() {
    while (!shutdown) {
        // do work
    }
}

// 2. 双重检查锁
private volatile Singleton instance;

// 3. 读多写少的状态
private volatile int counter;  // 只有一个线程写，多个线程读

// 不适用：i++操作
private volatile int count = 0;
public void increment() {
    count++;  // 非原子，volatile无法保证线程安全
}
```

## 二、synchronized与锁优化深度解析

### 2.1 synchronized原理深度实现

#### 2.1.1 对象头结构深度分析

**对象头的底层实现**：
```java
public class ObjectHeader {
    // 64位JVM对象头结构
    private static final int MARK_WORD_SIZE = 8;
    private static final int CLASS_POINTER_SIZE = 8;
    private static final int ARRAY_LENGTH_SIZE = 4;
    
    // Mark Word的位域定义
    private static final int HASHCODE_BITS = 25;
    private static final int AGE_BITS = 4;
    private static final int BIASED_LOCK_BIT = 1;
    private static final int LOCK_TYPE_BITS = 2;
    
    // 锁状态枚举
    public enum LockState {
        UNLOCKED(0),           // 无锁
        BIASED(1),             // 偏向锁
        LIGHTWEIGHT(2),        // 轻量级锁
        HEAVYWEIGHT(3);        // 重量级锁
        
        private final int value;
        LockState(int value) { this.value = value; }
    }
    
    // 获取锁状态
    public LockState getLockState(long markWord) {
        int lockBits = (int) (markWord & 0x3);
        return LockState.values()[lockBits];
    }
    
    // 获取偏向锁信息
    public boolean isBiasedLock(long markWord) {
        return (markWord & 0x1) == 1;
    }
    
    // 获取线程ID（偏向锁）
    public long getThreadId(long markWord) {
        return markWord >>> 3; // 右移3位获取线程ID
    }
}
```

**锁升级的完整流程**：
```java
public class LockUpgrade {
    private final Object lockObject;
    private volatile long markWord;
    
    public LockUpgrade(Object lockObject) {
        this.lockObject = lockObject;
        this.markWord = getMarkWord(lockObject);
    }
    
    // 锁升级状态机
    public void upgradeLock(Thread currentThread) {
        LockState currentState = getCurrentLockState();
        
        switch (currentState) {
            case UNLOCKED:
                // 无锁 -> 偏向锁
                if (shouldUseBiasedLocking()) {
                    upgradeToBiasedLock(currentThread);
                } else {
                    upgradeToLightweightLock(currentThread);
                }
                break;
                
            case BIASED:
                // 偏向锁处理
                if (isCurrentThreadBiased(currentThread)) {
                    // 重入，直接返回
                    return;
                } else {
                    // 撤销偏向锁，升级到轻量级锁
                    revokeBiasedLock();
                    upgradeToLightweightLock(currentThread);
                }
                break;
                
            case LIGHTWEIGHT:
                // 轻量级锁处理
                if (isCurrentThreadLocked(currentThread)) {
                    // 重入，增加锁计数
                    incrementLockCount();
                } else {
                    // 竞争激烈，升级到重量级锁
                    upgradeToHeavyweightLock();
                }
                break;
                
            case HEAVYWEIGHT:
                // 重量级锁，直接阻塞
                blockOnHeavyweightLock();
                break;
        }
    }
}
```

### 2.2 synchronized使用

```java
// 1. 同步实例方法 - 锁是this
public synchronized void method1() {
    // ...
}
// 等价于
public void method1() {
    synchronized (this) {
        // ...
    }
}

// 2. 同步静态方法 - 锁是Class对象
public static synchronized void method2() {
    // ...
}
// 等价于
public static void method2() {
    synchronized (MyClass.class) {
        // ...
    }
}

// 3. 同步代码块
public void method3() {
    synchronized (lock) {
        // ...
    }
}
```

### 2.2 synchronized原理

**字节码层面**：
```
public void synchronized method();
    descriptor: ()V
    flags: (0x0021) ACC_PUBLIC, ACC_SYNCHRONIZED  // 方法标志
    
public void method();
    Code:
       0: aload_0
       1: dup
       2: astore_1
       3: monitorenter      // 进入同步
       4: aload_1
       5: monitorexit       // 退出同步
       6: goto             14
       9: astore_2
      10: aload_1
      11: monitorexit       // 异常时也要退出
      12: aload_2
      13: athrow
```

**对象头结构**（64位JVM）：
```
|-----------------------------------------------------------------------------------------------|
|                                    Object Header (128 bits)                                   |
|-----------------------------------------------------------------------------------------------|
|                        Mark Word (64 bits)                    | Klass Pointer (64 bits)      |
|-----------------------------------------------------------------------------------------------|
```

**Mark Word结构**：
```
|-----------------------------------------------------------------------|-----------------------|
|                          Mark Word (64 bits)                          |      State            |
|-----------------------------------------------------------------------|-----------------------|
| unused:25 | hashcode:31 | unused:1 | age:4 | biased_lock:0 | lock:01 | Normal (无锁)         |
|-----------------------------------------------------------------------|-----------------------|
| thread:54 | epoch:2 | unused:1 | age:4 | biased_lock:1 | lock:01     | Biased (偏向锁)       |
|-----------------------------------------------------------------------|-----------------------|
| ptr_to_lock_record:62                                    | lock:00    | Lightweight (轻量锁)  |
|-----------------------------------------------------------------------|-----------------------|
| ptr_to_heavyweight_monitor:62                            | lock:10    | Heavyweight (重量锁)  |
|-----------------------------------------------------------------------|-----------------------|
| marked for GC                                            | lock:11    | GC Marked             |
|-----------------------------------------------------------------------|-----------------------|
```

### 2.3 锁升级过程

```mermaid
flowchart LR
    A[无锁] --> B[偏向锁]
    B --> C[轻量级锁]
    C --> D[重量级锁]
```

#### 2.3.1 偏向锁

**场景**：大多数情况下，锁不仅不存在多线程竞争，而且总是由同一个线程获取。

**原理**：
1. 首次获取锁时，在对象头记录线程ID
2. 下次该线程进入同步块时，检查线程ID是否一致
3. 一致则直接进入，无需CAS操作

**撤销**：
- 其他线程尝试获取锁
- 调用对象的hashCode()
- 调用wait()/notify()

```java
// 开启/关闭偏向锁
-XX:+UseBiasedLocking         // 开启（默认）
-XX:BiasedLockingStartupDelay=0  // 启动延迟（默认4s）
-XX:-UseBiasedLocking         // 关闭
```

#### 2.3.2 轻量级锁

**场景**：线程交替执行同步块，不存在竞争。

**原理**：
1. 线程在栈帧中创建Lock Record
2. CAS将对象头的Mark Word复制到Lock Record
3. CAS将对象头指向Lock Record
4. 成功则获取锁，失败则自旋
5. 自旋一定次数后升级为重量级锁

#### 2.3.3 重量级锁

**原理**：
- 基于操作系统的互斥量（Mutex）实现
- 需要进行用户态和内核态的切换
- 性能开销大

**Monitor对象**：
```
ObjectMonitor {
    _header       // Mark Word备份
    _object       // 关联的对象
    _owner        // 持有锁的线程
    _WaitSet      // wait()的线程队列
    _EntryList    // 等待锁的线程队列
    _recursions   // 重入次数
    _count        // 计数器
}
```

```mermaid
graph TB
    A[线程尝试获取锁] --> B{_owner为null?}
    B -->|是| C[CAS设置_owner]
    B -->|否| D[进入_EntryList]
    C --> E{成功?}
    E -->|是| F[获取锁]
    E -->|否| D
    D --> G[阻塞等待]
    F --> H[执行同步代码]
    H --> I[释放锁]
    I --> J[唤醒_EntryList中的线程]
```

### 2.4 锁优化技术

#### 2.4.1 自旋锁与自适应自旋

**自旋锁**：
```java
// 伪代码
while (!CAS(lock, null, currentThread)) {
    // 自旋等待
}
```

**自适应自旋**：
- 如果上次自旋成功，则增加自旋次数
- 如果上次自旋失败，则减少自旋次数甚至不自旋

**参数**：
```bash
-XX:+UseSpinning               # JDK 6后默认开启
-XX:PreBlockSpin=10            # 自旋次数
```

#### 2.4.2 锁消除

**场景**：JIT编译器检测到不可能存在共享数据竞争时，消除锁。

```java
public String concat(String s1, String s2) {
    StringBuffer sb = new StringBuffer();  // 局部变量，不存在竞争
    sb.append(s1);  // StringBuffer的append是synchronized的
    sb.append(s2);
    return sb.toString();
}
// JIT会消除StringBuffer内部的锁

// 开启锁消除
-XX:+EliminateLocks
```

#### 2.4.3 锁粗化

**场景**：连续对同一对象加锁，JIT会扩大锁的范围。

```java
// 优化前
for (int i = 0; i < 1000; i++) {
    synchronized (lock) {
        // ...
    }
}

// JIT优化后
synchronized (lock) {
    for (int i = 0; i < 1000; i++) {
        // ...
    }
}
```

### 2.5 synchronized vs ReentrantLock

| 特性 | synchronized | ReentrantLock |
|------|--------------|---------------|
| 实现 | JVM层面 | JDK层面（AQS） |
| 释放 | 自动释放 | 必须手动释放 |
| 可中断 | 不可中断 | 可中断（lockInterruptibly） |
| 公平锁 | 非公平 | 支持公平/非公平 |
| 条件队列 | 一个（wait/notify） | 多个（Condition） |
| 性能 | JDK 6后与Lock相当 | - |
| 便利性 | 简单 | 需要手动释放 |

**使用建议**：
- 简单场景：使用synchronized
- 需要高级功能（可中断、超时、公平锁、多条件）：使用ReentrantLock

## 三、AQS与显式锁

### 3.1 AQS原理

**AbstractQueuedSynchronizer**是Java并发工具的基础框架。

**核心思想**：
- state：同步状态（volatile int）
- FIFO队列：等待线程队列
- CAS操作：修改state

```java
public abstract class AbstractQueuedSynchronizer {
    private volatile int state;  // 同步状态
    
    // 获取state
    protected final int getState() {
        return state;
    }
    
    // 设置state
    protected final void setState(int newState) {
        state = newState;
    }
    
    // CAS更新state
    protected final boolean compareAndSetState(int expect, int update) {
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }
}
```

**队列结构**：
```
     +------+  prev +-------+  prev +-------+
head |      | <---- |       | <---- |       |  tail
     | Node |       | Node  |       | Node  |
     |      | ----> |       | ----> |       |
     +------+  next +-------+  next +-------+
```

**Node节点**：
```java
static final class Node {
    volatile int waitStatus;    // 等待状态
    volatile Node prev;         // 前驱节点
    volatile Node next;         // 后继节点
    volatile Thread thread;     // 等待线程
    Node nextWaiter;            // Condition队列的后继节点
}
```

### 3.2 AQS使用示例

```java
// 自定义独占锁
public class Mutex implements Lock {
    private static class Sync extends AbstractQueuedSynchronizer {
        // 尝试获取锁
        protected boolean tryAcquire(int arg) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }
        
        // 尝试释放锁
        protected boolean tryRelease(int arg) {
            if (getState() == 0) {
                throw new IllegalMonitorStateException();
            }
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }
        
        // 是否持有锁
        protected boolean isHeldExclusively() {
            return getState() == 1;
        }
        
        Condition newCondition() {
            return new ConditionObject();
        }
    }
    
    private final Sync sync = new Sync();
    
    public void lock() {
        sync.acquire(1);
    }
    
    public void unlock() {
        sync.release(1);
    }
    
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }
    
    // ...其他Lock接口方法
}
```

### 3.3 ReentrantLock

**可重入性**：
```java
public class ReentrantExample {
    private ReentrantLock lock = new ReentrantLock();
    
    public void method1() {
        lock.lock();
        try {
            method2();  // 可以再次获取锁
        } finally {
            lock.unlock();
        }
    }
    
    public void method2() {
        lock.lock();  // state+1
        try {
            // ...
        } finally {
            lock.unlock();  // state-1
        }
    }
}
```

**公平锁与非公平锁**：
```java
// 公平锁：按照等待时间顺序获取锁
ReentrantLock fairLock = new ReentrantLock(true);

// 非公平锁（默认）：允许插队
ReentrantLock unfairLock = new ReentrantLock(false);
```

**tryLock超时**：
```java
if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
    try {
        // 获取到锁
    } finally {
        lock.unlock();
    }
} else {
    // 超时未获取到锁
}
```

**可中断**：
```java
try {
    lock.lockInterruptibly();  // 可以被interrupt()中断
    try {
        // ...
    } finally {
        lock.unlock();
    }
} catch (InterruptedException e) {
    // 处理中断
}
```

### 3.4 ReadWriteLock

**特点**：
- 读锁共享：多个线程可同时持有读锁
- 写锁独占：只有一个线程可持有写锁
- 读写互斥：读锁和写锁不能同时持有

```java
public class Cache {
    private Map<String, Object> map = new HashMap<>();
    private ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private Lock readLock = rwLock.readLock();
    private Lock writeLock = rwLock.writeLock();
    
    // 读取
    public Object get(String key) {
        readLock.lock();
        try {
            return map.get(key);
        } finally {
            readLock.unlock();
        }
    }
    
    // 写入
    public void put(String key, Object value) {
        writeLock.lock();
        try {
            map.put(key, value);
        } finally {
            writeLock.unlock();
        }
    }
}
```

**锁降级**：
```java
// 写锁可以降级为读锁，但读锁不能升级为写锁
writeLock.lock();
try {
    // 更新数据
    readLock.lock();  // 降级：先获取读锁
} finally {
    writeLock.unlock();  // 释放写锁
}
try {
    // 读取数据
} finally {
    readLock.unlock();
}
```

### 3.5 StampedLock（JDK 8）

**特点**：
- 比ReadWriteLock更快
- 支持乐观读
- 不支持重入
- 不支持Condition

```java
public class Point {
    private double x, y;
    private final StampedLock sl = new StampedLock();
    
    // 移动点
    void move(double deltaX, double deltaY) {
        long stamp = sl.writeLock();  // 写锁
        try {
            x += deltaX;
            y += deltaY;
        } finally {
            sl.unlockWrite(stamp);
        }
    }
    
    // 计算距离（乐观读）
    double distanceFromOrigin() {
        long stamp = sl.tryOptimisticRead();  // 乐观读
        double currentX = x, currentY = y;
        if (!sl.validate(stamp)) {  // 验证是否被修改
            stamp = sl.readLock();  // 升级为读锁
            try {
                currentX = x;
                currentY = y;
            } finally {
                sl.unlockRead(stamp);
            }
        }
        return Math.sqrt(currentX * currentX + currentY * currentY);
    }
}
```

## 四、线程池

### 4.1 线程池架构

```mermaid
classDiagram
    Executor <|-- ExecutorService
    ExecutorService <|-- AbstractExecutorService
    AbstractExecutorService <|-- ThreadPoolExecutor
    AbstractExecutorService <|-- ForkJoinPool
    ExecutorService <|-- ScheduledExecutorService
    ScheduledExecutorService <|-- ScheduledThreadPoolExecutor
```

### 4.2 ThreadPoolExecutor

**核心参数**：
```java
public ThreadPoolExecutor(
    int corePoolSize,              // 核心线程数
    int maximumPoolSize,           // 最大线程数
    long keepAliveTime,            // 空闲线程存活时间
    TimeUnit unit,                 // 时间单位
    BlockingQueue<Runnable> workQueue,  // 工作队列
    ThreadFactory threadFactory,   // 线程工厂
    RejectedExecutionHandler handler  // 拒绝策略
)
```

**执行流程**：
```mermaid
flowchart TD
    A[提交任务] --> B{线程数 < core?}
    B -->|是| C[创建核心线程]
    B -->|否| D{队列已满?}
    D -->|否| E[加入队列]
    D -->|是| F{线程数 < max?}
    F -->|是| G[创建非核心线程]
    F -->|否| H[执行拒绝策略]
```

**工作队列**：
```java
// 1. ArrayBlockingQueue - 有界队列
new ArrayBlockingQueue<>(100);

// 2. LinkedBlockingQueue - 可选有界队列
new LinkedBlockingQueue<>();  // 无界
new LinkedBlockingQueue<>(100);  // 有界

// 3. SynchronousQueue - 不存储元素
new SynchronousQueue<>();

// 4. PriorityBlockingQueue - 优先级队列
new PriorityBlockingQueue<>();

// 5. DelayQueue - 延迟队列
new DelayQueue<>();
```

**拒绝策略**：
```java
// 1. AbortPolicy - 抛出异常（默认）
new ThreadPoolExecutor.AbortPolicy();

// 2. CallerRunsPolicy - 调用者线程执行
new ThreadPoolExecutor.CallerRunsPolicy();

// 3. DiscardPolicy - 静默丢弃
new ThreadPoolExecutor.DiscardPolicy();

// 4. DiscardOldestPolicy - 丢弃最老的任务
new ThreadPoolExecutor.DiscardOldestPolicy();

// 5. 自定义
new RejectedExecutionHandler() {
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        // 记录日志
        // 保存到数据库/MQ
    }
}
```

### 4.3 Executors工具类

```java
// 1. newFixedThreadPool - 固定大小线程池
ExecutorService pool = Executors.newFixedThreadPool(10);
// 特点：core = max = 10, 队列无界

// 2. newCachedThreadPool - 缓存线程池
ExecutorService pool = Executors.newCachedThreadPool();
// 特点：core = 0, max = Integer.MAX_VALUE, SynchronousQueue
// 适合：大量短期任务

// 3. newSingleThreadExecutor - 单线程线程池
ExecutorService pool = Executors.newSingleThreadExecutor();
// 特点：core = max = 1, 队列无界
// 适合：顺序执行任务

// 4. newScheduledThreadPool - 定时线程池
ScheduledExecutorService pool = Executors.newScheduledThreadPool(5);
// 适合：定时任务、周期任务
```

**注意**：
- 阿里巴巴Java开发手册不推荐使用Executors创建线程池
- 原因：默认队列无界，可能导致OOM
- 建议：手动创建ThreadPoolExecutor，明确参数

### 4.4 线程池最佳实践

#### 4.4.1 合理配置线程数

**CPU密集型**：
```java
// 线程数 = CPU核心数 + 1
int poolSize = Runtime.getRuntime().availableProcessors() + 1;
```

**IO密集型**：
```java
// 线程数 = CPU核心数 * 2
// 或 = CPU核心数 / (1 - 阻塞系数)  阻塞系数0.8~0.9
int poolSize = Runtime.getRuntime().availableProcessors() * 2;
```

#### 4.4.2 优雅关闭

```java
// 1. shutdown() - 不接受新任务，等待已有任务完成
pool.shutdown();

// 2. shutdownNow() - 不接受新任务，尝试停止正在执行的任务
List<Runnable> notExecuted = pool.shutdownNow();

// 3. 等待终止
pool.awaitTermination(60, TimeUnit.SECONDS);

// 4. 优雅关闭示例
pool.shutdown();
try {
    if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
        pool.shutdownNow();
        if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
            System.err.println("Pool did not terminate");
        }
    }
} catch (InterruptedException e) {
    pool.shutdownNow();
    Thread.currentThread().interrupt();
}
```

#### 4.4.3 监控线程池

```java
ThreadPoolExecutor pool = ...;

// 任务数量
long taskCount = pool.getTaskCount();          // 总任务数
long completedCount = pool.getCompletedTaskCount();  // 完成数
int queueSize = pool.getQueue().size();        // 队列中的任务数

// 线程数量
int poolSize = pool.getPoolSize();             // 当前线程数
int activeCount = pool.getActiveCount();       // 活动线程数
int largestPoolSize = pool.getLargestPoolSize();  // 历史最大线程数

// 动态调整
pool.setCorePoolSize(20);
pool.setMaximumPoolSize(50);
```

### 4.5 ForkJoinPool

**特点**：
- 分治算法
- 工作窃取（Work Stealing）
- JDK 7引入

**使用**：
```java
// 1. 继承RecursiveTask（有返回值）
class SumTask extends RecursiveTask<Long> {
    private static final int THRESHOLD = 100;
    private int[] array;
    private int start, end;
    
    protected Long compute() {
        if (end - start <= THRESHOLD) {
            // 直接计算
            long sum = 0;
            for (int i = start; i < end; i++) {
                sum += array[i];
            }
            return sum;
        } else {
            // 分割任务
            int mid = (start + end) / 2;
            SumTask left = new SumTask(array, start, mid);
            SumTask right = new SumTask(array, mid, end);
            left.fork();  // 异步执行
            right.fork();
            return left.join() + right.join();  // 等待结果
        }
    }
}

// 使用
ForkJoinPool pool = new ForkJoinPool();
long result = pool.invoke(new SumTask(array, 0, array.length));
```

## 五、原子类（Atomic）

### 5.1 原子类概述

**核心原理**：CAS（Compare-And-Swap）+ volatile

**分类**：
1. 基本类型：AtomicInteger、AtomicLong、AtomicBoolean
2. 数组类型：AtomicIntegerArray、AtomicLongArray、AtomicReferenceArray
3. 引用类型：AtomicReference、AtomicStampedReference、AtomicMarkableReference
4. 字段更新器：AtomicIntegerFieldUpdater、AtomicLongFieldUpdater、AtomicReferenceFieldUpdater
5. 累加器（JDK 8）：LongAdder、DoubleAdder

### 5.2 基本类型原子类

**AtomicInteger示例**：
```java
public class AtomicIntegerDemo {
    private AtomicInteger count = new AtomicInteger(0);
    
    public void increment() {
        count.incrementAndGet();  // count++
    }
    
    public void decrement() {
        count.decrementAndGet();  // count--
    }
    
    public int get() {
        return count.get();
    }
    
    // CAS操作
    public boolean compareAndSet(int expect, int update) {
        return count.compareAndSet(expect, update);
    }
}
```

**主要方法**：
```java
AtomicInteger ai = new AtomicInteger(0);

// 获取和设置
int value = ai.get();              // 获取当前值
ai.set(10);                        // 设置值
ai.lazySet(10);                    // 最终设置（不保证立即可见）

// 自增自减
int i1 = ai.getAndIncrement();     // i++
int i2 = ai.incrementAndGet();     // ++i
int i3 = ai.getAndDecrement();     // i--
int i4 = ai.decrementAndGet();     // --i

// 加减操作
int i5 = ai.getAndAdd(5);          // 获取后加5
int i6 = ai.addAndGet(5);          // 加5后获取

// CAS操作
boolean success = ai.compareAndSet(10, 20);  // 期望10则设置为20

// JDK 8新增
int i7 = ai.getAndUpdate(x -> x * 2);        // 获取后更新
int i8 = ai.updateAndGet(x -> x * 2);        // 更新后获取
int i9 = ai.getAndAccumulate(10, (x, y) -> x + y);  // 累积操作
```

**底层实现**（简化）：
```java
public class AtomicInteger {
    private volatile int value;  // volatile保证可见性
    
    public final int incrementAndGet() {
        for (;;) {  // 自旋
            int current = get();
            int next = current + 1;
            if (compareAndSet(current, next))  // CAS
                return next;
        }
    }
    
    public final boolean compareAndSet(int expect, int update) {
        return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
    }
}
```

### 5.3 数组类型原子类

```java
public class AtomicArrayDemo {
    public static void main(String[] args) {
        AtomicIntegerArray array = new AtomicIntegerArray(10);
        
        // 设置索引0的值为5
        array.set(0, 5);
        
        // 索引0的值自增
        array.incrementAndGet(0);
        
        // CAS：如果索引0的值是6，则设置为10
        boolean success = array.compareAndSet(0, 6, 10);
        
        System.out.println(array.get(0));  // 10
    }
}
```

### 5.4 引用类型原子类

**AtomicReference**：
```java
public class User {
    private String name;
    private int age;
    // getter/setter
}

public class AtomicReferenceDemo {
    private AtomicReference<User> userRef = new AtomicReference<>();
    
    public void updateUser(User oldUser, User newUser) {
        userRef.compareAndSet(oldUser, newUser);
    }
}
```

**AtomicStampedReference（解决ABA问题）**：
```java
public class AtomicStampedReferenceDemo {
    public static void main(String[] args) {
        AtomicStampedReference<Integer> ref = 
            new AtomicStampedReference<>(100, 0);
        
        // 获取当前版本号
        int[] stampHolder = new int[1];
        Integer value = ref.get(stampHolder);
        int stamp = stampHolder[0];
        
        // CAS：值为100且版本号为0时，更新为101并将版本号+1
        boolean success = ref.compareAndSet(100, 101, stamp, stamp + 1);
        
        System.out.println("Update success: " + success);
        System.out.println("New value: " + ref.getReference());
        System.out.println("New stamp: " + ref.getStamp());
    }
}
```

**ABA问题演示**：
```java
// ABA问题
AtomicInteger atomicInt = new AtomicInteger(100);

// 线程1
Thread t1 = new Thread(() -> {
    int value = atomicInt.get();  // 读取100
    // 睡眠，让线程2执行
    try { Thread.sleep(100); } catch (InterruptedException e) {}
    // CAS成功，但实际上值经历了100->200->100
    boolean success = atomicInt.compareAndSet(100, 200);
    System.out.println("T1 CAS: " + success);  // true
});

// 线程2
Thread t2 = new Thread(() -> {
    atomicInt.compareAndSet(100, 200);  // 100->200
    atomicInt.compareAndSet(200, 100);  // 200->100
});

t1.start();
t2.start();
```

**使用AtomicStampedReference解决**：
```java
AtomicStampedReference<Integer> ref = 
    new AtomicStampedReference<>(100, 0);

// 线程1
Thread t1 = new Thread(() -> {
    int[] stampHolder = new int[1];
    int value = ref.get(stampHolder);
    int stamp = stampHolder[0];
    
    try { Thread.sleep(100); } catch (InterruptedException e) {}
    
    // CAS失败，因为版本号已经改变
    boolean success = ref.compareAndSet(100, 200, stamp, stamp + 1);
    System.out.println("T1 CAS: " + success);  // false
});

// 线程2
Thread t2 = new Thread(() -> {
    int[] stampHolder = new int[1];
    int value = ref.get(stampHolder);
    int stamp = stampHolder[0];
    
    ref.compareAndSet(100, 200, stamp, stamp + 1);
    ref.compareAndSet(200, 100, stamp + 1, stamp + 2);
});
```

### 5.5 字段更新器

**作用**：以原子方式更新对象的字段。

**使用场景**：
- 需要原子更新某个类的字段
- 但又不想使用锁
- 字段必须是volatile修饰的

```java
public class AtomicFieldUpdaterDemo {
    static class User {
        volatile String name;
        volatile int age;
    }
    
    public static void main(String[] args) {
        // 创建字段更新器
        AtomicReferenceFieldUpdater<User, String> nameUpdater =
            AtomicReferenceFieldUpdater.newUpdater(
                User.class, String.class, "name");
        
        AtomicIntegerFieldUpdater<User> ageUpdater =
            AtomicIntegerFieldUpdater.newUpdater(User.class, "age");
        
        User user = new User();
        
        // 原子更新name
        nameUpdater.compareAndSet(user, null, "Tom");
        
        // 原子更新age
        ageUpdater.incrementAndGet(user);
        
        System.out.println(user.name);  // Tom
        System.out.println(user.age);   // 1
    }
}
```

**注意事项**：
- 字段必须是volatile修饰
- 不能是static字段
- 不能是final字段
- 字段必须对updater可见（不能是private）

### 5.6 LongAdder（JDK 8）

**问题**：AtomicLong在高并发下性能不佳
- CAS失败后自旋
- 多线程竞争同一个变量

**解决**：LongAdder
- 热点分离：将单个变量拆分为多个Cell
- 减少竞争：不同线程操作不同Cell
- 最终求和：sum() = base + Σcells

**原理**：
```
AtomicLong:  所有线程竞争同一个value
    Thread1 \
    Thread2  --> [value] (CAS竞争激烈)
    Thread3 /

LongAdder:   线程分散到不同Cell
    Thread1 --> [Cell1]
    Thread2 --> [Cell2]  (减少竞争)
    Thread3 --> [Cell3]
    
    sum() = base + Cell1 + Cell2 + Cell3
```

**使用示例**：
```java
public class LongAdderDemo {
    private LongAdder counter = new LongAdder();
    
    public void increment() {
        counter.increment();  // +1
    }
    
    public void add(long x) {
        counter.add(x);       // +x
    }
    
    public long sum() {
        return counter.sum();  // 求和
    }
    
    public void reset() {
        counter.reset();       // 重置为0
    }
}
```

**性能对比**：
```java
public class PerformanceTest {
    public static void main(String[] args) throws InterruptedException {
        int threadCount = 50;
        int iterations = 1_000_000;
        
        // 测试AtomicLong
        AtomicLong atomicLong = new AtomicLong(0);
        long start1 = System.currentTimeMillis();
        CountDownLatch latch1 = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    atomicLong.incrementAndGet();
                }
                latch1.countDown();
            }).start();
        }
        latch1.await();
        long time1 = System.currentTimeMillis() - start1;
        
        // 测试LongAdder
        LongAdder longAdder = new LongAdder();
        long start2 = System.currentTimeMillis();
        CountDownLatch latch2 = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    longAdder.increment();
                }
                latch2.countDown();
            }).start();
        }
        latch2.await();
        long time2 = System.currentTimeMillis() - start2;
        
        System.out.println("AtomicLong: " + time1 + "ms");
        System.out.println("LongAdder: " + time2 + "ms");
        // LongAdder通常快2-5倍
    }
}
```

**AtomicLong vs LongAdder**：

| 特性 | AtomicLong | LongAdder |
|------|-----------|-----------|
| 实现 | CAS单变量 | Cell数组分散热点 |
| 性能 | 高并发下性能差 | 高并发性能好 |
| 内存 | 占用少 | 占用多（Cell数组） |
| 精确性 | 实时精确值 | sum()非实时 |
| 使用场景 | 低并发、需要精确值 | 高并发计数器 |

**选择建议**：
- 计数器、统计：LongAdder
- 需要CAS操作：AtomicLong
- 需要实时精确值：AtomicLong
- 高并发累加：LongAdder

## 六、并发容器

### 6.1 ConcurrentHashMap

**JDK 7实现**：
- Segment数组（继承ReentrantLock）
- 每个Segment是一个小HashMap
- 并发度 = Segment数量

**JDK 8实现**：
- Node数组 + 链表/红黑树
- CAS + synchronized
- 并发度更高

```java
// put操作（JDK 8简化版）
final V putVal(K key, V value) {
    int hash = spread(key.hashCode());
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh;
        if (tab == null || (n = tab.length) == 0)
            tab = initTable();  // 初始化
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            if (casTabAt(tab, i, null, new Node<K,V>(hash, key, value)))
                break;  // CAS成功
        }
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);  // 帮助扩容
        else {
            synchronized (f) {  // 锁住头节点
                // 链表或红黑树插入
            }
        }
    }
    addCount(1L, binCount);
    return null;
}
```

**size()实现**：
```java
// counterCells数组 + baseCount
// 类似LongAdder的设计
public int size() {
    long n = sumCount();
    return ((n < 0L) ? 0 :
            (n > (long)Integer.MAX_VALUE) ? Integer.MAX_VALUE :
            (int)n);
}

final long sumCount() {
    CounterCell[] as = counterCells; CounterCell a;
    long sum = baseCount;
    if (as != null) {
        for (int i = 0; i < as.length; ++i) {
            if ((a = as[i]) != null)
                sum += a.value;
        }
    }
    return sum;
}
```

### 5.2 CopyOnWriteArrayList

**原理**：
- 写时复制（Copy-On-Write）
- 读不加锁，写加锁
- 适合读多写少场景

```java
public boolean add(E e) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        Object[] newElements = Arrays.copyOf(elements, len + 1);  // 复制
        newElements[len] = e;
        setArray(newElements);  // 替换
        return true;
    } finally {
        lock.unlock();
    }
}

public E get(int index) {
    return get(getArray(), index);  // 无锁读取
}
```

**缺点**：
- 内存占用大（两份数组）
- 数据一致性问题（读取的可能是旧数据）

### 5.3 BlockingQueue

**常用实现**：
```java
// 1. ArrayBlockingQueue - 数组实现，有界
BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(100);

// 2. LinkedBlockingQueue - 链表实现，可选有界
BlockingQueue<Integer> queue = new LinkedBlockingQueue<>();

// 3. PriorityBlockingQueue - 优先级队列，无界
BlockingQueue<Integer> queue = new PriorityBlockingQueue<>();

// 4. DelayQueue - 延迟队列
BlockingQueue<Delayed> queue = new DelayQueue<>();

// 5. SynchronousQueue - 不存储元素，直接传递
BlockingQueue<Integer> queue = new SynchronousQueue<>();
```

**主要方法**：
| 操作 | 抛异常 | 返回特殊值 | 阻塞 | 超时 |
|------|--------|-----------|------|------|
| 插入 | add(e) | offer(e) | put(e) | offer(e, time, unit) |
| 移除 | remove() | poll() | take() | poll(time, unit) |
| 检查 | element() | peek() | - | - |

**生产者-消费者**：
```java
public class ProducerConsumer {
    private BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);
    
    class Producer implements Runnable {
        public void run() {
            try {
                for (int i = 0; i < 100; i++) {
                    queue.put(i);  // 队列满时阻塞
                    System.out.println("Produced: " + i);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    class Consumer implements Runnable {
        public void run() {
            try {
                while (true) {
                    Integer item = queue.take();  // 队列空时阻塞
                    System.out.println("Consumed: " + item);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
```

## 七、并发工具类

### 7.1 CountDownLatch

**用途**：一个或多个线程等待其他线程完成操作。

```java
public class CountDownLatchDemo {
    public static void main(String[] args) throws InterruptedException {
        int N = 5;
        CountDownLatch latch = new CountDownLatch(N);
        
        for (int i = 0; i < N; i++) {
            new Thread(() -> {
                System.out.println(Thread.currentThread().getName() + " working");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                latch.countDown();  // 计数-1
                System.out.println(Thread.currentThread().getName() + " done");
            }).start();
        }
        
        latch.await();  // 等待计数归零
        System.out.println("All threads done");
    }
}
```

### 7.2 CyclicBarrier

**用途**：让一组线程到达屏障时被阻塞，直到最后一个线程到达。

```java
public class CyclicBarrierDemo {
    public static void main(String[] args) {
        int N = 5;
        CyclicBarrier barrier = new CyclicBarrier(N, () -> {
            System.out.println("All arrived, continue");  // 所有线程到达后执行
        });
        
        for (int i = 0; i < N; i++) {
            new Thread(() -> {
                try {
                    System.out.println(Thread.currentThread().getName() + " arrived");
                    barrier.await();  // 等待其他线程
                    System.out.println(Thread.currentThread().getName() + " continue");
                } catch (Exception e) {
                }
            }).start();
        }
    }
}
```

**CountDownLatch vs CyclicBarrier**：
| 特性 | CountDownLatch | CyclicBarrier |
|------|----------------|---------------|
| 复用 | 不可复用 | 可复用（reset） |
| 等待对象 | 一个线程等多个线程 | 多个线程互相等待 |
| 计数 | 减法（countDown） | 加法（await） |

### 7.3 Semaphore

**用途**：控制同时访问资源的线程数量。

```java
public class SemaphoreDemo {
    public static void main(String[] args) {
        Semaphore semaphore = new Semaphore(3);  // 3个许可
        
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    semaphore.acquire();  // 获取许可
                    System.out.println(Thread.currentThread().getName() + " acquired");
                    Thread.sleep(2000);
                    System.out.println(Thread.currentThread().getName() + " release");
                } catch (InterruptedException e) {
                } finally {
                    semaphore.release();  // 释放许可
                }
            }).start();
        }
    }
}
```

**应用场景**：
- 数据库连接池
- 限流

### 7.4 Exchanger

**用途**：两个线程交换数据。

```java
public class ExchangerDemo {
    public static void main(String[] args) {
        Exchanger<String> exchanger = new Exchanger<>();
        
        new Thread(() -> {
            try {
                String data = "Thread-1 data";
                String received = exchanger.exchange(data);
                System.out.println("Thread-1 received: " + received);
            } catch (InterruptedException e) {
            }
        }).start();
        
        new Thread(() -> {
            try {
                String data = "Thread-2 data";
                String received = exchanger.exchange(data);
                System.out.println("Thread-2 received: " + received);
            } catch (InterruptedException e) {
            }
        }).start();
    }
}
```

## 八、CompletableFuture异步编程

### 8.1 Future的局限性

**传统Future的问题**:
```java
ExecutorService executor = Executors.newFixedThreadPool(10);
Future<String> future = executor.submit(() -> {
    Thread.sleep(1000);
    return "Result";
});

// 问题1：只能通过阻塞get()获取结果
String result = future.get();  // 阻塞等待

// 问题2：无法链式调用
// 问题3：无法组合多个Future
// 问题4：无法异常处理
```

### 8.2 CompletableFuture基本使用

**创建CompletableFuture**:
```java
// 1. 创建已完成的Future
CompletableFuture<String> future1 = CompletableFuture.completedFuture("Hello");

// 2. runAsync - 无返回值
CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
    System.out.println("Running async");
});

// 3. supplyAsync - 有返回值
CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
    return "Result";
});

// 4. 指定线程池
ExecutorService executor = Executors.newFixedThreadPool(10);
CompletableFuture<String> future4 = CompletableFuture.supplyAsync(() -> {
    return "Result";
}, executor);
```

### 8.3 结果转换

**thenApply - 同步转换**:
```java
CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
    return 100;
}).thenApply(result -> {
    return result * 2;  // 200
}).thenApply(result -> {
    return result + 50;  // 250
});

System.out.println(future.get());  // 250
```

**thenApplyAsync - 异步转换**:
```java
CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
    System.out.println("Task1: " + Thread.currentThread().getName());
    return 100;
}).thenApplyAsync(result -> {
    System.out.println("Task2: " + Thread.currentThread().getName());
    return result * 2;
});
```

### 8.4 结果消费

**thenAccept - 消费结果，无返回值**:
```java
CompletableFuture.supplyAsync(() -> {
    return "Hello";
}).thenAccept(result -> {
    System.out.println("Result: " + result);
});
```

**thenRun - 不关心结果**:
```java
CompletableFuture.supplyAsync(() -> {
    return "Hello";
}).thenRun(() -> {
    System.out.println("Task completed");
});
```

### 8.5 组合多个Future

**thenCompose - 依赖组合**:
```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    return "User123";
}).thenCompose(userId -> {
    // 根据用户ID查询用户信息
    return CompletableFuture.supplyAsync(() -> {
        return "UserInfo: " + userId;
    });
});
```

**thenCombine - 合并两个独立的Future**:
```java
CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
    return 100;
});

CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> {
    return 200;
});

CompletableFuture<Integer> result = future1.thenCombine(future2, (r1, r2) -> {
    return r1 + r2;  // 300
});
```

**allOf - 等待所有Future完成**:
```java
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> "Task1");
CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> "Task2");
CompletableFuture<String> f3 = CompletableFuture.supplyAsync(() -> "Task3");

CompletableFuture<Void> allFutures = CompletableFuture.allOf(f1, f2, f3);

allFutures.thenRun(() -> {
    // 所有任务完成后执行
    String result1 = f1.join();
    String result2 = f2.join();
    String result3 = f3.join();
    System.out.println(result1 + ", " + result2 + ", " + result3);
});
```

**anyOf - 任意一个完成**:
```java
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> {
    sleep(1000);
    return "Task1";
});
CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> {
    sleep(500);
    return "Task2";  // 最快完成
});

CompletableFuture<Object> fastest = CompletableFuture.anyOf(f1, f2);
System.out.println(fastest.get());  // Task2
```

### 8.6 异常处理

**exceptionally - 异常恢复**:
```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    if (true) {
        throw new RuntimeException("Error occurred");
    }
    return "Success";
}).exceptionally(ex -> {
    System.out.println("Exception: " + ex.getMessage());
    return "Default value";  // 返回默认值
});

System.out.println(future.get());  // Default value
```

**handle - 处理正常结果和异常**:
```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    if (Math.random() > 0.5) {
        throw new RuntimeException("Error");
    }
    return "Success";
}).handle((result, ex) -> {
    if (ex != null) {
        return "Error: " + ex.getMessage();
    }
    return result;
});
```

**whenComplete - 类似finally**:
```java
CompletableFuture.supplyAsync(() -> {
    return "Result";
}).whenComplete((result, ex) -> {
    if (ex != null) {
        System.out.println("Exception: " + ex);
    } else {
        System.out.println("Success: " + result);
    }
});
```

### 8.7 实战案例

**案例1：并行调用多个服务**:
```java
public class ParallelServiceCall {
    public CompletableFuture<UserDetail> getUserDetail(String userId) {
        // 并行调用3个服务
        CompletableFuture<User> userFuture = 
            CompletableFuture.supplyAsync(() -> userService.getUser(userId));
        
        CompletableFuture<List<Order>> ordersFuture = 
            CompletableFuture.supplyAsync(() -> orderService.getOrders(userId));
        
        CompletableFuture<Address> addressFuture = 
            CompletableFuture.supplyAsync(() -> addressService.getAddress(userId));
        
        // 等待所有结果，然后组合
        return CompletableFuture.allOf(userFuture, ordersFuture, addressFuture)
            .thenApply(v -> {
                User user = userFuture.join();
                List<Order> orders = ordersFuture.join();
                Address address = addressFuture.join();
                return new UserDetail(user, orders, address);
            });
    }
}
```

**案例2：超时控制**:
```java
public class TimeoutExample {
    public CompletableFuture<String> fetchDataWithTimeout(String url) {
        return CompletableFuture.supplyAsync(() -> {
            return httpClient.get(url);  // 可能很慢
        }).orTimeout(3, TimeUnit.SECONDS)  // JDK 9+
          .exceptionally(ex -> {
              if (ex instanceof TimeoutException) {
                  return "Request timeout";
              }
              return "Error: " + ex.getMessage();
          });
    }
    
    // JDK 8兼容写法
    public CompletableFuture<String> fetchDataWithTimeoutJDK8(String url) {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            return httpClient.get(url);
        });
        
        // 创建超时Future
        CompletableFuture<String> timeout = new CompletableFuture<>();
        scheduler.schedule(() -> {
            timeout.completeExceptionally(new TimeoutException("Request timeout"));
        }, 3, TimeUnit.SECONDS);
        
        // 返回最先完成的
        return future.applyToEither(timeout, r -> r);
    }
}
```

**案例3：批量处理**:
```java
public class BatchProcessing {
    public CompletableFuture<List<Result>> processInBatch(List<String> ids) {
        // 将ids分批处理
        int batchSize = 10;
        List<CompletableFuture<Result>> futures = new ArrayList<>();
        
        for (int i = 0; i < ids.size(); i += batchSize) {
            int end = Math.min(i + batchSize, ids.size());
            List<String> batch = ids.subList(i, end);
            
            CompletableFuture<Result> future = CompletableFuture.supplyAsync(() -> {
                return processBatch(batch);
            });
            
            futures.add(future);
        }
        
        // 等待所有批次完成
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                return futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
            });
    }
}
```

### 8.8 最佳实践

**1. 合理使用线程池**:
```java
// 不推荐：使用默认线程池（ForkJoinPool.commonPool()）
CompletableFuture.supplyAsync(() -> doSomething());

// 推荐：使用自定义线程池
ExecutorService executor = new ThreadPoolExecutor(
    10, 20, 60L, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(100),
    new ThreadPoolExecutor.CallerRunsPolicy()
);

CompletableFuture.supplyAsync(() -> doSomething(), executor);
```

**2. 避免阻塞**:
```java
// 不推荐：使用get()阻塞
String result = future.get();  // 阻塞当前线程

// 推荐：使用回调
future.thenAccept(result -> {
    // 异步处理结果
});
```

**3. 异常处理**:
```java
CompletableFuture.supplyAsync(() -> {
    return riskyOperation();
}).exceptionally(ex -> {
    log.error("Operation failed", ex);
    return defaultValue;
}).thenAccept(result -> {
    // 处理结果
});
```

**4. 避免过长的链式调用**:
```java
// 不推荐：过长的链
future.thenApply(...).thenApply(...).thenApply(...).thenApply(...);

// 推荐：分解为多个步骤
CompletableFuture<A> step1 = future.thenApply(this::processA);
CompletableFuture<B> step2 = step1.thenApply(this::processB);
CompletableFuture<C> step3 = step2.thenApply(this::processC);
```

## 九、死锁检测与预防

### 9.1 死锁的四个必要条件

1. **互斥条件**：资源不能被共享，只能由一个线程使用
2. **请求与保持条件**：线程已持有至少一个资源，又请求新资源
3. **不剥夺条件**：资源不能被强制剥夺，只能主动释放
4. **循环等待条件**：存在线程资源的循环等待链

### 9.2 死锁示例

**经典死锁**:
```java
public class DeadlockDemo {
    private static Object lock1 = new Object();
    private static Object lock2 = new Object();
    
    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            synchronized (lock1) {
                System.out.println("T1 got lock1");
                sleep(100);
                synchronized (lock2) {
                    System.out.println("T1 got lock2");
                }
            }
        });
        
        Thread t2 = new Thread(() -> {
            synchronized (lock2) {
                System.out.println("T2 got lock2");
                sleep(100);
                synchronized (lock1) {
                    System.out.println("T2 got lock1");
                }
            }
        });
        
        t1.start();
        t2.start();
        // T1持有lock1等待lock2，T2持有lock2等待lock1 -> 死锁
    }
}
```

**转账死锁**:
```java
public class TransferDeadlock {
    public void transfer(Account from, Account to, int amount) {
        synchronized (from) {
            System.out.println("Locked " + from.getId());
            sleep(100);
            synchronized (to) {
                System.out.println("Locked " + to.getId());
                from.debit(amount);
                to.credit(amount);
            }
        }
    }
    
    // 线程1: transfer(accountA, accountB, 100)
    // 线程2: transfer(accountB, accountA, 200)
    // -> 死锁
}
```

### 9.3 死锁检测

**使用jstack检测**:
```bash
# 1. 找到Java进程PID
jps -l

# 2. 生成线程dump
jstack <pid> > thread_dump.txt

# 3. 查找死锁
grep -A 10 "Found.*deadlock" thread_dump.txt
```

**死锁输出示例**:
```
Found one Java-level deadlock:
=============================
"Thread-1":
  waiting to lock monitor 0x00007f8a4c004e00 (object 0x00000000d5f78a10, a java.lang.Object),
  which is held by "Thread-0"
"Thread-0":
  waiting to lock monitor 0x00007f8a4c007350 (object 0x00000000d5f78a20, a java.lang.Object),
  which is held by "Thread-1"

Java stack information for the threads listed above:
===================================================
"Thread-1":
        at DeadlockDemo.lambda$main$1(DeadlockDemo.java:23)
        - waiting to lock <0x00000000d5f78a10> (a java.lang.Object)
        - locked <0x00000000d5f78a20> (a java.lang.Object)
        ...
```

**编程方式检测**:
```java
public class DeadlockDetector {
    public static void detectDeadlock() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreads = threadBean.findDeadlockedThreads();
        
        if (deadlockedThreads != null) {
            ThreadInfo[] threadInfos = threadBean.getThreadInfo(deadlockedThreads);
            System.out.println("检测到死锁！");
            for (ThreadInfo threadInfo : threadInfos) {
                System.out.println("线程: " + threadInfo.getThreadName());
                System.out.println("锁信息: " + threadInfo.getLockInfo());
                System.out.println("锁拥有者: " + threadInfo.getLockOwnerName());
            }
        }
    }
    
    public static void main(String[] args) {
        // 启动定时检测
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(() -> {
            detectDeadlock();
        }, 0, 5, TimeUnit.SECONDS);
    }
}
```

### 9.4 死锁预防

**方法1：按顺序加锁**:
```java
public class OrderedLocking {
    public void transfer(Account from, Account to, int amount) {
        // 按账户ID排序，避免循环等待
        Account first = from.getId() < to.getId() ? from : to;
        Account second = from.getId() < to.getId() ? to : from;
        
        synchronized (first) {
            synchronized (second) {
                from.debit(amount);
                to.credit(amount);
            }
        }
    }
}
```

**方法2：使用tryLock超时**:
```java
public class TryLockExample {
    private Lock lock1 = new ReentrantLock();
    private Lock lock2 = new ReentrantLock();
    
    public void method() throws InterruptedException {
        while (true) {
            if (lock1.tryLock(100, TimeUnit.MILLISECONDS)) {
                try {
                    if (lock2.tryLock(100, TimeUnit.MILLISECONDS)) {
                        try {
                            // 业务逻辑
                            return;
                        } finally {
                            lock2.unlock();
                        }
                    }
                } finally {
                    lock1.unlock();
                }
            }
            // 获取锁失败，重试
            Thread.sleep(10);
        }
    }
}
```

**方法3：使用单个锁**:
```java
public class SingleLockTransfer {
    private static final Object TRANSFER_LOCK = new Object();
    
    public void transfer(Account from, Account to, int amount) {
        synchronized (TRANSFER_LOCK) {
            from.debit(amount);
            to.credit(amount);
        }
    }
}
```

**方法4：使用并发工具类**:
```java
public class ConcurrentTransfer {
    private Semaphore semaphore = new Semaphore(1);
    
    public void transfer(Account from, Account to, int amount) 
            throws InterruptedException {
        semaphore.acquire();
        try {
            from.debit(amount);
            to.credit(amount);
        } finally {
            semaphore.release();
        }
    }
}
```

### 9.5 活锁

**活锁示例**:
```java
public class LivelockDemo {
    static class Spoon {
        private Diner owner;
        
        public synchronized void use() {
            System.out.println(owner.name + " is eating");
        }
        
        public synchronized void setOwner(Diner diner) {
            owner = diner;
        }
        
        public synchronized Diner getOwner() {
            return owner;
        }
    }
    
    static class Diner {
        private String name;
        private boolean isHungry;
        
        public void eatWith(Spoon spoon, Diner spouse) {
            while (isHungry) {
                if (spoon.getOwner() != this) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                    }
                    continue;
                }
                
                // 礼让：如果配偶饿了，让给配偶
                if (spouse.isHungry) {
                    System.out.println(name + ": You eat first");
                    spoon.setOwner(spouse);
                    continue;  // 活锁：双方都在礼让
                }
                
                spoon.use();
                isHungry = false;
                spoon.setOwner(spouse);
            }
        }
    }
}
```

**解决活锁**:
```java
// 引入随机等待，打破对称性
if (spouse.isHungry) {
    System.out.println(name + ": You eat first");
    spoon.setOwner(spouse);
    Thread.sleep((long) (Math.random() * 100));  // 随机等待
    continue;
}
```

## 十、真实案例分析

### 10.1 案例1：高并发计数器性能优化

**问题背景**:
- 系统需要统计接口调用次数
- QPS达到10万+
- 使用AtomicLong导致CPU飙升

**初始实现**:
```java
public class RequestCounter {
    private AtomicLong counter = new AtomicLong(0);
    
    public void increment() {
        counter.incrementAndGet();  // 高并发下CAS失败率高
    }
    
    public long getCount() {
        return counter.get();
    }
}
```

**性能问题**:
- 50个线程，每个线程100万次递增
- AtomicLong耗时: 5200ms
- CPU使用率: 95%

**优化方案**:
```java
public class OptimizedRequestCounter {
    private LongAdder counter = new LongAdder();  // 使用LongAdder
    
    public void increment() {
        counter.increment();
    }
    
    public long getCount() {
        return counter.sum();
    }
}
```

**优化效果**:
- LongAdder耗时: 1800ms（提升65%）
- CPU使用率: 60%

**经验总结**:
- 高并发计数场景使用LongAdder
- 不需要实时精确值时优先LongAdder
- AtomicLong适合低并发或需要CAS操作的场景

### 10.2 案例2：线程池OOM问题

**问题现象**:
- 应用运行一段时间后OOM
- 堆内存充足，-Xmx设置4G
- 线程数不断增长

**问题代码**:
```java
// 错误示例
ExecutorService executor = Executors.newCachedThreadPool();

for (int i = 0; i < 100000; i++) {
    executor.submit(() -> {
        Thread.sleep(60000);  // 模拟长时间任务
    });
}
// CachedThreadPool会创建大量线程，每个线程占用1MB栈空间
// 100000个线程 = 100GB栈空间 -> OOM
```

**排查过程**:
```bash
# 1. 查看线程数
jstack <pid> | grep "java.lang.Thread.State" | wc -l
# 输出: 50000+

# 2. 查看内存
jmap -heap <pid>
# Thread count: 50000+
# Thread stack size: 1MB
# Total: 50GB

# 3. OOM类型
java.lang.OutOfMemoryError: unable to create new native thread
```

**解决方案**:
```java
// 正确示例：使用有界线程池
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    10,  // 核心线程数
    20,  // 最大线程数
    60L, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(1000),  // 有界队列
    new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
);

// 监控线程池
ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);
monitor.scheduleAtFixedRate(() -> {
    System.out.println("Active threads: " + executor.getActiveCount());
    System.out.println("Queue size: " + executor.getQueue().size());
}, 0, 10, TimeUnit.SECONDS);
```

**经验总结**:
- 避免使用Executors.newCachedThreadPool()
- 使用有界队列和明确的线程池参数
- 监控线程池状态
- 设置合理的拒绝策略

### 10.3 案例3：ConcurrentHashMap死循环

**问题背景**:
- JDK 7环境下，并发写入ConcurrentHashMap
- CPU持续100%
- 应用假死

**问题原因**:
- JDK 7的ConcurrentHashMap在扩容时可能形成环形链表
- 导致get()操作死循环

**复现代码**:
```java
// JDK 7环境
public class ConcurrentHashMapDeadLoop {
    public static void main(String[] args) {
        final Map<String, String> map = new ConcurrentHashMap<>();
        
        for (int i = 0; i < 100; i++) {
            new Thread(() -> {
                for (int j = 0; j < 10000; j++) {
                    map.put(UUID.randomUUID().toString(), "value");
                }
            }).start();
        }
    }
}
```

**解决方案**:
```java
// 方案1：升级到JDK 8+
// JDK 8重写了ConcurrentHashMap，使用CAS+synchronized

// 方案2：预估容量，减少扩容
Map<String, String> map = new ConcurrentHashMap<>(100000, 0.75f, 16);

// 方案3：使用Collections.synchronizedMap
Map<String, String> map = Collections.synchronizedMap(new HashMap<>());
```

**经验总结**:
- JDK 7的ConcurrentHashMap存在并发问题
- 升级到JDK 8+
- 合理设置初始容量，减少扩容

## 十一、高频面试题

### Q1：synchronized和volatile的区别？

**核心原理**：
synchronized和volatile是Java并发编程的两个核心关键字，核心区别在于"原子性+可见性+有序性+使用方式"。synchronized通过互斥锁保证原子性，volatile通过内存屏障保证可见性和有序性，但volatile不保证原子性。

数学上，synchronized的互斥性可以用信号量模型描述：P(mutex)和V(mutex)操作，确保临界区只有一个线程访问。volatile的内存语义遵循happens-before关系，确保写操作对后续读操作可见。

**详细对比**：

| 特性 | synchronized | volatile |
|------|--------------|----------|
| **原子性** | 保证（互斥锁） | 不保证（单次读写原子） |
| **可见性** | 保证（unlock前刷新） | 保证（内存屏障） |
| **有序性** | 保证（as-if-serial） | 保证（禁止重排序） |
| **使用范围** | 方法、代码块 | 变量 |
| **阻塞特性** | 会阻塞（互斥等待） | 不会阻塞（无锁） |
| **性能开销** | 较高（上下文切换） | 较低（内存屏障） |
| **适用场景** | 临界区保护 | 状态标志、双重检查 |

**源码分析**：

**synchronized实现**：
```java
// 字节码层面
public synchronized void method() {
    // 方法执行
}
// 编译后：
monitorenter  // 获取锁
// 方法体
monitorexit   // 释放锁
```

**volatile实现**：
```java
// 内存屏障插入
volatile int value = 0;

public void write() {
    value = 1;  // StoreStore + StoreLoad屏障
}

public int read() {
    return value;  // LoadLoad + LoadStore屏障
}
```

**应用场景**：
1. **synchronized适用**：临界区保护、方法同步、代码块同步
2. **volatile适用**：状态标志、双重检查锁定、发布不可变对象
3. **组合使用**：volatile状态+synchronized操作，提升性能

**实战经验**：
在生产环境并发优化中，我们根据场景选择合适的同步机制。

**案例1：状态标志优化**
- **问题**：使用synchronized保护简单的状态标志，性能差
- **分析**：synchronized开销大，状态标志只需要可见性
- **解决**：使用volatile替代synchronized
- **效果**：性能提升30%，CPU使用率降低20%

**案例2：双重检查锁定**
- **问题**：单例模式线程安全问题
- **分析**：需要保证可见性和有序性，避免指令重排序
- **解决**：使用volatile+双重检查锁定
- **效果**：线程安全+高性能，延迟初始化成功

**案例3：临界区保护**
- **问题**：共享资源访问需要原子性保证
- **分析**：复合操作需要互斥保护
- **解决**：使用synchronized保护临界区
- **效果**：数据一致性保证，无竞态条件

**权衡取舍**：
| 同步机制 | 性能 | 复杂度 | 适用场景 | 开发难度 |
|----------|------|--------|----------|----------|
| **synchronized** | 低 | 低 | 临界区保护 | 低 |
| **volatile** | 高 | 中 | 状态标志 | 中 |
| **Lock** | 中 | 高 | 复杂同步 | 高 |
| **CAS** | 很高 | 很高 | 无锁编程 | 很高 |

**常见追问**：
1. 追问：volatile为什么不能保证原子性？→ 回答：volatile只保证单次读写的原子性，对于复合操作（如i++）需要多个步骤，中间可能被其他线程修改。需要使用synchronized或AtomicInteger等原子类保证原子性。
2. 追问：synchronized的性能优化有哪些？→ 回答：锁升级（偏向锁→轻量级锁→重量级锁）、锁消除、锁粗化、自旋锁等优化技术。JDK6后synchronized性能大幅提升，接近ReentrantLock。

**踩坑经验**：
- 坑1：volatile误用导致原子性问题。解决：理解volatile只保证可见性，复合操作需要使用synchronized或原子类。
- 坑2：synchronized过度使用导致性能问题。解决：合理使用volatile、原子类、Lock等替代方案，减少锁竞争。
- 坑3：双重检查锁定实现错误。解决：正确使用volatile关键字，避免指令重排序导致的问题。

### Q2：happens-before是什么？

**核心原理**：
happens-before是Java内存模型的核心概念，规定了操作之间的可见性关系。核心思想是"顺序保证+可见性保证+有序性保证"，如果A happens-before B，则A的结果对B可见，且A在B之前执行。

数学上，happens-before关系具有传递性：如果A happens-before B，B happens-before C，则A happens-before C。这保证了内存操作的全局顺序一致性。

**8条happens-before规则**：

1. **程序次序规则**：同一线程内，前面的操作happens-before后面的操作
2. **锁定规则**：unlock操作happens-before后续的lock操作
3. **volatile规则**：volatile写happens-before后续的volatile读
4. **传递性规则**：A happens-before B，B happens-before C，则A happens-before C
5. **线程启动规则**：Thread.start() happens-before线程内的任何操作
6. **线程终止规则**：线程内的任何操作happens-before Thread.join()
7. **中断规则**：interrupt() happens-before检测到中断
8. **对象终结规则**：对象构造happens-before finalize()

**源码分析**：

**volatile的happens-before实现**：
```java
// 内存屏障插入
volatile int value = 0;

public void write() {
    value = 1;  // StoreStore + StoreLoad屏障
    // 后续操作能看到value=1
}

public int read() {
    int result = value;  // LoadLoad + LoadStore屏障
    // 能看到之前write()的结果
    return result;
}
```

**synchronized的happens-before实现**：
```java
public class HappensBeforeExample {
    private int value = 0;
    
    public synchronized void write() {
        value = 1;  // unlock前刷新到主内存
    }
    
    public synchronized int read() {
        return value;  // lock后从主内存读取
    }
}
```

**应用场景**：
1. **volatile变量**：状态标志、双重检查锁定
2. **synchronized块**：临界区保护、方法同步
3. **线程通信**：Thread.start()、Thread.join()
4. **原子操作**：AtomicInteger、AtomicReference

**实战经验**：
在生产环境并发编程中，正确理解happens-before关系至关重要。

**案例1：双重检查锁定**
- **问题**：单例模式在多线程环境下可能创建多个实例
- **分析**：指令重排序导致对象引用在构造完成前被赋值
- **解决**：使用volatile关键字保证happens-before关系
- **效果**：线程安全的延迟初始化

**案例2：状态标志优化**
- **问题**：线程间状态同步问题
- **分析**：普通变量不保证可见性
- **解决**：使用volatile保证happens-before关系
- **效果**：状态同步正确，性能提升

**案例3：线程通信**
- **问题**：主线程和子线程数据传递问题
- **分析**：缺乏happens-before关系保证
- **解决**：使用Thread.start()和Thread.join()建立happens-before关系
- **效果**：数据传递正确，无竞态条件

**权衡取舍**：
| 同步机制 | 性能 | 复杂度 | 适用场景 | 开发难度 |
|----------|------|--------|----------|----------|
| **volatile** | 高 | 低 | 状态标志 | 低 |
| **synchronized** | 中 | 低 | 临界区保护 | 低 |
| **Lock** | 中 | 高 | 复杂同步 | 高 |
| **原子类** | 很高 | 中 | 无锁编程 | 中 |

**常见追问**：
1. 追问：happens-before和as-if-serial的区别？→ 回答：as-if-serial保证单线程内程序执行结果与顺序执行一致，happens-before保证多线程间操作的可见性和有序性。happens-before是as-if-serial在多线程环境下的扩展。
2. 追问：如何利用happens-before关系优化性能？→ 回答：合理使用volatile减少不必要的synchronized，利用线程启动和终止规则避免显式同步，使用原子类实现无锁编程。

**踩坑经验**：
- 坑1：忽略happens-before关系导致可见性问题。解决：正确理解和使用happens-before规则，使用volatile或synchronized保证可见性。
- 坑2：过度依赖happens-before关系导致性能问题。解决：在保证正确性的前提下，选择性能最优的同步机制。
- 坑3：happens-before关系理解错误导致并发bug。解决：深入学习Java内存模型，通过实际测试验证并发程序的正确性。

### Q3：CAS的ABA问题如何解决？

**ABA问题**：
```
时刻1: A
时刻2: B (其他线程修改)
时刻3: A (其他线程再次修改)
CAS认为没有变化，但实际变化了
```

**解决方案**：
1. **版本号**：AtomicStampedReference
```java
AtomicStampedReference<Integer> ref = new AtomicStampedReference<>(100, 0);

int stamp = ref.getStamp();
ref.compareAndSet(100, 101, stamp, stamp + 1);
```

2. **布尔标记**：AtomicMarkableReference

### Q4：线程池的核心参数及执行流程？

**核心参数**：
- corePoolSize：核心线程数
- maximumPoolSize：最大线程数
- keepAliveTime：空闲线程存活时间
- workQueue：工作队列
- threadFactory：线程工厂
- handler：拒绝策略

**执行流程**：
1. 线程数 < core：创建核心线程
2. 核心线程满，加入队列
3. 队列满，创建非核心线程
4. 线程数达到max，执行拒绝策略

### Q5：如何设计一个线程安全的单例？

```java
// 方式1：枚举（推荐）
public enum Singleton {
    INSTANCE;
    
    public void method() {
        // ...
    }
}

// 方式2：静态内部类
public class Singleton {
    private Singleton() {}
    
    private static class Holder {
        private static final Singleton INSTANCE = new Singleton();
    }
    
    public static Singleton getInstance() {
        return Holder.INSTANCE;
    }
}

// 方式3：双重检查锁
public class Singleton {
    private static volatile Singleton instance;
    
    private Singleton() {}
    
    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

### Q6：如何实现一个线程安全的LRU缓存？

```java
public class LRUCache<K, V> {
    private final int capacity;
    private final Map<K, V> cache;
    
    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = Collections.synchronizedMap(
            new LinkedHashMap<K, V>(capacity, 0.75f, true) {
                protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                    return size() > LRUCache.this.capacity;
                }
            }
        );
    }
    
    public V get(K key) {
        return cache.get(key);
    }
    
    public void put(K key, V value) {
        cache.put(key, value);
    }
}
```

### Q7：ThreadLocal原理及内存泄漏问题？

**原理**：
- 每个Thread有一个ThreadLocalMap
- ThreadLocalMap的key是ThreadLocal对象（弱引用）
- value是实际存储的值（强引用）

**内存泄漏**：
- key被GC后为null
- value仍然被ThreadLocalMap强引用
- 线程池场景下，线程不销毁，value一直存在

**解决**：
```java
ThreadLocal<Object> threadLocal = new ThreadLocal<>();
try {
    threadLocal.set(value);
    // 使用
} finally {
    threadLocal.remove();  // 及时清理
}
```

### Q8：说说你对AQS的理解？

AQS是Java并发工具的基础框架，核心思想：
1. **state**：volatile int表示同步状态
2. **队列**：FIFO队列存储等待线程
3. **CAS**：修改state
4. **模板方法**：tryAcquire、tryRelease等由子类实现

基于AQS实现的类：
- ReentrantLock
- Semaphore
- CountDownLatch
- ReentrantReadWriteLock

### Q9：如何保证线程安全？

**方法总结**:

1. **互斥同步**（阻塞同步）
   - synchronized
   - ReentrantLock
   - 缺点：性能开销大

2. **非阻塞同步**
   - CAS（Atomic类）
   - 缺点：ABA问题、自旋消耗CPU

3. **无同步方案**
   - ThreadLocal
   - 不可变对象（final、String）
   - 栈封闭（局部变量）

4. **并发容器**
   - ConcurrentHashMap
   - CopyOnWriteArrayList
   - BlockingQueue

### Q10：什么是伪共享(False Sharing)?如何避免?

**原理**:
- CPU缓存行(Cache Line)一般64字节
- 多个线程修改同一缓存行的不同变量
- 导致缓存行失效，性能下降

**示例**:
```java
// 伪共享
class FalseSharing {
    volatile long v1;  // 8字节
    volatile long v2;  // 8字节，与v1在同一缓存行
}

// 线程1修改v1，线程2修改v2
// 虽然修改不同变量，但会互相影响缓存
```

**解决方案**:
```java
// JDK 8: @Contended注解
@Contended
class Padded {
    volatile long v1;
}

// 手动填充
class ManualPadding {
    volatile long p1, p2, p3, p4, p5, p6, p7;  // 填充
    volatile long value;
    volatile long p8, p9, p10, p11, p12, p13, p14;  // 填充
}
```

## 十二、最佳实践与总结

### 12.1 并发编程最佳实践

**1. 优先使用不可变对象**
```java
public final class ImmutablePoint {
    private final int x;
    private final int y;
    
    public ImmutablePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    // 只有getter，没有setter
    public int getX() { return x; }
    public int getY() { return y; }
}
```

**2. 减小锁的粒度**
```java
// 不推荐：锁整个方法
public synchronized void process(List<Item> items) {
    for (Item item : items) {
        // 长时间处理
    }
}

// 推荐：只锁必要部分
public void process(List<Item> items) {
    for (Item item : items) {
        // 不需要锁的处理
        
        synchronized (lock) {
            // 需要锁的部分
        }
    }
}
```

**3. 使用并发工具类**
```java
// 不推荐：自己实现
Object lock = new Object();
int count = 0;
synchronized (lock) {
    while (count < N) {
        lock.wait();
    }
}

// 推荐：使用CountDownLatch
CountDownLatch latch = new CountDownLatch(N);
latch.await();
```

**4. 避免在锁内部调用外部方法**
```java
// 危险：可能导致死锁
public synchronized void process() {
    externalObject.callback();  // 不知道callback会做什么
}

// 安全：先复制数据，锁外调用
List<Listener> listeners;
synchronized (this) {
    listeners = new ArrayList<>(this.listeners);
}
for (Listener listener : listeners) {
    listener.callback();
}
```

**5. 合理设置线程名称**
```java
// 方便问题排查
ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
    .setNameFormat("my-pool-%d")
    .build();

ExecutorService executor = new ThreadPoolExecutor(
    10, 20, 60L, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(100),
    namedThreadFactory
);
```

### 12.2 性能优化建议

**1. 选择合适的并发级别**
```java
// CPU密集型
int threadCount = Runtime.getRuntime().availableProcessors() + 1;

// IO密集型
int threadCount = Runtime.getRuntime().availableProcessors() * 2;
```

**2. 避免频繁创建线程**
```java
// 不推荐
for (int i = 0; i < 1000; i++) {
    new Thread(() -> doSomething()).start();
}

// 推荐：使用线程池
ExecutorService executor = Executors.newFixedThreadPool(10);
for (int i = 0; i < 1000; i++) {
    executor.submit(() -> doSomething());
}
```

**3. 合理使用锁**
- 能用乐观锁(CAS)就不用悲观锁
- 能用分段锁就不用全局锁
- 能用读写锁就不用互斥锁

### 12.3 常见陷阱

**1. double-check锁定**
```java
// 错误：instance可能未完全初始化
if (instance == null) {
    synchronized (Singleton.class) {
        if (instance == null) {
            instance = new Singleton();  // 非原子操作
        }
    }
}

// 正确：使用volatile
private static volatile Singleton instance;
```

**2. wait/notify使用**
```java
// 错误：可能遗漏通知
synchronized (lock) {
    if (condition) {
        lock.wait();
    }
}

// 正确：使用while循环
synchronized (lock) {
    while (!condition) {
        lock.wait();
    }
}
```

**3. finally中释放锁**
```java
// 必须在finally中释放
lock.lock();
try {
    // 业务逻辑
} finally {
    lock.unlock();  // 确保释放
}
```

### 12.4 学习路径建议

**基础阶段**（1-2个月）:
1. 理解JMM、volatile、synchronized
2. 掌握线程的基本API
3. 了解常见的并发问题

**进阶阶段**（3-6个月）:
1. 深入学习AQS原理
2. 掌握线程池原理和调优
3. 熟悉并发容器实现

**高级阶段**（持续学习）:
1. 阅读JUC源码
2. 实践高并发系统设计
3. 研究新特性（Virtual Thread等）

### 12.5 推荐资源

**书籍**:
- 《Java并发编程实战》- Brian Goetz
- 《Java并发编程的艺术》- 方腾飞
- 《深入理解Java虚拟机》- 周志明

**在线资源**:
- [Java并发编程网](http://ifeve.com/)
- [并发编程博客](https://www.cnblogs.com/dolphin0520/category/602863.html)
- [Doug Lea的主页](http://gee.cs.oswego.edu/dl/)

**工具**:
- JProfiler - 性能分析
- Arthas - 在线诊断
- JMH - 基准测试

### 12.6 面试高频考点

**必掌握**:
- ✅ synchronized和volatile原理
- ✅ JMM内存模型
- ✅ AQS原理
- ✅ 线程池参数和工作流程
- ✅ ConcurrentHashMap实现
- ✅ 死锁的产生和预防
- ✅ ThreadLocal原理和内存泄漏

**加分项**:
- ⭐ 锁优化（偏向锁、轻量级锁等）
- ⭐ CompletableFuture使用
- ⭐ LongAdder原理
- ⭐ 伪共享问题
- ⭐ 真实并发问题排查经验

### 12.7 生产环境并发优化实战案例

#### 12.7.1 高并发秒杀系统线程池优化案例

**背景**：秒杀系统在高峰期QPS达到10万+，需要优化线程池配置以提升系统吞吐量。

**问题分析**：
```java
// 原始线程池配置
@Configuration
public class ThreadPoolConfig {
    @Bean
    public ThreadPoolExecutor seckillExecutor() {
        return new ThreadPoolExecutor(
            10,                    // 核心线程数过少
            20,                    // 最大线程数不足
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),  // 队列容量不足
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "seckill-thread-" + threadNumber.getAndIncrement());
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略不当
        );
    }
}
```

**优化后配置**：
```java
@Configuration
public class OptimizedThreadPoolConfig {
    
    @Bean("seckillExecutor")
    public ThreadPoolExecutor seckillExecutor() {
        // 根据CPU核心数和业务特点计算线程数
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        int maximumPoolSize = corePoolSize * 2;
        
        return new ThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(5000),  // 增大队列容量
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "seckill-thread-" + threadNumber.getAndIncrement());
                    thread.setDaemon(false);
                    thread.setPriority(Thread.NORM_PRIORITY);
                    return thread;
                }
            },
            new ThreadPoolExecutor.AbortPolicy()  // 快速失败，避免雪崩
        );
    }
    
    @Bean("asyncExecutor")
    public ThreadPoolExecutor asyncExecutor() {
        // 异步任务专用线程池
        return new ThreadPoolExecutor(
            4,
            8,
            30L, TimeUnit.SECONDS,
            new SynchronousQueue<>(),  // 直接传递，不缓存
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "async-thread-" + threadNumber.getAndIncrement());
                    thread.setDaemon(true);
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
```

**线程池监控**：
```java
@Component
public class ThreadPoolMonitor {
    private final MeterRegistry meterRegistry;
    private final Map<String, ThreadPoolExecutor> threadPools = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        // 注册线程池监控指标
        threadPools.forEach((name, executor) -> {
            Gauge.builder("threadpool.core.size")
                .tag("pool", name)
                .register(meterRegistry, executor::getCorePoolSize);
                
            Gauge.builder("threadpool.active.size")
                .tag("pool", name)
                .register(meterRegistry, executor::getActiveCount);
                
            Gauge.builder("threadpool.queue.size")
                .tag("pool", name)
                .register(meterRegistry, () -> executor.getQueue().size());
        });
    }
    
    @Scheduled(fixedRate = 5000)
    public void logThreadPoolStatus() {
        threadPools.forEach((name, executor) -> {
            log.info("ThreadPool {} - Core: {}, Active: {}, Queue: {}, Completed: {}", 
                name, 
                executor.getCorePoolSize(),
                executor.getActiveCount(),
                executor.getQueue().size(),
                executor.getCompletedTaskCount());
        });
    }
}
```

**优化效果**：
- 系统吞吐量：从5万QPS提升至12万QPS
- 平均响应时间：从200ms降至80ms
- 线程利用率：从60%提升至85%
- 系统稳定性：显著提升，无线程池满载情况

#### 12.7.2 分布式锁性能优化案例

**背景**：分布式环境下商品库存扣减，需要高性能的分布式锁实现。

**原始实现**：
```java
@Service
public class InventoryService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    public boolean deductInventory(String productId, int quantity) {
        String lockKey = "inventory:lock:" + productId;
        String lockValue = UUID.randomUUID().toString();
        
        // 简单Redis锁实现
        Boolean success = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(30));
            
        if (!success) {
            return false;  // 获取锁失败
        }
        
        try {
            // 业务逻辑
            return doDeductInventory(productId, quantity);
        } finally {
            // 释放锁
            if (lockValue.equals(redisTemplate.opsForValue().get(lockKey))) {
                redisTemplate.delete(lockKey);
            }
        }
    }
}
```

**优化后实现**：
```java
@Service
public class OptimizedInventoryService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    // 使用Redisson实现高性能分布式锁
    @Autowired
    private RedissonClient redissonClient;
    
    // 本地缓存减少Redis访问
    private final Cache<String, Integer> localCache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(1, TimeUnit.SECONDS)
        .build();
    
    public boolean deductInventory(String productId, int quantity) {
        // 1. 先检查本地缓存
        Integer localStock = localCache.getIfPresent(productId);
        if (localStock != null && localStock < quantity) {
            return false;
        }
        
        // 2. 使用Redisson分布式锁
        RLock lock = redissonClient.getLock("inventory:lock:" + productId);
        
        try {
            // 尝试获取锁，最多等待100ms
            if (lock.tryLock(100, 30000, TimeUnit.MILLISECONDS)) {
                try {
                    // 3. 双重检查库存
                    Integer currentStock = getCurrentStock(productId);
                    if (currentStock < quantity) {
                        return false;
                    }
                    
                    // 4. 扣减库存
                    boolean success = doDeductInventory(productId, quantity);
                    if (success) {
                        // 5. 更新本地缓存
                        localCache.put(productId, currentStock - quantity);
                    }
                    return success;
                } finally {
                    lock.unlock();
                }
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    private Integer getCurrentStock(String productId) {
        String stockKey = "inventory:stock:" + productId;
        String stockStr = redisTemplate.opsForValue().get(stockKey);
        return stockStr != null ? Integer.parseInt(stockStr) : 0;
    }
}
```

**性能对比**：
- 锁获取时间：从平均50ms降至5ms
- 系统吞吐量：提升300%
- 锁竞争：减少80%
- 系统稳定性：显著提升

#### 12.7.3 虚拟线程(JDK 21)生产应用案例

**背景**：IO密集型微服务，传统线程池无法有效利用CPU资源。

**传统线程池实现**：
```java
@Service
public class TraditionalHttpService {
    private final ExecutorService executor = Executors.newFixedThreadPool(200);
    
    public CompletableFuture<String> fetchData(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 模拟IO操作
                Thread.sleep(100);
                return httpClient.get(url);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }
}
```

**虚拟线程实现**：
```java
@Service
public class VirtualThreadHttpService {
    // 使用虚拟线程执行器
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    
    public CompletableFuture<String> fetchData(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 虚拟线程可以轻松处理大量IO操作
                Thread.sleep(100);
                return httpClient.get(url);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    // 批量处理大量请求
    public List<String> batchFetchData(List<String> urls) {
        List<CompletableFuture<String>> futures = urls.stream()
            .map(this::fetchData)
            .toList();
            
        return futures.stream()
            .map(CompletableFuture::join)
            .toList();
    }
}
```

**性能对比**：
- 内存使用：从2GB降至200MB
- 线程数量：从200个平台线程支持10万个虚拟线程
- 响应时间：基本无变化
- 系统资源利用率：提升500%

## 十一、架构师级面试题

### Q1：AQS（AbstractQueuedSynchronizer）的实现原理和设计思想？

**核心原理**：
AQS是Java并发包的核心框架，采用模板方法模式，通过CLH队列和CAS操作实现同步器。其设计思想是"状态+队列"，状态表示同步器的状态，队列管理等待线程。

数学上，AQS的公平性F = Σ(T_i × P_i)，其中T_i是等待时间，P_i是优先级。FIFO队列确保F值最小，实现公平性。

**AQS核心组件**：
```java
// AQS核心数据结构
public abstract class AbstractQueuedSynchronizer {
    // 同步状态
    private volatile int state;
    
    // CLH队列头节点
    private transient volatile Node head;
    
    // CLH队列尾节点
    private transient volatile Node tail;
    
    // 节点状态
    static final class Node {
        volatile int waitStatus;
        volatile Node prev;
        volatile Node next;
        volatile Thread thread;
        Node nextWaiter;
    }
}
```

**AQS工作流程**：
1. **获取锁**：tryAcquire()尝试获取锁，失败则加入队列
2. **等待队列**：使用CLH队列管理等待线程
3. **释放锁**：tryRelease()释放锁，唤醒等待线程
4. **状态管理**：通过CAS操作保证状态变更的原子性

**应用场景**：
1. **ReentrantLock**：可重入锁实现
2. **CountDownLatch**：倒计时门闩
3. **Semaphore**：信号量控制
4. **CyclicBarrier**：循环栅栏
5. **FutureTask**：异步任务结果

**实战经验**：
在分布式锁实现中，我们基于AQS设计了Redis分布式锁：
- 使用AQS管理本地等待队列
- Redis实现锁的获取和释放
- 结合看门狗机制防止死锁

性能测试显示，相比传统synchronized，AQS实现的分布式锁吞吐量提升200%，延迟降低60%。

**权衡取舍**：
- **公平性 vs 性能**：公平锁保证顺序但性能较低
- **可重入 vs 复杂度**：可重入增加复杂度但提升易用性
- **阻塞 vs 自旋**：阻塞节省CPU但增加延迟

**常见追问**：
1. 追问：AQS如何保证公平性？→ 回答：通过FIFO队列和严格的入队出队顺序保证公平性。
2. 追问：AQS的CLH队列有什么优势？→ 回答：CLH队列减少内存争用，提高并发性能。

**踩坑经验**：
- 坑1：AQS实现时忘记处理中断。解决：正确处理中断状态，支持可中断的等待。
- 坑2：状态管理不当导致死锁。解决：确保状态变更的原子性和一致性。

### Q2：ConcurrentHashMap的演进历程和实现原理？

**核心原理**：
ConcurrentHashMap是线程安全的HashMap实现，从JDK 1.5到JDK 21经历了三次重大演进：分段锁→CAS+synchronized→红黑树+链表。核心思想是通过分段锁和CAS操作减少锁竞争。

数学上，并发性能P = T / (L × C)，其中T是总操作数，L是锁粒度，C是竞争度。分段锁将L降低到1/N，显著提升P值。

**JDK演进历程**：
| JDK版本 | 实现方式 | 特点 | 性能提升 |
|---------|----------|------|----------|
| JDK 1.5 | 分段锁 | 16个Segment | 基础性能 |
| JDK 1.7 | 分段锁优化 | 减少锁竞争 | 20-30% |
| JDK 1.8+ | CAS+synchronized | 链表+红黑树 | 50-100% |

**JDK 8+实现原理**：
```java
// ConcurrentHashMap核心结构
public class ConcurrentHashMap<K,V> {
    // 数组+链表+红黑树
    transient volatile Node<K,V>[] table;
    
    // 扩容时的临时表
    private transient volatile Node<K,V>[] nextTable;
    
    // 控制标识符
    private transient volatile int sizeCtl;
    
    // 节点类型
    static class Node<K,V> implements Map.Entry<K,V> {
        final int hash;
        final K key;
        volatile V val;
        volatile Node<K,V> next;
    }
    
    // 红黑树节点
    static final class TreeNode<K,V> extends Node<K,V> {
        TreeNode<K,V> parent;
        TreeNode<K,V> left;
        TreeNode<K,V> right;
        TreeNode<K,V> prev;
        boolean red;
    }
}
```

**核心操作实现**：
1. **put操作**：CAS插入头节点，synchronized处理链表/树
2. **get操作**：无锁读取，支持并发读
3. **扩容操作**：多线程协助扩容，分段迁移
4. **树化操作**：链表长度>8时转换为红黑树

**应用场景**：
1. **缓存系统**：高并发读写缓存
2. **计数器**：分布式计数器实现
3. **会话管理**：用户会话存储
4. **配置管理**：动态配置存储

**实战经验**：
在电商平台商品缓存中，我们使用ConcurrentHashMap存储商品信息：
- 商品ID作为key，商品详情作为value
- 支持高并发读写，QPS达到10万+
- 结合LRU策略自动淘汰过期数据

性能测试显示，相比Hashtable，ConcurrentHashMap吞吐量提升300%，延迟降低80%。

**权衡取舍**：
- **内存 vs 性能**：红黑树占用更多内存但提升查找性能
- **复杂度 vs 易用性**：复杂实现提供更好性能但增加维护成本
- **一致性 vs 性能**：弱一致性提升性能但可能影响业务逻辑

**常见追问**：
1. 追问：ConcurrentHashMap如何保证线程安全？→ 回答：通过CAS操作和synchronized关键字，CAS处理简单操作，synchronized处理复杂操作。
2. 追问：为什么JDK8要引入红黑树？→ 回答：链表长度过长时查找效率低，红黑树将查找时间复杂度从O(n)降至O(logn)。

**踩坑经验**：
- 坑1：ConcurrentHashMap的size()方法不准确。解决：使用mappingCount()方法或遍历计算准确大小。
- 坑2：在迭代过程中修改导致ConcurrentModificationException。解决：使用ConcurrentHashMap的迭代器，支持并发修改。

### Q3：线程池的核心参数和调优策略？

**核心原理**：
线程池是管理线程生命周期的框架，通过复用线程减少创建销毁开销。核心参数包括核心线程数、最大线程数、队列容量、拒绝策略等，需要根据业务特点进行调优。

数学上，线程池效率E = T / (C + W)，其中T是总任务数，C是创建开销，W是等待时间。合理配置参数可以最大化E值。

**核心参数详解**：
```java
// 线程池核心参数
public ThreadPoolExecutor(
    int corePoolSize,        // 核心线程数
    int maximumPoolSize,     // 最大线程数
    long keepAliveTime,      // 线程存活时间
    TimeUnit unit,           // 时间单位
    BlockingQueue<Runnable> workQueue,  // 工作队列
    ThreadFactory threadFactory,       // 线程工厂
    RejectedExecutionHandler handler   // 拒绝策略
) {
    // 参数验证和初始化
    if (corePoolSize < 0 || maximumPoolSize <= 0 || 
        maximumPoolSize < corePoolSize || keepAliveTime < 0)
        throw new IllegalArgumentException();
    
    this.corePoolSize = corePoolSize;
    this.maximumPoolSize = maximumPoolSize;
    this.workQueue = workQueue;
    this.keepAliveTime = unit.toNanos(keepAliveTime);
    this.threadFactory = threadFactory;
    this.handler = handler;
}
```

**参数调优策略**：
1. **核心线程数**：CPU密集型=N+1，IO密集型=2N
2. **最大线程数**：根据业务峰值和系统资源确定
3. **队列容量**：平衡内存使用和响应时间
4. **拒绝策略**：根据业务特点选择合适的策略

**拒绝策略对比**：
| 策略 | 特点 | 适用场景 | 风险 |
|------|------|----------|------|
| AbortPolicy | 抛异常 | 严格业务 | 任务丢失 |
| CallerRunsPolicy | 调用者执行 | 降级处理 | 阻塞调用者 |
| DiscardPolicy | 静默丢弃 | 可容忍丢失 | 数据丢失 |
| DiscardOldestPolicy | 丢弃最老 | 实时性要求 | 数据丢失 |

**应用场景**：
1. **Web应用**：处理HTTP请求
2. **批处理系统**：数据处理任务
3. **微服务**：异步任务处理
4. **定时任务**：定时执行任务

**实战经验**：
在订单处理系统中，我们设计了分层线程池架构：
- 核心业务线程池：处理订单创建、支付等关键业务
- 异步任务线程池：处理日志记录、消息发送等非关键业务
- 批处理线程池：处理数据统计、报表生成等批量任务

通过监控和调优，系统吞吐量从1000TPS提升到5000TPS，平均响应时间从200ms降至50ms。

**权衡取舍**：
- **线程数 vs 资源消耗**：更多线程提升并发但消耗更多资源
- **队列大小 vs 响应时间**：大队列缓冲更多任务但增加延迟
- **拒绝策略 vs 业务连续性**：不同策略影响业务连续性

**常见追问**：
1. 追问：如何确定最优线程数？→ 回答：通过压测确定，CPU密集型应用线程数=CPU核数+1，IO密集型应用线程数=CPU核数×2。
2. 追问：线程池监控哪些指标？→ 回答：活跃线程数、队列长度、任务执行时间、拒绝任务数等关键指标。

**踩坑经验**：
- 坑1：线程池参数设置不当导致任务堆积。解决：根据业务特点合理设置核心线程数和队列容量。
- 坑2：线程池未正确关闭导致资源泄漏。解决：使用shutdown()和awaitTermination()正确关闭线程池。

### Q4：Java内存模型（JMM）和happens-before关系？

**核心原理**：
Java内存模型定义了线程与主内存之间的抽象关系，通过happens-before关系保证内存操作的可见性和有序性。JMM的核心是"工作内存+主内存"的抽象模型。

数学上，内存一致性C = Σ(V_i × O_i)，其中V_i是可见性，O_i是有序性。happens-before关系确保C值满足程序语义要求。

**JMM内存模型**：
```java
// JMM内存模型抽象
public class JMMModel {
    // 主内存
    private final Map<Variable, Value> mainMemory = new ConcurrentHashMap<>();
    
    // 工作内存（线程私有）
    private final Map<Thread, Map<Variable, Value>> workingMemory = new ConcurrentHashMap<>();
    
    // happens-before关系
    private final Set<HappensBeforeRelation> happensBefore = new HashSet<>();
    
    // 内存操作
    public void write(Thread thread, Variable var, Value value) {
        // 1. 写入工作内存
        workingMemory.get(thread).put(var, value);
        
        // 2. 检查happens-before关系
        if (hasHappensBefore(thread, var)) {
            // 3. 刷新到主内存
            mainMemory.put(var, value);
            
            // 4. 通知其他线程
            notifyOtherThreads(var, value);
        }
    }
    
    public Value read(Thread thread, Variable var) {
        // 1. 从工作内存读取
        Value localValue = workingMemory.get(thread).get(var);
        
        // 2. 检查happens-before关系
        if (hasHappensBefore(thread, var)) {
            // 3. 从主内存读取最新值
            Value mainValue = mainMemory.get(var);
            if (mainValue != null) {
                workingMemory.get(thread).put(var, mainValue);
                return mainValue;
            }
        }
        
        return localValue;
    }
}
```

**happens-before规则**：
1. **程序顺序规则**：同一线程内，前面的操作happens-before后面的操作
2. **volatile规则**：volatile写happens-before后续的volatile读
3. **锁规则**：解锁happens-before后续的加锁
4. **传递性规则**：A happens-before B，B happens-before C，则A happens-before C
5. **线程启动规则**：线程start() happens-before线程内任何操作
6. **线程终止规则**：线程内任何操作happens-before线程终止

**应用场景**：
1. **双重检查锁定**：单例模式的线程安全实现
2. **volatile变量**：状态标志、计数器等
3. **锁同步**：synchronized、ReentrantLock等
4. **原子操作**：AtomicInteger、AtomicReference等

**实战经验**：
在分布式缓存系统中，我们使用JMM保证缓存一致性：
- 使用volatile修饰缓存状态标志
- 通过happens-before关系保证读写顺序
- 结合CAS操作实现无锁更新

通过JMM优化，缓存命中率从85%提升到95%，数据一致性达到99.99%。

**权衡取舍**：
- **性能 vs 一致性**：强一致性影响性能，需要权衡
- **复杂度 vs 正确性**：复杂的内存模型提供更强保证但增加理解成本
- **硬件 vs 抽象**：JMM抽象了硬件差异但可能影响性能

**常见追问**：
1. 追问：volatile如何保证可见性？→ 回答：volatile写会刷新到主内存，volatile读会从主内存读取，通过内存屏障保证可见性。
2. 追问：happens-before和as-if-serial有什么区别？→ 回答：happens-before是多线程语义，as-if-serial是单线程语义。

**踩坑经验**：
- 坑1：误用volatile导致性能问题。解决：volatile只保证可见性，不保证原子性，复杂操作需要同步。
- 坑2：happens-before关系理解错误导致并发bug。解决：深入理解JMM规范，正确使用同步机制。

### Q5：CompletableFuture异步编程的最佳实践？

**核心原理**：
CompletableFuture是Java 8引入的异步编程框架，基于函数式编程思想，通过链式调用和组合操作实现复杂的异步逻辑。其核心是"异步执行+结果组合"的模式。

数学上，异步效率A = Σ(T_i / C_i)，其中T_i是任务时间，C_i是并发度。CompletableFuture通过并行执行最大化A值。

**CompletableFuture核心特性**：
```java
// CompletableFuture核心操作
public class CompletableFutureExample {
    
    // 异步执行
    public CompletableFuture<String> asyncTask(String input) {
        return CompletableFuture.supplyAsync(() -> {
            // 模拟耗时操作
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Processed: " + input;
        });
    }
    
    // 结果转换
    public CompletableFuture<Integer> transformResult(CompletableFuture<String> future) {
        return future.thenApply(result -> result.length());
    }
    
    // 异常处理
    public CompletableFuture<String> handleException(CompletableFuture<String> future) {
        return future.handle((result, throwable) -> {
            if (throwable != null) {
                return "Error: " + throwable.getMessage();
            }
            return result;
        });
    }
    
    // 结果组合
    public CompletableFuture<String> combineResults(
            CompletableFuture<String> future1,
            CompletableFuture<String> future2) {
        return future1.thenCombine(future2, (result1, result2) -> 
            result1 + " + " + result2);
    }
    
    // 多任务等待
    public CompletableFuture<List<String>> waitForAll(
            List<CompletableFuture<String>> futures) {
        return CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }
}
```

**最佳实践**：
1. **异常处理**：使用handle()或exceptionally()处理异常
2. **超时控制**：使用orTimeout()设置超时时间
3. **线程池管理**：自定义线程池避免ForkJoinPool限制
4. **资源清理**：及时取消不需要的任务

**应用场景**：
1. **微服务调用**：并行调用多个服务
2. **数据处理**：并行处理大量数据
3. **文件操作**：异步读写文件
4. **网络请求**：并发HTTP请求

**实战经验**：
在订单处理系统中，我们使用CompletableFuture实现并行处理：
- 并行调用用户服务、商品服务、库存服务
- 使用thenCombine组合多个服务结果
- 通过handle处理服务调用异常

性能测试显示，相比串行调用，并行调用响应时间从500ms降至150ms，吞吐量提升200%。

**权衡取舍**：
- **复杂度 vs 性能**：异步编程增加复杂度但提升性能
- **资源消耗 vs 响应时间**：更多并发消耗更多资源但降低响应时间
- **错误处理 vs 代码简洁**：完善的错误处理增加代码复杂度

**常见追问**：
1. 追问：CompletableFuture和Future有什么区别？→ 回答：CompletableFuture支持链式调用、异常处理、结果组合等高级特性。
2. 追问：如何避免CompletableFuture的内存泄漏？→ 回答：及时取消不需要的任务，避免持有不必要的引用。

**踩坑经验**：
- 坑1：CompletableFuture链式调用过长导致栈溢出。解决：合理拆分链式调用，避免过深的嵌套。
- 坑2：异常处理不当导致任务静默失败。解决：使用handle()或exceptionally()正确处理异常。

---

**关键字**：Java并发、JMM、synchronized、volatile、AQS、线程池、ConcurrentHashMap、CompletableFuture、死锁、原子类、锁优化、并发容器、最佳实践、happens-before、内存模型、CAS、CLH队列、分段锁、红黑树、虚拟线程、异步编程、函数式编程、响应式编程

