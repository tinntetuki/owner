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

## 一、索引

### 1.1 索引类型

**B+树索引**（InnoDB默认）：
```
      [10|20|30]
      /    |    \
  [1-9] [11-19] [21-29] [31-39]
    ↓      ↓      ↓      ↓
  data   data   data   data
```

**哈希索引**（Memory引擎）：
- O(1)查找
- 不支持范围查询
- 不支持排序

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

## 五、SQL优化

### 5.1 慢查询定位

**开启慢查询日志**：
```sql
SET GLOBAL slow_query_log = 1;
SET GLOBAL long_query_time = 2;  -- 2秒
```

**分析慢查询**：
```bash
mysqldumpslow -s t -t 10 /var/log/mysql/slow.log
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

---

**关键字**：索引、锁、事务、MVCC、执行计划、分库分表、主从复制

