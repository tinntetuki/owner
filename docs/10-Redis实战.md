# Redis实战深度解析

## 目录
- [一、数据结构](#一数据结构)
- [二、持久化](#二持久化)
- [三、集群方案](#三集群方案)
- [四、缓存策略](#四缓存策略)
- [五、实战场景](#五实战场景)
- [六、性能优化](#六性能优化)
- [七、高频面试题](#七高频面试题)

## 一、Redis源码深度解析

### 1.1 Redis整体架构

#### 1.1.1 Redis服务器架构

**Redis服务器架构图**：
```
┌─────────────────────────────────────────────────────────┐
│                    Redis服务器                          │
├─────────────────────────────────────────────────────────┤
│  Event Loop (事件循环)    │  Command Processor (命令处理器) │
│  ┌─────────────────────┐  │  ┌─────────────────────┐    │
│  │   File Events       │  │  │   Command Parser    │    │
│  │   Time Events       │  │  │   Command Executor  │    │
│  │   I/O Multiplexing  │  │  │   Response Builder  │    │
│  └─────────────────────┘  │  └─────────────────────┘    │
├─────────────────────────────────────────────────────────┤
│  Memory Management (内存管理) │  Persistence (持久化)      │
│  ┌─────────────────────┐  │  ┌─────────────────────┐    │
│  │   Object System     │  │  │   RDB Persistence  │    │
│  │   Memory Allocator  │  │  │   AOF Persistence   │    │
│  │   LRU Eviction      │  │  │   Background Tasks  │    │
│  └─────────────────────┘  │  └─────────────────────┘    │
├─────────────────────────────────────────────────────────┤
│  Data Structures (数据结构) │  Network Layer (网络层)     │
│  ┌─────────────────────┐  │  ┌─────────────────────┐    │
│  │   String (SDS)      │  │  │   TCP Server        │    │
│  │   List (QuickList) │  │  │   Protocol Parser   │    │
│  │   Hash (Dict)      │  │  │   Client Management  │    │
│  │   Set (IntSet)     │  │  └─────────────────────┘    │
│  │   ZSet (SkipList)  │  │                            │
│  └─────────────────────┘  │                            │
└─────────────────────────────────────────────────────────┘
```

#### 1.1.2 Redis事件循环深度解析

**事件循环核心实现**：
```c
// Redis事件循环核心代码（简化版）
void aeMain(aeEventLoop *eventLoop) {
    eventLoop->stop = 0;
    while (!eventLoop->stop) {
        // 1. 处理文件事件
        aeProcessEvents(eventLoop, AE_ALL_EVENTS);
        
        // 2. 处理时间事件
        if (eventLoop->beforesleep != NULL)
            eventLoop->beforesleep(eventLoop);
    }
}

// 事件处理函数
int aeProcessEvents(aeEventLoop *eventLoop, int flags) {
    int processed = 0, numevents;
    
    // 1. 获取就绪的文件事件
    numevents = aeApiPoll(eventLoop, tvp);
    
    // 2. 处理文件事件
    for (j = 0; j < numevents; j++) {
        aeFileEvent *fe = &eventLoop->events[eventLoop->fired[j].fd];
        int mask = eventLoop->fired[j].mask;
        int fd = eventLoop->fired[j].fd;
        int rfired = 0;
        
        // 处理读事件
        if (fe->mask & mask & AE_READABLE) {
            rfired = 1;
            fe->rfileProc(eventLoop, fd, fe->clientData, mask);
        }
        
        // 处理写事件
        if (fe->mask & mask & AE_WRITABLE) {
            if (!rfired || fe->wfileProc != fe->rfileProc)
                fe->wfileProc(eventLoop, fd, fe->clientData, mask);
        }
        processed++;
    }
    
    // 3. 处理时间事件
    if (flags & AE_TIME_EVENTS)
        processed += processTimeEvents(eventLoop);
    
    return processed;
}
```

#### 1.1.3 Redis内存管理深度解析

**Redis内存分配器**：
```c
// Redis内存分配器实现
void *zmalloc(size_t size) {
    void *ptr = malloc(size + PREFIX_SIZE);
    if (!ptr) zmalloc_oom_handler(size);
    
    // 记录分配的内存大小
    *((size_t*)ptr) = size;
    update_zmalloc_stat_alloc(size + PREFIX_SIZE);
    
    return (char*)ptr + PREFIX_SIZE;
}

void zfree(void *ptr) {
    void *realptr;
    size_t oldsize;
    
    if (ptr == NULL) return;
    
    realptr = (char*)ptr - PREFIX_SIZE;
    oldsize = *((size_t*)realptr);
    update_zmalloc_stat_free(oldsize + PREFIX_SIZE);
    free(realptr);
}

// 内存统计
size_t zmalloc_used_memory(void) {
    size_t um;
    atomicGet(used_memory, um);
    return um;
}
```

### 1.2 数据结构源码深度解析

#### 1.2.1 SDS（Simple Dynamic String）源码实现

**SDS结构定义**：
```c
// SDS结构定义
struct sdshdr {
    unsigned int len;     // 字符串长度
    unsigned int free;    // 剩余空间
    char buf[];          // 字符数组
};

// SDS类型定义
#define SDS_TYPE_5  0
#define SDS_TYPE_8  1
#define SDS_TYPE_16 2
#define SDS_TYPE_32 3
#define SDS_TYPE_64 4

// SDS创建函数
sds sdsnewlen(const void *init, size_t initlen) {
    void *sh;
    sds s;
    char type = sdsReqType(initlen);
    
    // 根据长度选择不同的SDS类型
    if (type == SDS_TYPE_5 && initlen == 0) type = SDS_TYPE_8;
    
    int hdrlen = sdsHdrSize(type);
    unsigned char *fp;
    
    sh = s_malloc(hdrlen + initlen + 1);
    if (sh == NULL) return NULL;
    
    s = (char*)sh + hdrlen;
    fp = ((unsigned char*)s) - 1;
    
    switch(type) {
        case SDS_TYPE_5: {
            *fp = type | (initlen << SDS_TYPE_BITS);
            break;
        }
        case SDS_TYPE_8: {
            SDS_HDR_VAR(8,s);
            sh->len = initlen;
            sh->alloc = initlen;
            *fp = type;
            break;
        }
        // ... 其他类型
    }
    
    if (initlen && init)
        memcpy(s, init, initlen);
    s[initlen] = '\0';
    return s;
}

// SDS扩容函数
sds sdsMakeRoomFor(sds s, size_t addlen) {
    void *sh, *newsh;
    size_t avail = sdsavail(s);
    size_t len, newlen;
    char type, oldtype = s[-1] & SDS_TYPE_MASK;
    int hdrlen;
    
    if (avail >= addlen) return s;
    
    len = sdslen(s);
    sh = (char*)s - sdsHdrSize(oldtype);
    newlen = (len + addlen);
    
    // 扩容策略：小于1MB时翻倍，大于1MB时每次增加1MB
    if (newlen < SDS_MAX_PREALLOC)
        newlen *= 2;
    else
        newlen += SDS_MAX_PREALLOC;
    
    type = sdsReqType(newlen);
    if (type == SDS_TYPE_5) type = SDS_TYPE_8;
    
    hdrlen = sdsHdrSize(type);
    newsh = s_realloc(sh, hdrlen + newlen + 1);
    if (newsh == NULL) return NULL;
    
    s = (char*)newsh + hdrlen;
    s[-1] = type;
    sdssetlen(s, len);
    sdssetalloc(s, newlen);
    return s;
}
```

#### 1.2.2 字典（Dict）源码实现

**字典结构定义**：
```c
// 字典结构
typedef struct dict {
    dictType *type;         // 类型特定函数
    void *privdata;         // 私有数据
    dictht ht[2];          // 哈希表数组
    long rehashidx;        // rehash索引
    unsigned long iterators; // 迭代器数量
} dict;

// 哈希表结构
typedef struct dictht {
    dictEntry **table;      // 哈希表数组
    unsigned long size;     // 哈希表大小
    unsigned long sizemask; // 哈希表大小掩码
    unsigned long used;     // 已使用节点数
} dictht;

// 哈希表节点
typedef struct dictEntry {
    void *key;              // 键
    union {
        void *val;
        uint64_t u64;
        int64_t s64;
        double d;
    } v;                    // 值
    struct dictEntry *next; // 指向下一个节点
} dictEntry;

// 字典查找函数
dictEntry *dictFind(dict *d, const void *key) {
    dictEntry *he;
    unsigned int h, idx, table;
    
    if (d->ht[0].used + d->ht[1].used == 0) return NULL;
    
    h = dictHashKey(d, key);
    for (table = 0; table <= 1; table++) {
        idx = h & d->ht[table].sizemask;
        he = d->ht[table].table[idx];
        while(he) {
            if (key==he->key || dictCompareKeys(d, key, he->key))
                return he;
            he = he->next;
        }
        if (!dictIsRehashing(d)) return NULL;
    }
    return NULL;
}

// 渐进式rehash
int dictRehash(dict *d, int n) {
    int empty_visits = n*10; // 最多访问的空桶数
    if (!dictIsRehashing(d)) return 0;
    
    while(n-- && d->ht[0].used != 0) {
        dictEntry *de, *nextde;
        
        // 找到非空桶
        while(d->ht[0].table[d->rehashidx] == NULL) {
            d->rehashidx++;
            if (--empty_visits == 0) return 1;
        }
        
        de = d->ht[0].table[d->rehashidx];
        // 移动所有键值对
        while(de) {
            unsigned int h;
            nextde = de->next;
            
            h = dictHashKey(d, de->key) & d->ht[1].sizemask;
            de->next = d->ht[1].table[h];
            d->ht[1].table[h] = de;
            d->ht[0].used--;
            d->ht[1].used++;
            de = nextde;
        }
        d->ht[0].table[d->rehashidx] = NULL;
        d->rehashidx++;
    }
    
    // 检查是否完成rehash
    if (d->ht[0].used == 0) {
        zfree(d->ht[0].table);
        d->ht[0] = d->ht[1];
        _dictReset(&d->ht[1]);
        d->rehashidx = -1;
        return 0;
    }
    
    return 1;
}
```

#### 1.2.3 跳跃表（SkipList）源码实现

**跳跃表结构定义**：
```c
// 跳跃表节点
typedef struct zskiplistNode {
    robj *obj;                    // 成员对象
    double score;                 // 分值
    struct zskiplistNode *backward; // 后退指针
    struct zskiplistLevel {
        struct zskiplistNode *forward; // 前进指针
        unsigned int span;              // 跨度
    } level[];                     // 层
} zskiplistNode;

// 跳跃表
typedef struct zskiplist {
    struct zskiplistNode *header, *tail; // 头节点和尾节点
    unsigned long length;                // 节点数量
    int level;                           // 层数
} zskiplist;

// 跳跃表查找
zskiplistNode *zslGetElementByRank(zskiplist *zsl, unsigned long rank) {
    zskiplistNode *x;
    unsigned long traversed = 0;
    int i;
    
    x = zsl->header;
    for (i = zsl->level-1; i >= 0; i--) {
        while (x->level[i].forward && (traversed + x->level[i].span) <= rank) {
            traversed += x->level[i].span;
            x = x->level[i].forward;
        }
        if (traversed == rank) {
            return x;
        }
    }
    return NULL;
}

// 跳跃表插入
zskiplistNode *zslInsert(zskiplist *zsl, double score, robj *obj) {
    zskiplistNode *update[ZSKIPLIST_MAXLEVEL], *x;
    unsigned int rank[ZSKIPLIST_MAXLEVEL];
    int i, level;
    
    x = zsl->header;
    for (i = zsl->level-1; i >= 0; i--) {
        rank[i] = i == (zsl->level-1) ? 0 : rank[i+1];
        while (x->level[i].forward &&
               (x->level[i].forward->score < score ||
                (x->level[i].forward->score == score &&
                 compareStringObjects(x->level[i].forward->obj,obj) < 0))) {
            rank[i] += x->level[i].span;
            x = x->level[i].forward;
        }
        update[i] = x;
    }
    
    // 随机生成层数
    level = zslRandomLevel();
    if (level > zsl->level) {
        for (i = zsl->level; i < level; i++) {
            rank[i] = 0;
            update[i] = zsl->header;
            update[i]->level[i].span = zsl->length;
        }
        zsl->level = level;
    }
    
    // 创建新节点
    x = zslCreateNode(level,score,obj);
    for (i = 0; i < level; i++) {
        x->level[i].forward = update[i]->level[i].forward;
        update[i]->level[i].forward = x;
        
        x->level[i].span = update[i]->level[i].span - (rank[0] - rank[i]);
        update[i]->level[i].span = (rank[0] - rank[i]) + 1;
    }
    
    for (i = level; i < zsl->level; i++) {
        update[i]->level[i].span++;
    }
    
    x->backward = (update[0] == zsl->header) ? NULL : update[0];
    if (x->level[0].forward)
        x->level[0].forward->backward = x;
    else
        zsl->tail = x;
    
    zsl->length++;
    return x;
}
```

## 二、数据结构

### 2.1 String

**底层实现**：SDS（Simple Dynamic String）

**SDS优势**：
- O(1)获取字符串长度
- 二进制安全
- 减少内存重分配
- 兼容C字符串

**常用命令**：
```redis
SET key value [EX seconds] [NX|XX]
GET key
INCR key
DECR key
MGET key1 key2
MSET key1 value1 key2 value2
```

**应用场景**：
```java
// 1. 计数器
jedis.incr("article:1001:view_count");

// 2. 分布式锁
String lockKey = "lock:order:1001";
String lockValue = UUID.randomUUID().toString();
Boolean result = jedis.set(lockKey, lockValue, "NX", "EX", 30);

// 3. 缓存对象
User user = userService.getUser(1001L);
jedis.setex("user:1001", 3600, JSON.toJSONString(user));
```

### 1.2 Hash

**常用命令**：
```redis
HSET key field value
HGET key field
HMGET key field1 field2
HGETALL key
HINCRBY key field increment
```

**应用场景**：
```java
// 存储对象
Map<String, String> user = new HashMap<>();
user.put("name", "John");
user.put("age", "30");
jedis.hmset("user:1001", user);

// 购物车
jedis.hincrby("cart:user:1001", "product:2001", 1);  // 增加商品数量
```

### 1.3 List

**底层实现**：QuickList（ziplist + linkedlist）

**常用命令**：
```redis
LPUSH key value
RPUSH key value
LPOP key
RPOP key
LRANGE key start stop
LTRIM key start stop
```

**应用场景**：
```java
// 1. 消息队列
jedis.lpush("queue:task", task);
String task = jedis.rpop("queue:task");

// 2. 最新列表
jedis.lpush("news:latest", newsId);
jedis.ltrim("news:latest", 0, 99);  // 保留最新100条
List<String> latest = jedis.lrange("news:latest", 0, 9);  // 取前10条
```

### 1.4 Set

**常用命令**：
```redis
SADD key member
SREM key member
SISMEMBER key member
SCARD key
SINTER key1 key2        # 交集
SUNION key1 key2        # 并集
SDIFF key1 key2         # 差集
```

**应用场景**：
```java
// 1. 标签系统
jedis.sadd("user:1001:tags", "java", "redis", "mysql");

// 2. 共同好友
jedis.sinter("user:1001:friends", "user:1002:friends");

// 3. 去重
jedis.sadd("article:1001:read_users", "user:1001");
```

### 1.5 Sorted Set

**底层实现**：skiplist + hashtable

**常用命令**：
```redis
ZADD key score member
ZRANGE key start stop [WITHSCORES]
ZREVRANGE key start stop
ZINCRBY key increment member
ZRANK key member
```

**应用场景**：
```java
// 1. 排行榜
jedis.zincrby("rank:game:score", 100, "user:1001");
Set<String> top10 = jedis.zrevrange("rank:game:score", 0, 9);

// 2. 延迟队列
long delayTime = System.currentTimeMillis() + 60000;
jedis.zadd("delay:queue", delayTime, taskId);

// 获取到期任务
Set<String> tasks = jedis.zrangeByScore("delay:queue", 0, System.currentTimeMillis());

// 3. 限流（滑动窗口）
String key = "rate:limit:user:1001";
long now = System.currentTimeMillis();
jedis.zadd(key, now, UUID.randomUUID().toString());
jedis.zremrangeByScore(key, 0, now - 60000);  // 删除1分钟前的
long count = jedis.zcard(key);
if (count > 100) {
    // 限流
}
```

## 二、持久化

### 2.1 RDB（快照）

**配置**：
```redis
save 900 1      # 900秒内至少1次修改
save 300 10     # 300秒内至少10次修改
save 60 10000   # 60秒内至少10000次修改
```

**手动触发**：
```redis
SAVE      # 阻塞
BGSAVE    # 后台保存
```

**优缺点**：
- **优点**：恢复快、文件紧凑
- **缺点**：可能丢失数据、fork子进程消耗资源

### 2.2 AOF（追加日志）

**配置**：
```redis
appendonly yes
appendfsync always      # 每次写入都sync
appendfsync everysec    # 每秒sync（推荐）
appendfsync no          # 由OS决定
```

**AOF重写**：
```redis
AUTO-AOF-REWRITE-PERCENTAGE 100
AUTO-AOF-REWRITE-MIN-SIZE 64mb

# 手动触发
BGREWRITEAOF
```

**优缺点**：
- **优点**：数据安全、可读性好
- **缺点**：文件大、恢复慢

### 2.3 混合持久化（Redis 4.0+）

```redis
aof-use-rdb-preamble yes
```

**原理**：
- AOF文件前半部分是RDB格式
- 后半部分是AOF格式
- 结合两者优点

## 三、Redis集群原理深度解析

### 3.1 主从复制深度原理

#### 3.1.1 主从复制源码实现

**主从复制核心实现**：
```c
// 主从复制状态机
typedef enum {
    REPL_STATE_NONE = 0,           // 无复制状态
    REPL_STATE_CONNECT,            // 连接主服务器
    REPL_STATE_CONNECTING,         // 正在连接
    REPL_STATE_RECEIVE_PONG,       // 等待PONG响应
    REPL_STATE_SEND_AUTH,          // 发送AUTH
    REPL_STATE_RECEIVE_AUTH,       // 等待AUTH响应
    REPL_STATE_SEND_PORT,          // 发送端口信息
    REPL_STATE_RECEIVE_PORT,       // 等待端口响应
    REPL_STATE_SEND_CAPA,          // 发送能力信息
    REPL_STATE_RECEIVE_CAPA,       // 等待能力响应
    REPL_STATE_SEND_PSYNC,         // 发送PSYNC命令
    REPL_STATE_RECEIVE_PSYNC,      // 等待PSYNC响应
    REPL_STATE_TRANSFER,           // 接收RDB文件
    REPL_STATE_CONNECTED,          // 连接建立
} repl_state;

// 复制信息结构
typedef struct {
    char *masterhost;             // 主服务器地址
    int masterport;               // 主服务器端口
    char *masterauth;              // 主服务器密码
    int repl_state;               // 复制状态
    int repl_syncio_timeout;      // 同步IO超时
    long long master_repl_offset; // 主服务器复制偏移量
    long long repl_master_initial_offset; // 初始偏移量
    int repl_transfer_size;       // 传输大小
    int repl_transfer_read;       // 已读取大小
    int repl_transfer_last_fsync_off; // 最后fsync偏移
    int repl_transfer_fd;         // 传输文件描述符
    int repl_transfer_s;          // 传输socket
    char *repl_transfer_tmpfile;  // 临时文件名
    time_t repl_transfer_lastio;  // 最后IO时间
    int repl_serve_stale_data;    // 是否服务过期数据
    int repl_slave_ro;            // 只读从服务器
    time_t repl_down_since;       // 下线时间
    int repl_min_slaves_to_write; // 最小从服务器数
    int repl_min_slaves_max_lag;  // 最大延迟
    list *slaves;                 // 从服务器列表
    char replid[CONFIG_RUN_ID_SIZE+1]; // 复制ID
    long long repl_backlog_size;  // 复制积压缓冲区大小
    long long repl_backlog_histlen; // 历史长度
    long long repl_backlog_idx;   // 索引
    long long repl_backlog_off;   // 偏移量
    char *repl_backlog;           // 复制积压缓冲区
    time_t repl_no_slaves_since;  // 无从服务器时间
    int repl_ping_slave_period;   // ping从服务器周期
    time_t repl_slave_lag;        // 从服务器延迟
} replicationState;

// 处理PSYNC命令
void replicationFeedSlaves(list *slaves, int dictid, robj **argv, int argc) {
    listIter li;
    listNode *ln;
    client *slave;
    int j, len;
    char llstr[LONG_STR_SIZE];
    
    if (server.masterhost != NULL) return;
    if (listLength(slaves) == 0) return;
    
    // 构建命令字符串
    sds cmd = sdsempty();
    for (j = 0; j < argc; j++) {
        if (j != 0) cmd = sdscat(cmd, " ");
        cmd = sdscatlen(cmd, argv[j]->ptr, sdslen(argv[j]->ptr));
    }
    
    // 发送给所有从服务器
    listRewind(slaves, &li);
    while((ln = listNext(&li))) {
        slave = ln->value;
        
        // 检查从服务器状态
        if (slave->replstate == SLAVE_STATE_WAIT_BGSAVE_START ||
            slave->replstate == SLAVE_STATE_WAIT_BGSAVE_END ||
            slave->replstate == SLAVE_STATE_SEND_BULK) {
            continue;
        }
        
        // 发送命令
        addReplyMultiBulkLen(slave, argc);
        for (j = 0; j < argc; j++) {
            addReplyBulk(slave, argv[j]);
        }
    }
    
    sdsfree(cmd);
}
```

#### 3.1.2 增量复制实现原理

**增量复制核心算法**：
```c
// 处理PSYNC命令
int masterTryPartialResynchronization(client *c) {
    long long psync_offset, psync_len;
    char *master_replid = c->argv[1]->ptr;
    char buf[128];
    int buflen;
    
    // 检查复制ID是否匹配
    if (strcasecmp(master_replid, server.replid) &&
        (strcasecmp(master_replid, server.replid2) ||
         psync_offset > server.second_replid_offset)) {
        goto need_full_resync;
    }
    
    // 检查偏移量是否在积压缓冲区中
    if (!server.repl_backlog ||
        psync_offset < server.repl_backlog_off ||
        psync_offset > (server.repl_backlog_off + server.repl_backlog_histlen)) {
        goto need_full_resync;
    }
    
    // 执行增量同步
    c->flags |= CLIENT_SLAVE;
    c->replstate = SLAVE_STATE_ONLINE;
    c->repl_ack_time = server.unixtime;
    c->repl_put_online_on_ack = 0;
    listAddNodeTail(server.slaves, c);
    
    // 发送+CONTINUE响应
    buflen = snprintf(buf, sizeof(buf), "+CONTINUE %s\r\n", server.replid);
    if (write(c->fd, buf, buflen) != buflen) {
        freeClientAsync(c);
        return C_OK;
    }
    
    // 发送积压缓冲区中的数据
    psync_len = server.repl_backlog_histlen - (psync_offset - server.repl_backlog_off);
    if (psync_len > 0) {
        if (write(c->fd, server.repl_backlog + (psync_offset - server.repl_backlog_off), psync_len) != psync_len) {
            freeClientAsync(c);
            return C_OK;
        }
    }
    
    return C_OK;
    
need_full_resync:
    return C_ERR;
}
```

### 3.2 Redis Cluster深度原理

#### 3.2.1 Cluster架构深度解析

**Cluster节点结构**：
```c
// Cluster节点结构
typedef struct clusterNode {
    mstime_t ctime;                 // 节点创建时间
    char name[CLUSTER_NODENAME_LEN]; // 节点名称
    int flags;                      // 节点标志
    uint64_t configEpoch;           // 配置纪元
    char ip[NET_IP_STR_LEN];        // IP地址
    int port;                       // 端口
    int cport;                      // 集群端口
    clusterLink *link;              // 连接信息
    list *fail_reports;             // 故障报告
    dict *slots;                    // 槽位信息
    dict *slaveof;                  // 主从关系
    unsigned char slots[CLUSTER_SLOTS/8]; // 槽位位图
    int numslots;                   // 槽位数量
    int numslaves;                  // 从节点数量
    struct clusterNode **slaves;    // 从节点数组
    struct clusterNode *slaveof;    // 主节点
    mstime_t ping_sent;             // 最后ping时间
    mstime_t pong_received;         // 最后pong时间
    mstime_t data_received;         // 最后数据接收时间
    mstime_t fail_time;             // 故障时间
    mstime_t voted_time;             // 投票时间
    mstime_t orphaned_time;         // 孤立时间
    long long repl_offset;          // 复制偏移量
    char replid[CONFIG_RUN_ID_SIZE+1]; // 复制ID
    struct clusterNode **slaves;    // 从节点数组
    int numslaves;                  // 从节点数量
    struct clusterNode *slaveof;    // 主节点
} clusterNode;

// Cluster状态
typedef struct clusterState {
    clusterNode *myself;            // 当前节点
    uint64_t currentEpoch;         // 当前纪元
    int state;                      // 集群状态
    int size;                       // 集群大小
    dict *nodes;                    // 节点字典
    dict *nodes_black_list;         // 黑名单
    clusterNode *migrating_slots_to[CLUSTER_SLOTS]; // 迁移槽位
    clusterNode *importing_slots_from[CLUSTER_SLOTS]; // 导入槽位
    clusterNode *slots[CLUSTER_SLOTS]; // 槽位数组
    zskiplist *slots_to_keys;       // 槽位到键的映射
} clusterState;
```

#### 3.2.2 槽位分配算法

**槽位分配实现**：
```c
// 计算键的槽位
unsigned int keyHashSlot(char *key, int keylen) {
    int s, e; // start-end indexes of { and }
    
    for (s = 0; s < keylen; s++)
        if (key[s] == '{') break;
    
    // 没有找到{，使用整个键
    if (s == keylen) return crc16(key, keylen) & 0x3FFF;
    
    // 找到{，查找}
    for (e = s+1; e < keylen; e++)
        if (key[e] == '}') break;
    
    // 没有找到}，使用整个键
    if (e == keylen || e == s+1) return crc16(key, keylen) & 0x3FFF;
    
    // 使用{和}之间的内容
    return crc16(key+s+1, e-s-1) & 0x3FFF;
}

// 槽位迁移
int clusterMigrateSlot(clusterNode *n, int slot) {
    clusterNode *target = n->slots[slot];
    if (!target) return 0;
    
    // 开始迁移
    clusterNode *migrating = server.cluster->migrating_slots_to[slot];
    if (migrating) return 0;
    
    server.cluster->migrating_slots_to[slot] = target;
    return 1;
}

// 处理槽位迁移
void clusterHandleSlotMigration(clusterNode *node, int slot) {
    robj *key;
    dictIterator *di;
    dictEntry *de;
    
    // 获取槽位中的所有键
    di = dictGetIterator(server.db[0].dict);
    while((de = dictNext(di)) != NULL) {
        key = dictGetKey(de);
        if (keyHashSlot(key->ptr, sdslen(key->ptr)) == slot) {
            // 迁移键
            clusterMigrateKey(key, node);
        }
    }
    dictReleaseIterator(di);
}
```

#### 3.2.3 故障检测与转移

**故障检测实现**：
```c
// 故障检测
void clusterCron(void) {
    dictIterator *di;
    dictEntry *de;
    clusterNode *node;
    mstime_t now = mstime();
    mstime_t min_pong = 0;
    static unsigned long long iteration = 0;
    int j;
    
    iteration++;
    
    // 检查所有节点的ping/pong状态
    di = dictGetSafeIterator(server.cluster->nodes);
    while((de = dictNext(di)) != NULL) {
        node = dictGetVal(de);
        
        // 跳过自己
        if (node->flags & (CLUSTER_NODE_MYSELF|CLUSTER_NODE_HANDSHAKE)) continue;
        
        // 检查ping超时
        if (node->link && 
            (now - node->link->ctime) > server.cluster_node_timeout) {
            clusterDelNode(node);
            continue;
        }
        
        // 发送ping
        if (node->link && 
            (now - node->ping_sent) > server.cluster_node_timeout/2) {
            clusterSendPing(node->link, CLUSTERMSG_TYPE_PING);
            node->ping_sent = now;
        }
    }
    dictReleaseIterator(di);
    
    // 检查故障转移
    if (server.cluster->failover_auth_time) {
        if ((now - server.cluster->failover_auth_time) > 2000) {
            server.cluster->failover_auth_time = 0;
            server.cluster->failover_auth_count = 0;
        }
    }
}

// 故障转移
void clusterHandleSlaveFailover(void) {
    clusterNode *master, *slave;
    mstime_t data_age;
    int auth_age, master_numslaves;
    
    // 检查是否可以进行故障转移
    if (server.cluster->state != CLUSTER_STATE_OK) return;
    if (nodeIsMaster(server.cluster->myself)) return;
    
    master = server.cluster->myself->slaveof;
    if (!master) return;
    
    // 检查主节点是否真的故障
    if (master->flags & (CLUSTER_NODE_PFAIL|CLUSTER_NODE_FAIL)) == 0) return;
    
    // 检查数据新鲜度
    data_age = (mstime() - server.cluster->myself->slaveof->slave_repl_offset);
    if (data_age > server.cluster_slave_validity_factor * 1000) return;
    
    // 开始故障转移
    server.cluster->failover_auth_time = mstime();
    server.cluster->failover_auth_count = 0;
    server.cluster->failover_auth_rank = clusterGetSlaveRank();
    server.cluster->failover_auth_epoch = server.cluster->currentEpoch++;
    
    // 请求投票
    clusterRequestFailoverAuth();
}
```

### 3.3 集群扩容与缩容

#### 3.3.1 集群扩容实现

**扩容流程实现**：
```java
// 集群扩容实现
public class ClusterResharding {
    
    // 添加新节点
    public void addNode(String newNodeHost, int newNodePort) {
        // 1. 启动新节点
        startNewNode(newNodeHost, newNodePort);
        
        // 2. 加入集群
        joinCluster(newNodeHost, newNodePort);
        
        // 3. 重新分配槽位
        reshardSlots();
        
        // 4. 迁移数据
        migrateData();
    }
    
    // 重新分配槽位
    private void reshardSlots() {
        // 计算每个节点应该负责的槽位数量
        int totalSlots = 16384;
        int nodeCount = getClusterNodeCount();
        int slotsPerNode = totalSlots / nodeCount;
        
        // 重新分配槽位
        for (int i = 0; i < totalSlots; i++) {
            int targetNode = i / slotsPerNode;
            assignSlotToNode(i, targetNode);
        }
    }
    
    // 迁移数据
    private void migrateData() {
        for (int slot = 0; slot < 16384; slot++) {
            String sourceNode = getSlotOwner(slot);
            String targetNode = getSlotTarget(slot);
            
            if (!sourceNode.equals(targetNode)) {
                migrateSlotData(slot, sourceNode, targetNode);
            }
        }
    }
    
    // 迁移槽位数据
    private void migrateSlotData(int slot, String sourceNode, String targetNode) {
        // 1. 设置迁移状态
        setSlotMigrating(slot, sourceNode);
        setSlotImporting(slot, targetNode);
        
        // 2. 迁移键
        List<String> keys = getKeysInSlot(slot, sourceNode);
        for (String key : keys) {
            migrateKey(key, sourceNode, targetNode);
        }
        
        // 3. 完成迁移
        setSlotNode(slot, targetNode);
        clearSlotMigrating(slot);
        clearSlotImporting(slot);
    }
}
```

## 四、集群方案

### 3.2 Sentinel（哨兵）

**配置**：
```redis
# sentinel.conf
sentinel monitor mymaster 192.168.1.10 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel parallel-syncs mymaster 1
sentinel failover-timeout mymaster 180000
```

**工作原理**：
1. **监控**：检测Master和Slave是否正常
2. **通知**：通知管理员故障
3. **故障转移**：选举新Master
4. **配置提供**：提供Master地址

### 3.3 Cluster（集群）

**特点**：
- 数据分片（16384个slot）
- 去中心化
- 支持水平扩展

**创建集群**：
```bash
redis-cli --cluster create \
    192.168.1.10:6379 192.168.1.11:6379 192.168.1.12:6379 \
    192.168.1.10:6380 192.168.1.11:6380 192.168.1.12:6380 \
    --cluster-replicas 1
```

**Slot计算**：
```
slot = CRC16(key) % 16384
```

**Java客户端**：
```java
Set<HostAndPort> nodes = new HashSet<>();
nodes.add(new HostAndPort("192.168.1.10", 6379));
nodes.add(new HostAndPort("192.168.1.11", 6379));
nodes.add(new HostAndPort("192.168.1.12", 6379));

JedisCluster cluster = new JedisCluster(nodes);
cluster.set("key", "value");
```

## 四、缓存策略

### 4.1 缓存更新策略

**Cache Aside**（推荐）：
```java
// 读
public User getUser(Long id) {
    String key = "user:" + id;
    String cached = jedis.get(key);
    if (cached != null) {
        return JSON.parseObject(cached, User.class);
    }
    
    User user = userMapper.selectById(id);
    if (user != null) {
        jedis.setex(key, 3600, JSON.toJSONString(user));
    }
    return user;
}

// 写
public void updateUser(User user) {
    userMapper.updateById(user);
    jedis.del("user:" + user.getId());  // 删除缓存
}
```

### 4.2 缓存问题

**1. 缓存穿透**：
```java
// 布隆过滤器
@Component
public class BloomFilterService {
    private BloomFilter<Long> bloomFilter = BloomFilter.create(
        Funnels.longFunnel(),
        100000000,
        0.01
    );
    
    public User getUser(Long id) {
        if (!bloomFilter.mightContain(id)) {
            return null;  // 一定不存在
        }
        // 查询缓存和数据库
    }
}

// 缓存空值
public User getUser(Long id) {
    String key = "user:" + id;
    String cached = jedis.get(key);
    
    if (cached != null) {
        return "null".equals(cached) ? null : JSON.parseObject(cached, User.class);
    }
    
    User user = userMapper.selectById(id);
    if (user != null) {
        jedis.setex(key, 3600, JSON.toJSONString(user));
    } else {
        jedis.setex(key, 60, "null");  // 缓存空值，短过期
    }
    return user;
}
```

**2. 缓存击穿**：
```java
// 互斥锁
public User getUser(Long id) {
    String key = "user:" + id;
    String cached = jedis.get(key);
    if (cached != null) {
        return JSON.parseObject(cached, User.class);
    }
    
    String lockKey = "lock:user:" + id;
    try {
        if (jedis.setnx(lockKey, "1") == 1) {
            jedis.expire(lockKey, 10);
            
            User user = userMapper.selectById(id);
            jedis.setex(key, 3600, JSON.toJSONString(user));
            return user;
        } else {
            Thread.sleep(100);
            return getUser(id);  // 重试
        }
    } finally {
        jedis.del(lockKey);
    }
}
```

**3. 缓存雪崩**：
```java
// 随机过期时间
int expireTime = 3600 + new Random().nextInt(600);  // 3600~4200秒
jedis.setex(key, expireTime, value);

// 永不过期 + 异步更新
jedis.set(key, value);  // 不设置过期时间

// 定时任务更新
@Scheduled(fixedRate = 3000000)
public void refreshCache() {
    // 更新热点数据
}
```

## 五、实战场景

### 5.1 分布式锁

**Redisson实现**：
```java
@Component
public class RedisLockService {
    
    @Autowired
    private RedissonClient redissonClient;
    
    public void doSomething() {
        RLock lock = redissonClient.getLock("myLock");
        
        try {
            if (lock.tryLock(100, 10, TimeUnit.SECONDS)) {
                // 业务逻辑
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### 5.2 限流

**滑动窗口限流**：
```java
public boolean checkLimit(String userId, int limit, int window) {
    String key = "rate:limit:" + userId;
    long now = System.currentTimeMillis();
    
    // 添加当前时间戳
    jedis.zadd(key, now, UUID.randomUUID().toString());
    
    // 删除窗口外的数据
    jedis.zremrangeByScore(key, 0, now - window * 1000);
    
    // 统计窗口内的请求数
    long count = jedis.zcard(key);
    
    // 设置过期时间
    jedis.expire(key, window);
    
    return count <= limit;
}
```

### 5.3 排行榜

```java
@Service
public class RankService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    // 增加分数
    public void addScore(Long userId, double score) {
        String key = "rank:game";
        redisTemplate.opsForZSet().incrementScore(key, String.valueOf(userId), score);
    }
    
    // 获取排名
    public Long getRank(Long userId) {
        String key = "rank:game";
        return redisTemplate.opsForZSet().reverseRank(key, String.valueOf(userId));
    }
    
    // 获取Top N
    public Set<String> getTopN(int n) {
        String key = "rank:game";
        return redisTemplate.opsForZSet().reverseRange(key, 0, n - 1);
    }
}
```

## 六、性能优化

### 6.1 慢查询

**配置**：
```redis
CONFIG SET slowlog-log-slower-than 10000  # 10ms
CONFIG SET slowlog-max-len 128

# 查看慢查询
SLOWLOG GET 10
```

### 6.2 Pipeline

```java
// 普通方式
for (int i = 0; i < 10000; i++) {
    jedis.set("key:" + i, "value:" + i);  // 10000次网络往返
}

// Pipeline方式
Pipeline pipeline = jedis.pipelined();
for (int i = 0; i < 10000; i++) {
    pipeline.set("key:" + i, "value:" + i);
}
pipeline.sync();  // 1次网络往返
```

### 6.3 内存优化

**1. 选择合适的数据结构**：
```redis
# Hash比String节省内存
HSET user:1001 name John
HSET user:1001 age 30

# 比
SET user:1001:name John
SET user:1001:age 30
```

**2. 设置过期时间**：
```java
jedis.setex(key, 3600, value);
```

**3. 内存淘汰策略**：
```redis
maxmemory 2gb
maxmemory-policy allkeys-lru  # LRU淘汰
```

## 七、高频面试题

### Q1：Redis为什么快？

1. **内存存储**：内存读写速度快
2. **单线程**：避免上下文切换和竞争
3. **IO多路复用**：高效处理并发连接
4. **优化的数据结构**：SDS、ziplist等

### Q2：Redis单线程如何处理并发？

- **IO多路复用**（epoll/select）
- 单线程处理请求，避免锁竞争
- Redis 6.0引入多线程处理网络IO

### Q3：RDB和AOF的区别？

| 特性 | RDB | AOF |
|------|-----|-----|
| 持久化方式 | 快照 | 追加日志 |
| 恢复速度 | 快 | 慢 |
| 数据安全 | 可能丢失 | 更安全 |
| 文件大小 | 小 | 大 |

### Q4：如何保证Redis和MySQL数据一致性？

**方案1：延迟双删**：
```java
public void updateUser(User user) {
    jedis.del("user:" + user.getId());  // 删除缓存
    userMapper.updateById(user);
    Thread.sleep(500);
    jedis.del("user:" + user.getId());  // 再次删除
}
```

**方案2：Canal监听binlog**：
```
MySQL binlog → Canal → MQ → 更新Redis
```

### Q5：Redis集群如何扩容？

**Cluster扩容**：
```bash
# 1. 启动新节点
redis-server --port 6383

# 2. 加入集群
redis-cli --cluster add-node 192.168.1.13:6383 192.168.1.10:6379

# 3. 重新分配slot
redis-cli --cluster reshard 192.168.1.10:6379
```

---

**关键字**：Redis、数据结构、持久化、集群、缓存、分布式锁、Redisson

