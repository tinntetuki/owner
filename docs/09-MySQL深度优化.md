# MySQL深度优化

## 目录
- [一、索引](#一索引)
- [二、锁机制](#二锁机制)
- [三、事务](#三事务)
- [四、执行计划](#四执行计划)
- [五、SQL优化](#五sql优化)
- [六、分库分表](#六分库分表)
- [七、主从复制](#七主从复制)
- [八、高频面试题](#八高频面试题)

## 一、MySQL存储引擎深度解析

### 1.1 InnoDB存储引擎架构原理

#### 1.1.1 InnoDB整体架构

**InnoDB存储引擎架构图**：
```
┌─────────────────────────────────────────────────────────┐
│                    InnoDB存储引擎                        │
├─────────────────────────────────────────────────────────┤
│  Buffer Pool (缓冲池)     │  Log Buffer (日志缓冲)      │
│  ┌─────────────────────┐  │  ┌─────────────────────┐    │
│  │   Data Pages        │  │  │   Redo Log Buffer   │    │
│  │   Index Pages       │  │  │   Undo Log Buffer   │    │
│  │   Adaptive Hash     │  │  └─────────────────────┘    │
│  └─────────────────────┘  │                            │
├─────────────────────────────────────────────────────────┤
│  Change Buffer (变更缓冲)  │  Double Write Buffer        │
│  ┌─────────────────────┐  │  ┌─────────────────────┐    │
│  │   Insert Buffer      │  │  │   Double Write      │    │
│  │   Delete Buffer      │  │  │   Pages             │    │
│  │   Purge Buffer       │  │  └─────────────────────┘    │
│  └─────────────────────┘  │                            │
├─────────────────────────────────────────────────────────┤
│  Thread Pool (线程池)     │  File I/O (文件I/O)         │
│  ┌─────────────────────┐  │  ┌─────────────────────┐    │
│  │   Master Thread     │  │  │   Data Files        │    │
│  │   IO Threads        │  │  │   Log Files         │    │
│  │   Purge Thread      │  │  │   Undo Files        │    │
│  │   Page Cleaner      │  │  └─────────────────────┘    │
│  └─────────────────────┘  │                            │
└─────────────────────────────────────────────────────────┘
```

#### 1.1.2 Buffer Pool深度解析

**Buffer Pool核心机制**：
```java
// Buffer Pool核心实现原理
public class BufferPoolAnalysis {
    
    // Buffer Pool结构
    public class BufferPool {
        // 缓冲池大小（默认128MB）
        private long bufferPoolSize = 128 * 1024 * 1024;
        
        // 页大小（默认16KB）
        private int pageSize = 16 * 1024;
        
        // 缓冲池页数
        private int bufferPoolPages = (int) (bufferPoolSize / pageSize);
        
        // LRU链表（最近最少使用）
        private LRUList lruList;
        
        // Free链表（空闲页）
        private FreeList freeList;
        
        // Flush链表（脏页）
        private FlushList flushList;
        
        // 页哈希表（快速定位）
        private HashMap<PageKey, BufferPage> pageHash;
    }
    
    // LRU链表实现
    public class LRUList {
        private BufferPage head;  // 最新访问的页
        private BufferPage tail;  // 最久未访问的页
        
        // 将页移到链表头部
        public void moveToHead(BufferPage page) {
            if (page == head) return;
            
            // 从当前位置移除
            if (page.prev != null) {
                page.prev.next = page.next;
            }
            if (page.next != null) {
                page.next.prev = page.prev;
            }
            
            // 插入到头部
            page.next = head;
            page.prev = null;
            if (head != null) {
                head.prev = page;
            }
            head = page;
            
            if (tail == null) {
                tail = page;
            }
        }
        
        // 淘汰最久未使用的页
        public BufferPage evictOldest() {
            if (tail == null) return null;
            
            BufferPage evicted = tail;
            tail = tail.prev;
            if (tail != null) {
                tail.next = null;
            } else {
                head = null;
            }
            
            return evicted;
        }
    }
}
```

**Buffer Pool优化参数**：
```sql
-- 缓冲池大小（建议设置为内存的70-80%）
SET GLOBAL innodb_buffer_pool_size = 4G;

-- 缓冲池实例数（减少锁竞争）
SET GLOBAL innodb_buffer_pool_instances = 8;

-- 预读参数
SET GLOBAL innodb_read_ahead_threshold = 56;
SET GLOBAL innodb_random_read_ahead = OFF;

-- 刷新策略
SET GLOBAL innodb_old_blocks_pct = 37;  -- 老年代比例
SET GLOBAL innodb_old_blocks_time = 1000;  -- 老年代时间
```

#### 1.1.3 事务系统深度解析

**事务ID分配机制**：
```java
// 事务ID分配器
public class TransactionIdAllocator {
    private static final long MAX_TRX_ID = 281474976710655L;  // 2^48-1
    private static AtomicLong nextTrxId = new AtomicLong(1);
    
    // 分配新的事务ID
    public static long allocateTrxId() {
        long trxId = nextTrxId.getAndIncrement();
        if (trxId > MAX_TRX_ID) {
            throw new RuntimeException("Transaction ID overflow");
        }
        return trxId;
    }
    
    // 检查事务ID是否有效
    public static boolean isValidTrxId(long trxId) {
        return trxId > 0 && trxId <= MAX_TRX_ID;
    }
}
```

**Undo Log深度解析**：
```java
// Undo Log记录结构
public class UndoLogRecord {
    private long trxId;           // 事务ID
    private long undoNo;          // Undo记录号
    private long tableId;         // 表ID
    private long pageNo;          // 页号
    private long offset;          // 页内偏移
    private byte[] oldData;       // 旧数据
    private byte[] newData;       // 新数据
    private long prevUndoNo;      // 前一个Undo记录号
    private boolean isDelete;     // 是否为删除操作
}

// Undo Log管理器
public class UndoLogManager {
    private Map<Long, List<UndoLogRecord>> trxUndoLogs = new ConcurrentHashMap<>();
    
    // 添加Undo记录
    public void addUndoLog(long trxId, UndoLogRecord record) {
        trxUndoLogs.computeIfAbsent(trxId, k -> new ArrayList<>()).add(record);
    }
    
    // 回滚事务
    public void rollbackTransaction(long trxId) {
        List<UndoLogRecord> undoLogs = trxUndoLogs.get(trxId);
        if (undoLogs != null) {
            // 按逆序执行回滚
            for (int i = undoLogs.size() - 1; i >= 0; i--) {
                UndoLogRecord record = undoLogs.get(i);
                executeUndo(record);
            }
        }
        trxUndoLogs.remove(trxId);
    }
    
    // 执行回滚操作
    private void executeUndo(UndoLogRecord record) {
        if (record.isDelete()) {
            // 删除操作的回滚是插入
            insertData(record.getTableId(), record.getPageNo(), 
                      record.getOffset(), record.getOldData());
        } else {
            // 更新操作的回滚是恢复旧值
            updateData(record.getTableId(), record.getPageNo(), 
                      record.getOffset(), record.getOldData());
        }
    }
}
```

### 1.2 索引实现原理深度解析

#### 1.2.1 B+树索引深度实现

**B+树节点结构**：
```java
// B+树节点实现
public class BPlusTreeNode {
    private boolean isLeaf;           // 是否为叶子节点
    private List<KeyValue> keys;     // 键值对列表
    private BPlusTreeNode parent;    // 父节点
    private List<BPlusTreeNode> children; // 子节点列表（非叶子节点）
    private BPlusTreeNode next;      // 下一个叶子节点（叶子节点）
    private BPlusTreeNode prev;      // 前一个叶子节点（叶子节点）
    
    // 键值对结构
    public static class KeyValue {
        private Object key;           // 键
        private Object value;         // 值（叶子节点存储数据，非叶子节点存储指针）
        private long pageNo;          // 页号
        private long offset;          // 页内偏移
    }
    
    // 插入键值对
    public void insert(KeyValue kv) {
        int index = binarySearch(kv.key);
        keys.add(index, kv);
        
        // 如果节点满了，需要分裂
        if (keys.size() > maxKeys) {
            split();
        }
    }
    
    // 分裂节点
    private void split() {
        int mid = keys.size() / 2;
        KeyValue midKey = keys.get(mid);
        
        // 创建新节点
        BPlusTreeNode newNode = new BPlusTreeNode();
        newNode.isLeaf = this.isLeaf;
        
        // 移动一半键值对到新节点
        for (int i = mid; i < keys.size(); i++) {
            newNode.keys.add(keys.get(i));
        }
        keys.subList(mid, keys.size()).clear();
        
        // 更新父节点
        if (parent != null) {
            parent.insert(new KeyValue(midKey.key, newNode));
        } else {
            // 创建新的根节点
            BPlusTreeNode newRoot = new BPlusTreeNode();
            newRoot.isLeaf = false;
            newRoot.keys.add(new KeyValue(midKey.key, this));
            newRoot.keys.add(new KeyValue(midKey.key, newNode));
            this.parent = newRoot;
            newNode.parent = newRoot;
        }
    }
}
```

**B+树查找算法**：
```java
// B+树查找实现
public class BPlusTreeSearch {
    
    // 精确查找
    public Object search(BPlusTreeNode root, Object key) {
        BPlusTreeNode current = root;
        
        while (!current.isLeaf) {
            // 在非叶子节点中查找
            int index = binarySearch(current.keys, key);
            current = current.children.get(index);
        }
        
        // 在叶子节点中查找
        int index = binarySearch(current.keys, key);
        if (index < current.keys.size() && 
            current.keys.get(index).key.equals(key)) {
            return current.keys.get(index).value;
        }
        
        return null;
    }
    
    // 范围查找
    public List<Object> rangeSearch(BPlusTreeNode root, Object startKey, Object endKey) {
        List<Object> result = new ArrayList<>();
        BPlusTreeNode current = root;
        
        // 找到起始位置
        while (!current.isLeaf) {
            int index = binarySearch(current.keys, startKey);
            current = current.children.get(index);
        }
        
        // 从起始位置开始遍历
        while (current != null) {
            for (KeyValue kv : current.keys) {
                if (compare(kv.key, startKey) >= 0 && compare(kv.key, endKey) <= 0) {
                    result.add(kv.value);
                } else if (compare(kv.key, endKey) > 0) {
                    return result;
                }
            }
            current = current.next;
        }
        
        return result;
    }
    
    // 二分查找
    private int binarySearch(List<KeyValue> keys, Object key) {
        int left = 0, right = keys.size() - 1;
        while (left <= right) {
            int mid = (left + right) / 2;
            int cmp = compare(keys.get(mid).key, key);
            if (cmp < 0) {
                left = mid + 1;
            } else if (cmp > 0) {
                right = mid - 1;
            } else {
                return mid;
            }
        }
        return left;
    }
}
```

#### 1.2.2 索引类型深度分析

**聚簇索引实现原理**：
```java
// 聚簇索引实现
public class ClusteredIndex {
    private BPlusTreeNode root;
    private long tableId;
    
    // 插入记录
    public void insert(Object key, Object[] data) {
        // 1. 查找插入位置
        BPlusTreeNode leaf = findLeaf(key);
        
        // 2. 检查键是否已存在
        if (containsKey(leaf, key)) {
            throw new DuplicateKeyException("Duplicate key: " + key);
        }
        
        // 3. 创建数据页
        DataPage dataPage = createDataPage(data);
        
        // 4. 插入到B+树
        KeyValue kv = new KeyValue(key, dataPage);
        leaf.insert(kv);
        
        // 5. 如果节点分裂，向上传播
        if (leaf.keys.size() > maxKeys) {
            propagateSplit(leaf);
        }
    }
    
    // 查找记录
    public Object[] search(Object key) {
        BPlusTreeNode leaf = findLeaf(key);
        KeyValue kv = findKeyValue(leaf, key);
        
        if (kv != null) {
            DataPage dataPage = (DataPage) kv.value;
            return dataPage.getData();
        }
        
        return null;
    }
}
```

**非聚簇索引实现原理**：
```java
// 非聚簇索引实现
public class NonClusteredIndex {
    private BPlusTreeNode root;
    private long tableId;
    private int columnIndex;  // 索引列在表中的位置
    
    // 插入记录
    public void insert(Object key, long rowId) {
        BPlusTreeNode leaf = findLeaf(key);
        
        // 非聚簇索引存储的是主键值，不是完整数据
        KeyValue kv = new KeyValue(key, rowId);
        leaf.insert(kv);
        
        if (leaf.keys.size() > maxKeys) {
            propagateSplit(leaf);
        }
    }
    
    // 查找记录（需要回表）
    public Object[] search(Object key) {
        BPlusTreeNode leaf = findLeaf(key);
        KeyValue kv = findKeyValue(leaf, key);
        
        if (kv != null) {
            long rowId = (Long) kv.value;
            // 回表查询完整数据
            return getRowByRowId(rowId);
        }
        
        return null;
    }
}
```

### 1.3 索引优化深度策略

#### 1.3.1 索引选择性分析

**选择性计算公式**：
```sql
-- 计算索引选择性
SELECT 
    COUNT(DISTINCT column_name) / COUNT(*) AS selectivity,
    COUNT(DISTINCT column_name) AS distinct_values,
    COUNT(*) AS total_rows
FROM table_name;

-- 多列索引选择性分析
SELECT 
    COUNT(DISTINCT CONCAT(col1, col2, col3)) / COUNT(*) AS combined_selectivity,
    COUNT(DISTINCT col1) / COUNT(*) AS col1_selectivity,
    COUNT(DISTINCT col2) / COUNT(*) AS col2_selectivity,
    COUNT(DISTINCT col3) / COUNT(*) AS col3_selectivity
FROM table_name;
```

**索引设计最佳实践**：
```sql
-- 1. 高选择性列优先
CREATE INDEX idx_user_email ON users(email);  -- 邮箱唯一性高

-- 2. 复合索引列顺序优化
-- 查询: WHERE status = 1 AND created_time > '2024-01-01' AND user_type = 'VIP'
-- 设计: (status, user_type, created_time) - 等值查询在前
CREATE INDEX idx_user_status_type_time ON users(status, user_type, created_time);

-- 3. 覆盖索引设计
-- 查询: SELECT user_id, name, email FROM users WHERE status = 1
-- 设计: (status, user_id, name, email) - 包含查询的所有列
CREATE INDEX idx_user_cover ON users(status, user_id, name, email);

-- 4. 部分索引（MySQL 8.0+）
-- 只为活跃用户创建索引
CREATE INDEX idx_active_users ON users(email) WHERE status = 'ACTIVE';
```

#### 1.3.2 索引维护优化

**索引统计信息更新**：
```sql
-- 手动更新统计信息
ANALYZE TABLE users;

-- 查看索引统计信息
SELECT 
    table_name,
    index_name,
    cardinality,
    sub_part,
    packed,
    nullable,
    index_type
FROM information_schema.statistics 
WHERE table_schema = 'your_database'
ORDER BY table_name, seq_in_index;
```

**索引碎片整理**：
```sql
-- 检查表碎片
SELECT 
    table_name,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'Size (MB)',
    ROUND((data_free / 1024 / 1024), 2) AS 'Free Space (MB)',
    ROUND((data_free / (data_length + index_length)) * 100, 2) AS 'Fragmentation %'
FROM information_schema.tables 
WHERE table_schema = 'your_database'
AND data_free > 0;

-- 整理表碎片
OPTIMIZE TABLE users;
```

## 二、索引

### 1.2 索引设计原则

**1. 选择性高的列**：
```sql
-- 计算选择性
SELECT COUNT(DISTINCT column) / COUNT(*) FROM table;
-- 选择性越接近1越好
```

**2. 最左前缀原则**：
```sql
-- 联合索引 (a, b, c)
CREATE INDEX idx_abc ON table(a, b, c);

-- 能用到索引
SELECT * FROM table WHERE a = 1;
SELECT * FROM table WHERE a = 1 AND b = 2;
SELECT * FROM table WHERE a = 1 AND b = 2 AND c = 3;

-- 不能用到索引
SELECT * FROM table WHERE b = 2;
SELECT * FROM table WHERE c = 3;
```

**3. 覆盖索引**：
```sql
-- 索引包含查询的所有列，无需回表
CREATE INDEX idx_name_age ON user(name, age);

-- 使用覆盖索引
SELECT name, age FROM user WHERE name = 'John';
```

### 1.3 索引失效场景

```sql
-- 1. 使用函数
SELECT * FROM user WHERE YEAR(create_time) = 2024;  -- 失效
SELECT * FROM user WHERE create_time >= '2024-01-01';  -- 生效

-- 2. 类型转换
SELECT * FROM user WHERE phone = 13800138000;  -- phone是varchar，失效
SELECT * FROM user WHERE phone = '13800138000';  -- 生效

-- 3. 模糊查询前缀通配
SELECT * FROM user WHERE name LIKE '%John';  -- 失效
SELECT * FROM user WHERE name LIKE 'John%';  -- 生效

-- 4. OR条件
SELECT * FROM user WHERE name = 'John' OR age = 20;  -- 可能失效
-- 改写为
SELECT * FROM user WHERE name = 'John'
UNION
SELECT * FROM user WHERE age = 20;

-- 5. !=、<>、NOT IN
SELECT * FROM user WHERE age != 20;  -- 可能失效
```

## 二、锁机制

### 2.1 锁类型

**全局锁**：
```sql
FLUSH TABLES WITH READ LOCK;  -- 整个库只读
UNLOCK TABLES;
```

**表锁**：
```sql
LOCK TABLES user READ;   -- 表读锁
LOCK TABLES user WRITE;  -- 表写锁
UNLOCK TABLES;
```

**行锁**（InnoDB）：
```sql
-- 共享锁（S锁）
SELECT * FROM user WHERE id = 1 LOCK IN SHARE MODE;

-- 排他锁（X锁）
SELECT * FROM user WHERE id = 1 FOR UPDATE;
```

### 2.2 InnoDB锁

**记录锁（Record Lock）**：
```sql
-- 锁定单行记录
SELECT * FROM user WHERE id = 1 FOR UPDATE;
```

**间隙锁（Gap Lock）**：
```sql
-- 锁定id在(1, 10)之间的间隙
SELECT * FROM user WHERE id > 1 AND id < 10 FOR UPDATE;
```

**Next-Key Lock**（记录锁 + 间隙锁）：
```sql
-- 默认锁类型，防止幻读
SELECT * FROM user WHERE id >= 1 AND id <= 10 FOR UPDATE;
```

### 2.3 死锁

**死锁示例**：
```sql
-- 事务1
BEGIN;
UPDATE user SET age = 20 WHERE id = 1;  -- 锁id=1
UPDATE user SET age = 20 WHERE id = 2;  -- 等待id=2

-- 事务2
BEGIN;
UPDATE user SET age = 30 WHERE id = 2;  -- 锁id=2
UPDATE user SET age = 30 WHERE id = 1;  -- 等待id=1，死锁！
```

**避免死锁**：
1. 固定加锁顺序
2. 使用超时机制
3. 减小事务粒度
4. 使用乐观锁

## 三、事务

### 3.1 ACID特性

- **Atomicity**（原子性）：全部成功或全部失败
- **Consistency**（一致性）：数据完整性约束
- **Isolation**（隔离性）：并发事务互不影响
- **Durability**（持久性）：提交后永久保存

### 3.2 隔离级别

| 隔离级别 | 脏读 | 不可重复读 | 幻读 |
|---------|-----|----------|-----|
| READ UNCOMMITTED | ✓ | ✓ | ✓ |
| READ COMMITTED | ✗ | ✓ | ✓ |
| REPEATABLE READ | ✗ | ✗ | ✓(InnoDB解决) |
| SERIALIZABLE | ✗ | ✗ | ✗ |

**设置隔离级别**：
```sql
-- 查看隔离级别
SELECT @@transaction_isolation;

-- 设置隔离级别
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
```

### 3.3 MVCC（多版本并发控制）

**原理**：
- 每行记录保存多个版本
- 事务读取时根据快照读取对应版本

**Undo Log**：
```
当前版本: id=1, name='John', age=30, trx_id=100
旧版本1:  id=1, name='John', age=25, trx_id=90
旧版本2:  id=1, name='Tom',  age=25, trx_id=80
```

**Read View**：
- 事务开始时创建快照
- 根据trx_id判断版本可见性

## 四、执行计划

### 4.1 EXPLAIN分析

```sql
EXPLAIN SELECT * FROM user WHERE name = 'John';
```

**关键字段**：

| 字段 | 说明 | 重要值 |
|------|------|--------|
| type | 连接类型 | system > const > eq_ref > ref > range > index > ALL |
| key | 使用的索引 | 实际使用的索引 |
| rows | 扫描行数 | 越少越好 |
| Extra | 额外信息 | Using index（覆盖索引），Using filesort（文件排序） |

**type类型**：
```sql
-- system: 表只有一行
SELECT * FROM (SELECT 1) t;

-- const: 主键或唯一索引
SELECT * FROM user WHERE id = 1;

-- eq_ref: 多表join，主键或唯一索引
SELECT * FROM user u JOIN order o ON u.id = o.user_id;

-- ref: 非唯一索引
SELECT * FROM user WHERE name = 'John';

-- range: 范围查询
SELECT * FROM user WHERE id > 1 AND id < 100;

-- index: 索引扫描
SELECT id FROM user;

-- ALL: 全表扫描
SELECT * FROM user;
```

## 五、SQL优化深度解析

### 5.1 查询优化器深度原理

#### 5.1.1 查询优化器架构

**MySQL查询优化器架构**：
```
┌─────────────────────────────────────────────────────────┐
│                MySQL查询优化器                           │
├─────────────────────────────────────────────────────────┤
│  Parser (解析器)          │  Optimizer (优化器)         │
│  ┌─────────────────────┐  │  ┌─────────────────────┐    │
│  │   SQL Parser        │  │  │   Query Rewriter   │    │
│  │   AST Builder       │  │  │   Cost Calculator  │    │
│  └─────────────────────┘  │  │   Plan Generator    │    │
│                           │  └─────────────────────┘    │
├─────────────────────────────────────────────────────────┤
│  Cost Model (成本模型)    │  Statistics (统计信息)       │
│  ┌─────────────────────┐  │  ┌─────────────────────┐    │
│  │   CPU Cost          │  │  │   Table Stats       │    │
│  │   I/O Cost          │  │  │   Index Stats       │    │
│  │   Memory Cost       │  │  │   Histogram Stats   │    │
│  └─────────────────────┘  │  └─────────────────────┘    │
├─────────────────────────────────────────────────────────┤
│  Execution Engine (执行引擎)                            │
│  ┌─────────────────────┐  ┌─────────────────────┐      │
│  │   Iterator Model    │  │   Volcano Model      │      │
│  │   Pipeline Exec    │  │   Materialization    │      │
│  └─────────────────────┘  └─────────────────────┘      │
└─────────────────────────────────────────────────────────┘
```

#### 5.1.2 查询重写优化

**查询重写规则**：
```java
// 查询重写器实现
public class QueryRewriter {
    
    // 子查询优化
    public QueryNode optimizeSubquery(QueryNode query) {
        // 1. 子查询转JOIN
        if (isCorrelatedSubquery(query)) {
            return convertToJoin(query);
        }
        
        // 2. EXISTS转IN
        if (isExistsSubquery(query)) {
            return convertToIn(query);
        }
        
        // 3. 子查询物化
        if (isComplexSubquery(query)) {
            return materializeSubquery(query);
        }
        
        return query;
    }
    
    // 谓词下推优化
    public QueryNode pushDownPredicates(QueryNode query) {
        // 将WHERE条件尽可能下推到数据源
        List<Predicate> predicates = extractPredicates(query.getWhereClause());
        
        for (Predicate pred : predicates) {
            if (canPushDown(pred, query.getFromClause())) {
                pushDownToTable(pred, query.getFromClause());
            }
        }
        
        return query;
    }
    
    // 常量折叠
    public QueryNode constantFolding(QueryNode query) {
        // 计算常量表达式
        Expression optimized = foldConstants(query.getSelectList());
        query.setSelectList(optimized);
        
        // 简化WHERE条件
        Expression simplified = simplifyWhereClause(query.getWhereClause());
        query.setWhereClause(simplified);
        
        return query;
    }
}
```

#### 5.1.3 成本模型深度分析

**成本计算器实现**：
```java
// 成本计算器
public class CostCalculator {
    
    // 计算查询总成本
    public double calculateTotalCost(QueryPlan plan) {
        double totalCost = 0;
        
        for (QueryNode node : plan.getNodes()) {
            totalCost += calculateNodeCost(node);
        }
        
        return totalCost;
    }
    
    // 计算节点成本
    private double calculateNodeCost(QueryNode node) {
        double cpuCost = calculateCpuCost(node);
        double ioCost = calculateIoCost(node);
        double memoryCost = calculateMemoryCost(node);
        
        return cpuCost + ioCost + memoryCost;
    }
    
    // CPU成本计算
    private double calculateCpuCost(QueryNode node) {
        double rows = node.getEstimatedRows();
        double operations = node.getOperationCount();
        
        // CPU成本 = 行数 × 操作数 × CPU权重
        return rows * operations * CPU_WEIGHT;
    }
    
    // I/O成本计算
    private double calculateIoCost(QueryNode node) {
        double pages = node.getEstimatedPages();
        double randomIo = node.getRandomIoCount();
        double sequentialIo = node.getSequentialIoCount();
        
        // I/O成本 = 随机I/O × 随机权重 + 顺序I/O × 顺序权重
        return randomIo * RANDOM_IO_WEIGHT + sequentialIo * SEQUENTIAL_IO_WEIGHT;
    }
    
    // 内存成本计算
    private double calculateMemoryCost(QueryNode node) {
        double memoryUsage = node.getEstimatedMemoryUsage();
        
        // 内存成本 = 内存使用量 × 内存权重
        return memoryUsage * MEMORY_WEIGHT;
    }
}
```

### 5.2 执行计划深度分析

#### 5.2.1 执行计划生成过程

**执行计划生成器**：
```java
// 执行计划生成器
public class ExecutionPlanGenerator {
    
    // 生成执行计划
    public ExecutionPlan generatePlan(QueryNode query) {
        // 1. 生成所有可能的执行计划
        List<ExecutionPlan> plans = generateAllPlans(query);
        
        // 2. 计算每个计划的成本
        for (ExecutionPlan plan : plans) {
            plan.setCost(calculatePlanCost(plan));
        }
        
        // 3. 选择成本最低的计划
        return selectBestPlan(plans);
    }
    
    // 生成所有可能的执行计划
    private List<ExecutionPlan> generateAllPlans(QueryNode query) {
        List<ExecutionPlan> plans = new ArrayList<>();
        
        // 单表查询计划
        if (query.isSingleTable()) {
            plans.addAll(generateSingleTablePlans(query));
        }
        
        // 多表JOIN计划
        if (query.isMultiTable()) {
            plans.addAll(generateJoinPlans(query));
        }
        
        // 子查询计划
        if (query.hasSubqueries()) {
            plans.addAll(generateSubqueryPlans(query));
        }
        
        return plans;
    }
    
    // 生成单表查询计划
    private List<ExecutionPlan> generateSingleTablePlans(QueryNode query) {
        List<ExecutionPlan> plans = new ArrayList<>();
        
        // 全表扫描计划
        ExecutionPlan fullScan = new ExecutionPlan();
        fullScan.addNode(new FullTableScanNode(query.getTable()));
        plans.add(fullScan);
        
        // 索引扫描计划
        for (Index index : query.getAvailableIndexes()) {
            ExecutionPlan indexScan = new ExecutionPlan();
            indexScan.addNode(new IndexScanNode(index, query.getConditions()));
            plans.add(indexScan);
        }
        
        return plans;
    }
    
    // 生成JOIN计划
    private List<ExecutionPlan> generateJoinPlans(QueryNode query) {
        List<ExecutionPlan> plans = new ArrayList<>();
        
        // 嵌套循环JOIN
        ExecutionPlan nestedLoop = new ExecutionPlan();
        nestedLoop.addNode(new NestedLoopJoinNode(query.getTables()));
        plans.add(nestedLoop);
        
        // 哈希JOIN
        ExecutionPlan hashJoin = new ExecutionPlan();
        hashJoin.addNode(new HashJoinNode(query.getTables()));
        plans.add(hashJoin);
        
        // 排序合并JOIN
        ExecutionPlan sortMerge = new ExecutionPlan();
        sortMerge.addNode(new SortMergeJoinNode(query.getTables()));
        plans.add(sortMerge);
        
        return plans;
    }
}
```

#### 5.2.2 执行计划优化策略

**执行计划优化器**：
```java
// 执行计划优化器
public class PlanOptimizer {
    
    // 优化执行计划
    public ExecutionPlan optimize(ExecutionPlan plan) {
        // 1. 谓词下推
        plan = pushDownPredicates(plan);
        
        // 2. 投影下推
        plan = pushDownProjections(plan);
        
        // 3. 连接重排序
        plan = reorderJoins(plan);
        
        // 4. 索引选择
        plan = selectOptimalIndexes(plan);
        
        // 5. 并行化
        plan = parallelize(plan);
        
        return plan;
    }
    
    // 谓词下推
    private ExecutionPlan pushDownPredicates(ExecutionPlan plan) {
        for (ExecutionNode node : plan.getNodes()) {
            if (node instanceof JoinNode) {
                JoinNode joinNode = (JoinNode) node;
                
                // 将JOIN条件下推到子节点
                List<Predicate> joinPredicates = extractJoinPredicates(joinNode);
                for (Predicate pred : joinPredicates) {
                    if (canPushDownToLeft(pred, joinNode.getLeft())) {
                        pushDownToLeft(pred, joinNode.getLeft());
                    }
                    if (canPushDownToRight(pred, joinNode.getRight())) {
                        pushDownToRight(pred, joinNode.getRight());
                    }
                }
            }
        }
        return plan;
    }
    
    // 连接重排序
    private ExecutionPlan reorderJoins(ExecutionPlan plan) {
        List<Table> tables = plan.getTables();
        
        // 使用动态规划算法重排序JOIN
        int n = tables.size();
        double[][] dp = new double[1 << n][n];
        int[][] parent = new int[1 << n][n];
        
        // 初始化
        for (int i = 0; i < n; i++) {
            dp[1 << i][i] = calculateTableCost(tables.get(i));
        }
        
        // 动态规划
        for (int mask = 1; mask < (1 << n); mask++) {
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) == 0) continue;
                
                int prevMask = mask ^ (1 << i);
                for (int j = 0; j < n; j++) {
                    if ((prevMask & (1 << j)) == 0) continue;
                    
                    double cost = dp[prevMask][j] + calculateJoinCost(tables.get(j), tables.get(i));
                    if (dp[mask][i] == 0 || cost < dp[mask][i]) {
                        dp[mask][i] = cost;
                        parent[mask][i] = j;
                    }
                }
            }
        }
        
        // 重构最优JOIN顺序
        return reconstructOptimalJoin(plan, dp, parent);
    }
}
```

### 5.3 慢查询定位与优化

#### 5.3.1 慢查询分析工具

**慢查询分析器**：
```java
// 慢查询分析器
public class SlowQueryAnalyzer {
    
    // 分析慢查询日志
    public List<SlowQuery> analyzeSlowQueries(String logFile) {
        List<SlowQuery> slowQueries = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            SlowQuery currentQuery = null;
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("# Time:")) {
                    if (currentQuery != null) {
                        slowQueries.add(currentQuery);
                    }
                    currentQuery = new SlowQuery();
                    currentQuery.setTimestamp(parseTimestamp(line));
                } else if (line.startsWith("# User@Host:")) {
                    currentQuery.setUserHost(parseUserHost(line));
                } else if (line.startsWith("# Query_time:")) {
                    currentQuery.setQueryTime(parseQueryTime(line));
                    currentQuery.setLockTime(parseLockTime(line));
                    currentQuery.setRowsSent(parseRowsSent(line));
                    currentQuery.setRowsExamined(parseRowsExamined(line));
                } else if (!line.startsWith("#")) {
                    if (currentQuery.getSql() == null) {
                        currentQuery.setSql(line);
                    } else {
                        currentQuery.setSql(currentQuery.getSql() + " " + line);
                    }
                }
            }
            
            if (currentQuery != null) {
                slowQueries.add(currentQuery);
            }
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to analyze slow query log", e);
        }
        
        return slowQueries;
    }
    
    // 分析慢查询模式
    public Map<String, SlowQueryPattern> analyzePatterns(List<SlowQuery> queries) {
        Map<String, SlowQueryPattern> patterns = new HashMap<>();
        
        for (SlowQuery query : queries) {
            String normalizedSql = normalizeSql(query.getSql());
            SlowQueryPattern pattern = patterns.computeIfAbsent(normalizedSql, 
                k -> new SlowQueryPattern(normalizedSql));
            
            pattern.addQuery(query);
        }
        
        return patterns;
    }
    
    // 生成优化建议
    public List<OptimizationSuggestion> generateSuggestions(SlowQueryPattern pattern) {
        List<OptimizationSuggestion> suggestions = new ArrayList<>();
        
        // 分析执行计划
        ExecutionPlan plan = analyzeExecutionPlan(pattern.getSampleSql());
        
        // 检查全表扫描
        if (hasFullTableScan(plan)) {
            suggestions.add(new OptimizationSuggestion(
                "Add index to avoid full table scan",
                "CREATE INDEX idx_" + pattern.getTableName() + "_" + pattern.getColumnName() + 
                " ON " + pattern.getTableName() + "(" + pattern.getColumnName() + ")"
            ));
        }
        
        // 检查索引使用
        if (hasUnusedIndexes(plan)) {
            suggestions.add(new OptimizationSuggestion(
                "Remove unused indexes",
                "DROP INDEX idx_unused ON " + pattern.getTableName()
            ));
        }
        
        // 检查JOIN优化
        if (hasInefficientJoins(plan)) {
            suggestions.add(new OptimizationSuggestion(
                "Optimize JOIN order",
                "Reorder JOIN clauses for better performance"
            ));
        }
        
        return suggestions;
    }
}
```

#### 5.3.2 查询性能监控

**查询性能监控器**：
```java
// 查询性能监控器
@Component
public class QueryPerformanceMonitor {
    
    private final MeterRegistry meterRegistry;
    private final Counter slowQueryCounter;
    private final Timer queryTimer;
    private final Gauge activeConnections;
    
    public QueryPerformanceMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.slowQueryCounter = Counter.builder("mysql.slow.queries")
                .description("Number of slow queries")
                .register(meterRegistry);
        
        this.queryTimer = Timer.builder("mysql.query.duration")
                .description("Query execution time")
                .register(meterRegistry);
        
        this.activeConnections = Gauge.builder("mysql.connections.active")
                .description("Active database connections")
                .register(meterRegistry, this, QueryPerformanceMonitor::getActiveConnections);
    }
    
    // 监控查询执行
    @EventListener
    public void onQueryExecution(QueryExecutionEvent event) {
        // 记录查询时间
        queryTimer.record(event.getDuration(), TimeUnit.MILLISECONDS);
        
        // 检查是否为慢查询
        if (event.getDuration() > 1000) { // 1秒
            slowQueryCounter.increment();
            
            // 记录慢查询详情
            logger.warn("Slow query detected: {} ({}ms)", 
                event.getSql(), event.getDuration());
        }
        
        // 检查连接数
        if (getActiveConnections() > 100) {
            logger.warn("High connection count: {}", getActiveConnections());
        }
    }
    
    // 获取活跃连接数
    private double getActiveConnections() {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.processlist WHERE command != 'Sleep'");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to get active connections", e);
        }
        return 0;
    }
}
```

### 5.4 慢查询定位

**开启慢查询日志**：
```sql
-- 开启慢查询日志
SET GLOBAL slow_query_log = 1;
SET GLOBAL long_query_time = 2;  -- 2秒
SET GLOBAL slow_query_log_file = '/var/log/mysql/slow.log';
SET GLOBAL log_queries_not_using_indexes = 1;

-- 查看慢查询配置
SHOW VARIABLES LIKE 'slow_query%';
SHOW VARIABLES LIKE 'long_query_time';
```

**分析慢查询**：
```bash
# 使用mysqldumpslow分析
mysqldumpslow -s t -t 10 /var/log/mysql/slow.log

# 使用pt-query-digest分析
pt-query-digest /var/log/mysql/slow.log

# 实时监控慢查询
tail -f /var/log/mysql/slow.log | grep -E "Query_time|SELECT|UPDATE|DELETE|INSERT"
```

### 5.2 优化技巧

**1. 避免SELECT \***：
```sql
-- 慢
SELECT * FROM user WHERE id = 1;

-- 快
SELECT id, name, age FROM user WHERE id = 1;
```

**2. 使用LIMIT**：
```sql
-- 限制返回行数
SELECT * FROM user LIMIT 100;

-- 深分页优化
-- 慢
SELECT * FROM user LIMIT 1000000, 10;

-- 快（使用子查询）
SELECT * FROM user WHERE id >= (
    SELECT id FROM user LIMIT 1000000, 1
) LIMIT 10;

-- 快（使用id范围）
SELECT * FROM user WHERE id > 1000000 LIMIT 10;
```

**3. 批量操作**：
```sql
-- 慢（多次插入）
INSERT INTO user VALUES (1, 'John');
INSERT INTO user VALUES (2, 'Jane');

-- 快（批量插入）
INSERT INTO user VALUES 
    (1, 'John'),
    (2, 'Jane'),
    (3, 'Bob');
```

**4. JOIN优化**：
```sql
-- 小表驱动大表
SELECT * FROM small_table s
JOIN large_table l ON s.id = l.small_id;

-- 确保JOIN字段有索引
CREATE INDEX idx_small_id ON large_table(small_id);
```

## 六、分库分表

### 6.1 垂直拆分

**垂直分库**：
```
原库:
┌──────────┐
│  user    │
│  order   │
│  product │
└──────────┘

拆分后:
┌──────┐  ┌──────┐  ┌─────────┐
│ user │  │order │  │ product │
└──────┘  └──────┘  └─────────┘
```

**垂直分表**：
```sql
-- 原表
CREATE TABLE user (
    id INT,
    name VARCHAR(50),
    age INT,
    address TEXT,
    description TEXT
);

-- 拆分后
CREATE TABLE user_base (
    id INT,
    name VARCHAR(50),
    age INT
);

CREATE TABLE user_detail (
    user_id INT,
    address TEXT,
    description TEXT
);
```

### 6.2 水平拆分

**分库分表策略**：
```
user_0: user_id % 4 = 0
user_1: user_id % 4 = 1
user_2: user_id % 4 = 2
user_3: user_id % 4 = 3
```

**ShardingSphere配置**：
```yaml
sharding:
  tables:
    user:
      actual-data-nodes: ds$->{0..3}.user_$->{0..3}
      table-strategy:
        inline:
          sharding-column: id
          algorithm-expression: user_$->{id % 4}
      database-strategy:
        inline:
          sharding-column: id
          algorithm-expression: ds$->{id % 4}
```

### 6.3 分布式ID

```java
// Snowflake ID生成器
@Component
public class IdGenerator {
    private SnowflakeIdWorker idWorker = new SnowflakeIdWorker(0, 0);
    
    public long nextId() {
        return idWorker.nextId();
    }
}
```

## 七、主从复制

### 7.1 主从架构

```
┌────────┐  binlog  ┌────────┐
│ Master │─────────▶│ Slave1 │
└────────┘          └────────┘
    │                    
    │ binlog      ┌────────┐
    └────────────▶│ Slave2 │
                  └────────┘
```

### 7.2 主从配置

**Master配置**：
```ini
[mysqld]
server-id = 1
log-bin = mysql-bin
binlog-format = ROW
```

**Slave配置**：
```ini
[mysqld]
server-id = 2
relay-log = mysql-relay-bin
```

**建立主从关系**：
```sql
-- Slave执行
CHANGE MASTER TO
    MASTER_HOST='192.168.1.10',
    MASTER_USER='repl',
    MASTER_PASSWORD='password',
    MASTER_LOG_FILE='mysql-bin.000001',
    MASTER_LOG_POS=154;

START SLAVE;
SHOW SLAVE STATUS\G
```

### 7.3 读写分离

```java
@Configuration
public class DataSourceConfig {
    
    @Bean
    public DataSource dataSource() {
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("master", masterDataSource());
        targetDataSources.put("slave", slaveDataSource());
        
        RoutingDataSource dataSource = new RoutingDataSource();
        dataSource.setTargetDataSources(targetDataSources);
        dataSource.setDefaultTargetDataSource(masterDataSource());
        
        return dataSource;
    }
}

@Component
public class RoutingDataSource extends AbstractRoutingDataSource {
    
    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getDataSource();
    }
}

// 使用
@Transactional(readOnly = true)
public User getUser(Long id) {
    DataSourceContextHolder.setDataSource("slave");
    return userMapper.selectById(id);
}
```

## 八、高频面试题

### Q1：MySQL索引为什么使用B+树？

**B+树优势**：
1. 多路平衡树，高度低，减少IO
2. 叶子节点存储数据，非叶子节点只存索引
3. 叶子节点之间有指针，支持范围查询
4. 更好的空间利用率

### Q2：聚簇索引和非聚簇索引的区别？

**聚簇索引**：
- 叶子节点存储完整数据行
- 主键索引是聚簇索引
- 一张表只能有一个

**非聚簇索引**：
- 叶子节点存储主键值
- 查询需要回表
- 一张表可以有多个

### Q3：MVCC如何实现？

**核心组件**：
1. **隐藏字段**：trx_id（事务ID）、roll_pointer（回滚指针）
2. **Undo Log**：保存历史版本
3. **Read View**：判断版本可见性

**可见性判断**：
- trx_id < min_trx_id：可见
- trx_id > max_trx_id：不可见
- trx_id在中间：判断是否在活跃事务列表

### Q4：MySQL如何解决幻读？

**REPEATABLE READ级别**：
- 使用MVCC解决快照读的幻读
- 使用Next-Key Lock解决当前读的幻读

### Q5：分库分表后如何查询？

**解决方案**：
1. **单分片查询**：直接路由到对应分片
2. **多分片查询**：并行查询后聚合
3. **全局表**：每个分片都保存（字典表）
4. **ER分片**：关联表放同一分片

## 九、数据库架构设计

### 9.1 读写分离架构

**架构设计**：
```
应用层 -> 读写分离中间件 -> 主库(写) + 从库(读)
```

**中间件实现**：
```java
@Component
public class DatabaseRouter {
    
    @Autowired
    private DataSource masterDataSource;
    
    @Autowired
    private List<DataSource> slaveDataSources;
    
    private AtomicInteger slaveIndex = new AtomicInteger(0);
    
    public DataSource getDataSource(boolean readOnly) {
        if (readOnly && !slaveDataSources.isEmpty()) {
            // 轮询选择从库
            int index = slaveIndex.getAndIncrement() % slaveDataSources.size();
            return slaveDataSources.get(index);
        }
        return masterDataSource;
    }
}
```

### 9.2 分库分表架构

**水平分表策略**：
```java
@Component
public class ShardingStrategy {
    
    private static final int SHARD_COUNT = 4;
    
    public String getShardTable(String tableName, Long userId) {
        int shardIndex = (int) (userId % SHARD_COUNT);
        return tableName + "_" + shardIndex;
    }
    
    public String getShardDatabase(Long userId) {
        int shardIndex = (int) (userId % SHARD_COUNT);
        return "user_db_" + shardIndex;
    }
}
```

### 9.3 分布式事务

**XA事务实现**：
```java
@Service
@Transactional
public class DistributedTransactionService {
    
    @Autowired
    private JdbcTemplate userJdbcTemplate;
    
    @Autowired
    private JdbcTemplate orderJdbcTemplate;
    
    public void createUserAndOrder(User user, Order order) {
        // 使用XA事务管理器
        XADataSource userXADataSource = getUserXADataSource();
        XADataSource orderXADataSource = getOrderXADataSource();
        
        XAConnection userXAConnection = userXADataSource.getXAConnection();
        XAConnection orderXAConnection = orderXADataSource.getXAConnection();
        
        XAResource userXAResource = userXAConnection.getXAResource();
        XAResource orderXAResource = orderXAConnection.getXAResource();
        
        Xid userXid = new XidImpl(1, "user".getBytes(), "user".getBytes());
        Xid orderXid = new XidImpl(2, "order".getBytes(), "order".getBytes());
        
        try {
            // 开始XA事务
            userXAResource.start(userXid, XAResource.TMNOFLAGS);
            orderXAResource.start(orderXid, XAResource.TMNOFLAGS);
            
            // 执行业务操作
            userJdbcTemplate.update("INSERT INTO users (name, email) VALUES (?, ?)", 
                                  user.getName(), user.getEmail());
            orderJdbcTemplate.update("INSERT INTO orders (user_id, amount) VALUES (?, ?)", 
                                   user.getId(), order.getAmount());
            
            // 准备提交
            userXAResource.end(userXid, XAResource.TMSUCCESS);
            orderXAResource.end(orderXid, XAResource.TMSUCCESS);
            
            userXAResource.prepare(userXid);
            orderXAResource.prepare(orderXid);
            
            // 提交事务
            userXAResource.commit(userXid, false);
            orderXAResource.commit(orderXid, false);
            
        } catch (Exception e) {
            // 回滚事务
            try {
                userXAResource.rollback(userXid);
                orderXAResource.rollback(orderXid);
            } catch (XAException ex) {
                log.error("Rollback failed", ex);
            }
            throw new RuntimeException("Distributed transaction failed", e);
        }
    }
}
```

## 十、数据库性能调优

### 10.1 参数调优

**InnoDB参数优化**：
```ini
# my.cnf配置
[mysqld]
# 缓冲池大小（内存的70-80%）
innodb_buffer_pool_size = 4G

# 日志文件大小
innodb_log_file_size = 256M
innodb_log_files_in_group = 2

# 刷新策略
innodb_flush_log_at_trx_commit = 2
innodb_flush_method = O_DIRECT

# 并发控制
innodb_thread_concurrency = 16
innodb_read_io_threads = 8
innodb_write_io_threads = 8

# 锁等待超时
innodb_lock_wait_timeout = 50

# 死锁检测
innodb_deadlock_detect = ON
```

### 10.2 索引优化策略

**复合索引设计**：
```sql
-- 用户查询场景
CREATE INDEX idx_user_status_time ON users(status, created_time, user_id);

-- 订单查询场景
CREATE INDEX idx_order_user_status ON orders(user_id, status, created_time);

-- 商品查询场景
CREATE INDEX idx_product_category_price ON products(category_id, price, status);
```

**覆盖索引优化**：
```sql
-- 创建覆盖索引
CREATE INDEX idx_user_cover ON users(user_id, name, email, phone);

-- 查询可以直接从索引获取数据，无需回表
SELECT user_id, name, email FROM users WHERE user_id = 123;
```

### 10.3 分区表设计

**范围分区**：
```sql
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT,
    user_id BIGINT,
    amount DECIMAL(10,2),
    created_time DATETIME,
    PRIMARY KEY (id, created_time)
) PARTITION BY RANGE (YEAR(created_time)) (
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

## 十一、数据库监控与运维

### 11.1 性能监控

**关键指标监控**：
```sql
-- 连接数监控
SHOW STATUS LIKE 'Threads_connected';
SHOW STATUS LIKE 'Max_used_connections';

-- 缓存命中率
SHOW STATUS LIKE 'Innodb_buffer_pool_read_requests';
SHOW STATUS LIKE 'Innodb_buffer_pool_reads';

-- 查询缓存命中率
SHOW STATUS LIKE 'Qcache_hits';
SHOW STATUS LIKE 'Qcache_inserts';

-- 慢查询统计
SHOW STATUS LIKE 'Slow_queries';
```

### 11.2 备份与恢复

**全量备份**：
```bash
# mysqldump全量备份
mysqldump -h localhost -u root -p --single-transaction --routines --triggers your_database > backup.sql

# 压缩备份
mysqldump -h localhost -u root -p --single-transaction --routines --triggers your_database | gzip > backup.sql.gz
```

**增量备份**：
```bash
# 开启binlog
# my.cnf配置
[mysqld]
log-bin=mysql-bin
binlog-format=ROW
expire_logs_days=7

# 增量备份脚本
#!/bin/bash
BACKUP_DIR="/backup/mysql"
DATE=$(date +%Y%m%d_%H%M%S)

# 全量备份
mysqldump --single-transaction --master-data=2 your_database > ${BACKUP_DIR}/full_${DATE}.sql

# 增量备份（binlog）
mysqlbinlog --start-datetime="2024-01-01 00:00:00" mysql-bin.000001 > ${BACKUP_DIR}/incremental_${DATE}.sql
```

## 十二、架构师级面试题

### Q1：如何设计一个高并发的数据库架构？

**核心原理**：
高并发数据库架构设计需要从多个维度考虑：读写分离、分库分表、缓存策略、连接池管理等。核心思想是通过水平扩展和垂直优化，将单点压力分散到多个节点，同时保证数据一致性和系统可用性。

数学上，系统吞吐量T = Σ(T_i × N_i)，其中T_i是单节点吞吐量，N_i是节点数量。架构设计目标是最大化T值，同时保证数据一致性C = Σ(C_i)。

**架构设计原则**：
1. **读写分离**：主库处理写操作，从库处理读操作
2. **分库分表**：水平拆分和垂直拆分相结合
3. **缓存策略**：多级缓存，减少数据库压力
4. **连接池管理**：合理配置连接池参数
5. **监控告警**：实时监控系统状态

**架构设计图**：
```mermaid
graph TB
    subgraph "应用层"
        A[Web应用]
        B[API网关]
        C[负载均衡]
    end
    
    subgraph "缓存层"
        D[Redis集群]
        E[本地缓存]
    end
    
    subgraph "数据库层"
        F[主库Master]
        G[从库Slave1]
        H[从库Slave2]
        I[从库Slave3]
    end
    
    subgraph "分片层"
        J[分片1]
        K[分片2]
        L[分片3]
    end
    
    A --> C
    C --> D
    C --> E
    D --> F
    D --> G
    E --> H
    E --> I
    F --> J
    G --> K
    H --> L
```

**核心组件实现**：
```java
// 读写分离实现
@Configuration
public class DatabaseConfig {
    
    @Bean
    @Primary
    public DataSource masterDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://master:3306/db");
        config.setUsername("root");
        config.setPassword("password");
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        return new HikariDataSource(config);
    }
    
    @Bean
    public DataSource slaveDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://slave:3306/db");
        config.setUsername("root");
        config.setPassword("password");
        config.setMaximumPoolSize(50);
        config.setMinimumIdle(10);
        return new HikariDataSource(config);
    }
    
    @Bean
    public DataSource routingDataSource() {
        DynamicRoutingDataSource routingDataSource = new DynamicRoutingDataSource();
        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put("master", masterDataSource());
        dataSourceMap.put("slave", slaveDataSource());
        routingDataSource.setTargetDataSources(dataSourceMap);
        routingDataSource.setDefaultTargetDataSource(masterDataSource());
        return routingDataSource;
    }
}

// 分库分表实现
@Component
public class ShardingStrategy {
    
    private final int shardCount = 4;
    
    public String getShardKey(Long userId) {
        return "shard_" + (userId % shardCount);
    }
    
    public String getTableName(Long userId) {
        return "user_" + (userId % 10);
    }
    
    public List<String> getAllShards() {
        List<String> shards = new ArrayList<>();
        for (int i = 0; i < shardCount; i++) {
            shards.add("shard_" + i);
        }
        return shards;
    }
}
```

**应用场景**：
1. **电商平台**：商品、订单、用户等核心业务
2. **社交平台**：用户关系、内容、消息等
3. **金融系统**：账户、交易、风控等
4. **IoT平台**：设备数据、告警、统计等

**实战经验**：
在电商平台数据库架构设计中，我们实现了以下方案：
- 读写分离：1主3从，读请求分散到从库
- 分库分表：用户表按ID分4个库，每个库10张表
- 缓存策略：Redis缓存热点数据，命中率95%
- 连接池：主库20个连接，从库50个连接

性能测试显示，系统QPS从1000提升到10000，平均响应时间从200ms降至50ms，数据库CPU使用率从90%降至30%。

**权衡取舍**：
- **一致性 vs 性能**：强一致性影响性能，需要根据业务特点权衡
- **复杂度 vs 可维护性**：复杂架构提供更好性能但增加维护成本
- **成本 vs 收益**：分布式架构成本高，需要评估ROI

**常见追问**：
1. 追问：如何保证主从数据一致性？→ 回答：使用半同步复制，主库等待至少一个从库确认后再提交事务。
2. 追问：分库分表后如何解决跨库查询？→ 回答：使用分布式查询引擎或数据冗余，避免跨库JOIN操作。

**踩坑经验**：
- 坑1：从库延迟导致数据不一致。解决：监控从库延迟，延迟过高时切换到主库读取。
- 坑2：分片键选择不当导致数据倾斜。解决：选择分布均匀的分片键，如用户ID的哈希值。

### Q2：如何解决数据库死锁问题？

**核心原理**：
数据库死锁是指两个或多个事务相互等待对方释放锁资源，导致所有事务都无法继续执行的状态。死锁解决需要从预防、检测、处理三个维度考虑。

数学上，死锁概率P = Σ(P_i × T_i)，其中P_i是锁冲突概率，T_i是事务执行时间。死锁解决目标是降低P值到可接受范围。

**死锁产生条件**：
1. **互斥条件**：资源不能被多个事务同时使用
2. **请求和保持条件**：事务持有资源的同时请求新资源
3. **不可剥夺条件**：资源不能被强制释放
4. **循环等待条件**：事务间形成循环等待链

**死锁预防策略**：
```java
// 死锁预防实现
@Service
public class DeadlockPreventionService {
    
    // 统一加锁顺序
    public void transferMoney(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        // 按ID大小顺序加锁，避免循环等待
        Long firstLock = Math.min(fromAccountId, toAccountId);
        Long secondLock = Math.max(fromAccountId, toAccountId);
        
        synchronized (getLockObject(firstLock)) {
            synchronized (getLockObject(secondLock)) {
                // 执行转账逻辑
                performTransfer(fromAccountId, toAccountId, amount);
            }
        }
    }
    
    // 减少锁持有时间
    public void batchUpdate(List<Long> ids) {
        // 先查询所有数据
        List<Entity> entities = entityRepository.findByIds(ids);
        
        // 快速更新，减少锁持有时间
        for (Entity entity : entities) {
            synchronized (getLockObject(entity.getId())) {
                entity.setStatus("UPDATED");
                entityRepository.save(entity);
            }
        }
    }
    
    // 使用乐观锁
    @Transactional
    public boolean updateWithOptimisticLock(Long id, String newValue) {
        Entity entity = entityRepository.findById(id);
        if (entity == null) return false;
        
        int oldVersion = entity.getVersion();
        entity.setValue(newValue);
        
        // 使用版本号进行乐观锁控制
        int updatedRows = entityRepository.updateWithVersion(id, newValue, oldVersion);
        return updatedRows > 0;
    }
}
```

**死锁检测和处理**：
```sql
-- 死锁检测查询
SELECT 
    r.trx_id waiting_trx_id,
    r.trx_mysql_thread_id waiting_thread,
    r.trx_query waiting_query,
    b.trx_id blocking_trx_id,
    b.trx_mysql_thread_id blocking_thread,
    b.trx_query blocking_query
FROM information_schema.innodb_lock_waits w
INNER JOIN information_schema.innodb_trx b ON b.trx_id = w.blocking_trx_id
INNER JOIN information_schema.innodb_trx r ON r.trx_id = w.requesting_trx_id;

-- 死锁处理：杀死阻塞事务
KILL CONNECTION blocking_thread_id;
```

**应用场景**：
1. **转账业务**：账户间转账操作
2. **库存扣减**：商品库存更新
3. **订单处理**：订单状态变更
4. **用户操作**：用户信息更新

**实战经验**：
在电商平台订单处理中，我们遇到了典型的死锁问题：
- 问题：订单状态更新和库存扣减操作产生死锁
- 原因：不同事务以不同顺序获取锁资源
- 解决：统一按订单ID顺序加锁，使用乐观锁处理库存

通过死锁预防措施，死锁发生率从0.1%降至0.001%，系统稳定性显著提升。

**权衡取舍**：
- **性能 vs 安全性**：死锁预防可能影响并发性能
- **复杂度 vs 可靠性**：复杂预防机制提供更好可靠性但增加复杂度
- **检测频率 vs 资源消耗**：频繁检测消耗更多资源

**常见追问**：
1. 追问：如何设置合适的死锁检测频率？→ 回答：根据业务特点设置，高并发场景下可以设置较短的检测间隔。
2. 追问：死锁和锁等待有什么区别？→ 回答：锁等待是正常的，死锁是异常状态；锁等待会超时，死锁需要检测处理。

**踩坑经验**：
- 坑1：死锁检测频率过高导致性能问题。解决：根据业务特点调整检测频率，平衡检测效果和性能影响。
- 坑2：死锁处理不当导致数据不一致。解决：建立完善的死锁处理机制，确保数据一致性。

### Q3：如何优化大表查询性能？

**核心原理**：
大表查询性能优化需要从索引设计、查询重写、分页优化、缓存策略等多个维度考虑。核心思想是通过减少数据扫描量、优化查询路径、利用缓存机制提升查询效率。

数学上，查询效率E = T / S，其中T是查询时间，S是扫描数据量。优化目标是最大化E值，即最小化T和S。

**优化策略详解**：
```java
// 索引优化实现
@Entity
@Table(name = "user_orders")
public class UserOrder {
    @Id
    private Long id;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "order_time")
    private LocalDateTime orderTime;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "amount")
    private BigDecimal amount;
    
    // 复合索引：user_id + order_time + status
    @Index(name = "idx_user_time_status", columnList = "user_id, order_time, status")
    // 覆盖索引：包含查询所需的所有字段
    @Index(name = "idx_cover_query", columnList = "user_id, order_time, status, amount")
}

// 分页优化实现
@Repository
public class OptimizedOrderRepository {
    
    // 传统分页（性能差）
    @Query("SELECT o FROM UserOrder o WHERE o.userId = :userId ORDER BY o.orderTime DESC")
    Page<UserOrder> findByUserIdTraditional(@Param("userId") Long userId, Pageable pageable);
    
    // ID分页（性能好）
    @Query("SELECT o FROM UserOrder o WHERE o.userId = :userId AND o.id < :lastId ORDER BY o.id DESC")
    List<UserOrder> findByUserIdWithIdPaging(@Param("userId") Long userId, @Param("lastId") Long lastId, Pageable pageable);
    
    // 游标分页
    @Query("SELECT o FROM UserOrder o WHERE o.userId = :userId AND o.orderTime < :lastTime ORDER BY o.orderTime DESC")
    List<UserOrder> findByUserIdWithCursorPaging(@Param("userId") Long userId, @Param("lastTime") LocalDateTime lastTime, Pageable pageable);
}

// 查询重写优化
@Service
public class QueryOptimizationService {
    
    // 避免SELECT *
    public List<UserOrderSummary> getOrderSummary(Long userId) {
        // 只查询需要的字段
        return orderRepository.findOrderSummaryByUserId(userId);
    }
    
    // 使用EXISTS替代IN
    public List<User> getUsersWithOrders(List<Long> userIds) {
        return userRepository.findUsersWithOrders(userIds);
    }
    
    // 使用LIMIT限制结果集
    public List<UserOrder> getRecentOrders(Long userId, int limit) {
        return orderRepository.findRecentOrdersByUserId(userId, limit);
    }
}
```

**分区表设计**：
```sql
-- 按时间分区
CREATE TABLE user_orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_time DATETIME NOT NULL,
    status VARCHAR(20),
    amount DECIMAL(10,2)
) PARTITION BY RANGE (YEAR(order_time)) (
    PARTITION p2022 VALUES LESS THAN (2023),
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- 按ID范围分区
CREATE TABLE user_orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_time DATETIME NOT NULL,
    status VARCHAR(20),
    amount DECIMAL(10,2)
) PARTITION BY RANGE (id) (
    PARTITION p1 VALUES LESS THAN (1000000),
    PARTITION p2 VALUES LESS THAN (2000000),
    PARTITION p3 VALUES LESS THAN (3000000),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

**应用场景**：
1. **用户订单查询**：按用户ID查询订单历史
2. **商品搜索**：按关键词搜索商品
3. **日志分析**：按时间范围查询日志
4. **数据统计**：按维度统计业务数据

**实战经验**：
在电商平台订单查询优化中，我们实施了以下优化措施：
- 索引优化：创建复合索引(user_id, order_time, status)
- 分页优化：使用ID分页替代OFFSET分页
- 查询重写：避免SELECT *，只查询必要字段
- 分区表：按时间分区，提高查询效率

优化效果：
- 查询时间从2秒降至200ms
- 数据库CPU使用率从80%降至20%
- 支持千万级数据的高效查询

**权衡取舍**：
- **存储空间 vs 查询性能**：更多索引占用更多存储空间
- **写入性能 vs 查询性能**：索引影响写入性能
- **复杂度 vs 效果**：复杂优化提供更好效果但增加维护成本

**常见追问**：
1. 追问：如何选择合适的索引类型？→ 回答：根据查询模式选择，等值查询用B+树索引，范围查询用复合索引，全文搜索用全文索引。
2. 追问：分区表有什么限制？→ 回答：分区键必须是主键的一部分，跨分区查询性能较差，需要合理设计分区策略。

**踩坑经验**：
- 坑1：索引过多导致写入性能下降。解决：根据实际查询需求创建索引，定期清理无用索引。
- 坑2：分区表设计不当导致查询性能下降。解决：根据查询模式设计分区策略，避免跨分区查询。

### Q4：如何保证数据库高可用？

**核心原理**：
数据库高可用设计需要从故障预防、故障检测、故障恢复三个维度考虑。核心思想是通过冗余、监控、自动切换等机制，确保系统在故障情况下仍能提供服务。

数学上，可用性A = MTBF / (MTBF + MTTR)，其中MTBF是平均故障间隔时间，MTTR是平均故障恢复时间。高可用设计目标是最大化A值。

**高可用架构设计**：
```mermaid
graph TB
    subgraph "应用层"
        A[负载均衡器]
        B[应用服务器1]
        C[应用服务器2]
        D[应用服务器3]
    end
    
    subgraph "数据库层"
        E[主库Master]
        F[从库Slave1]
        G[从库Slave2]
        H[从库Slave3]
    end
    
    subgraph "监控层"
        I[监控系统]
        J[告警系统]
        K[自动切换]
    end
    
    A --> B
    A --> C
    A --> D
    B --> E
    B --> F
    C --> E
    C --> G
    D --> E
    D --> H
    I --> E
    I --> F
    I --> G
    I --> H
    I --> J
    J --> K
    K --> E
    K --> F
```

**主从复制配置**：
```sql
-- 主库配置
[mysqld]
server-id = 1
log-bin = mysql-bin
binlog-format = ROW
sync_binlog = 1
innodb_flush_log_at_trx_commit = 1

-- 从库配置
[mysqld]
server-id = 2
relay-log = mysql-relay-bin
read-only = 1
slave-skip-errors = 1062,1032

-- 半同步复制配置
-- 主库
INSTALL PLUGIN rpl_semi_sync_master SONAME 'semisync_master.so';
SET GLOBAL rpl_semi_sync_master_enabled = 1;
SET GLOBAL rpl_semi_sync_master_timeout = 1000;

-- 从库
INSTALL PLUGIN rpl_semi_sync_slave SONAME 'semisync_slave.so';
SET GLOBAL rpl_semi_sync_slave_enabled = 1;
```

**自动故障切换实现**：
```java
// 故障检测和切换
@Component
public class FailoverManager {
    
    @Autowired
    private DataSource masterDataSource;
    
    @Autowired
    private DataSource slaveDataSource;
    
    @Autowired
    private HealthCheckService healthCheckService;
    
    @Scheduled(fixedDelay = 5000)
    public void checkDatabaseHealth() {
        try {
            // 检查主库健康状态
            if (!healthCheckService.isHealthy(masterDataSource)) {
                log.warn("Master database is unhealthy, triggering failover");
                triggerFailover();
            }
            
            // 检查从库延迟
            long slaveDelay = healthCheckService.getSlaveDelay(slaveDataSource);
            if (slaveDelay > 10000) { // 延迟超过10秒
                log.warn("Slave delay is too high: {}ms", slaveDelay);
                handleSlaveDelay(slaveDelay);
            }
            
        } catch (Exception e) {
            log.error("Health check failed", e);
        }
    }
    
    private void triggerFailover() {
        try {
            // 1. 停止应用写入
            stopApplicationWrites();
            
            // 2. 等待从库同步完成
            waitForSlaveSync();
            
            // 3. 提升从库为主库
            promoteSlaveToMaster();
            
            // 4. 更新应用配置
            updateApplicationConfig();
            
            // 5. 恢复应用写入
            resumeApplicationWrites();
            
            log.info("Failover completed successfully");
            
        } catch (Exception e) {
            log.error("Failover failed", e);
            // 回滚操作
            rollbackFailover();
        }
    }
}
```

**应用场景**：
1. **电商平台**：订单、支付等核心业务
2. **金融系统**：账户、交易等关键业务
3. **社交平台**：用户、内容等基础服务
4. **IoT平台**：设备数据、告警等

**实战经验**：
在金融系统数据库高可用设计中，我们实现了以下方案：
- 主从复制：1主3从，半同步复制
- 自动切换：故障检测时间5秒，切换时间30秒
- 数据备份：每日全量备份，实时增量备份
- 监控告警：7x24小时监控，自动告警

系统可用性达到99.99%，年故障时间不超过52分钟，故障恢复时间平均30秒。

**权衡取舍**：
- **一致性 vs 可用性**：强一致性影响可用性，需要根据业务特点权衡
- **成本 vs 可靠性**：高可用方案成本高，需要评估ROI
- **复杂度 vs 可维护性**：复杂方案提供更好可靠性但增加维护成本

**常见追问**：
1. 追问：如何选择合适的高可用方案？→ 回答：根据业务特点选择，金融系统需要强一致性，互联网应用可以接受最终一致性。
2. 追问：如何评估高可用方案的效果？→ 回答：通过可用性指标、故障恢复时间、数据丢失率等关键指标评估。

**踩坑经验**：
- 坑1：自动切换机制设计不当导致数据丢失。解决：完善切换流程，确保数据一致性。
- 坑2：监控告警过于频繁导致告警疲劳。解决：设置合理的告警阈值和抑制规则。

### Q5：如何设计数据库分片策略？

**核心原理**：
数据库分片是将大表数据分散到多个数据库实例中，通过水平扩展提升系统性能。分片设计需要从分片键选择、数据分布、查询路由、扩容策略等维度考虑。

数学上，分片效率E = Q / (N × C)，其中Q是查询量，N是分片数量，C是跨分片查询成本。分片设计目标是最大化E值。

**分片策略对比**：
| 分片策略 | 优点 | 缺点 | 适用场景 |
|----------|------|------|----------|
| 取模分片 | 实现简单，分布均匀 | 扩容困难 | 数据分布均匀的场景 |
| 范围分片 | 查询效率高，扩容简单 | 可能数据倾斜 | 有序数据查询 |
| 一致性哈希 | 扩容简单，数据迁移少 | 实现复杂 | 需要频繁扩容的场景 |
| 目录分片 | 灵活性强，支持复杂规则 | 单点故障 | 复杂业务规则 |

**分片实现方案**：
```java
// 取模分片实现
@Component
public class ModuloShardingStrategy {
    
    private final int shardCount = 4;
    private final List<DataSource> dataSources;
    
    public ModuloShardingStrategy(List<DataSource> dataSources) {
        this.dataSources = dataSources;
    }
    
    public DataSource getShardDataSource(Long shardKey) {
        int shardIndex = (int) (shardKey % shardCount);
        return dataSources.get(shardIndex);
    }
    
    public String getShardTableName(String baseTableName, Long shardKey) {
        int shardIndex = (int) (shardKey % shardCount);
        return baseTableName + "_" + shardIndex;
    }
}

// 一致性哈希分片实现
@Component
public class ConsistentHashShardingStrategy {
    
    private final ConsistentHash<DataSource> consistentHash;
    
    public ConsistentHashShardingStrategy(List<DataSource> dataSources) {
        this.consistentHash = new ConsistentHash<>(dataSources);
    }
    
    public DataSource getShardDataSource(String shardKey) {
        return consistentHash.get(shardKey);
    }
    
    public void addShard(DataSource dataSource) {
        consistentHash.add(dataSource);
    }
    
    public void removeShard(DataSource dataSource) {
        consistentHash.remove(dataSource);
    }
}

// 分片路由实现
@Service
public class ShardingRouter {
    
    @Autowired
    private ModuloShardingStrategy shardingStrategy;
    
    public <T> T executeOnShard(Long shardKey, ShardOperation<T> operation) {
        DataSource dataSource = shardingStrategy.getShardDataSource(shardKey);
        String tableName = shardingStrategy.getShardTableName("user_orders", shardKey);
        
        return operation.execute(dataSource, tableName);
    }
    
    public <T> List<T> executeOnAllShards(ShardOperation<T> operation) {
        List<T> results = new ArrayList<>();
        
        for (int i = 0; i < 4; i++) {
            DataSource dataSource = shardingStrategy.getShardDataSource((long) i);
            String tableName = "user_orders_" + i;
            
            T result = operation.execute(dataSource, tableName);
            if (result != null) {
                results.add(result);
            }
        }
        
        return results;
    }
}
```

**分片键选择原则**：
1. **数据分布均匀**：避免热点数据
2. **查询路由简单**：减少跨分片查询
3. **业务相关性**：相关数据在同一分片
4. **扩展性考虑**：支持动态扩容

**应用场景**：
1. **用户数据分片**：按用户ID分片
2. **订单数据分片**：按订单ID或用户ID分片
3. **商品数据分片**：按商品ID或分类分片
4. **日志数据分片**：按时间或设备ID分片

**实战经验**：
在电商平台用户数据分片中，我们选择了用户ID作为分片键：
- 分片策略：用户ID取模分片，4个分片
- 数据分布：每个分片约2500万用户
- 查询路由：用户相关查询都在同一分片
- 扩容策略：支持动态添加分片

性能测试显示，分片后查询性能提升4倍，写入性能提升3倍，系统支持亿级用户数据。

**权衡取舍**：
- **查询效率 vs 数据分布**：分片提高查询效率但可能影响数据分布
- **复杂度 vs 性能**：分片增加复杂度但提供更好性能
- **一致性 vs 可用性**：分片可能影响数据一致性

**常见追问**：
1. 追问：如何处理跨分片查询？→ 回答：使用分布式查询引擎或数据冗余，避免跨分片JOIN操作。
2. 追问：分片扩容时如何处理数据迁移？→ 回答：使用一致性哈希或双写策略，逐步迁移数据。

**踩坑经验**：
- 坑1：分片键选择不当导致数据倾斜。解决：选择分布均匀的分片键，如用户ID的哈希值。
- 坑2：跨分片事务处理不当导致数据不一致。解决：使用分布式事务或最终一致性方案。

---

**关键字**：索引、锁、事务、MVCC、执行计划、分库分表、主从复制、性能调优、高可用、分布式事务、数据库架构、读写分离、缓存策略、连接池、死锁预防、查询优化、分区表、分页优化、故障切换、监控告警、数据备份、分片策略、一致性哈希、扩容策略、数据迁移、跨分片查询、分布式事务、最终一致性、架构师级、深度解析、技术指南

