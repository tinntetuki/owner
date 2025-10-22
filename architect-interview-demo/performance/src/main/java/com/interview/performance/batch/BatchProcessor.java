package com.interview.performance.batch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 批处理优化
 * 
 * 场景：
 * - 批量插入数据库
 * - 批量调用RPC
 * - 批量发送消息
 * 
 * 优点：
 * - 减少网络往返
 * - 提高吞吐量
 * - 降低系统负载
 */
public class BatchProcessor {
    
    /**
     * 简单批处理
     */
    public static class SimpleBatchProcessor<T> {
        private final int batchSize;
        
        public SimpleBatchProcessor(int batchSize) {
            this.batchSize = batchSize;
        }
        
        /**
         * 批量处理数据
         */
        public void process(List<T> allData) {
            int total = allData.size();
            
            for (int i = 0; i < total; i += batchSize) {
                int end = Math.min(i + batchSize, total);
                List<T> batch = allData.subList(i, end);
                
                processBatch(batch);
                
                System.out.println("已处理: " + end + "/" + total);
            }
        }
        
        protected void processBatch(List<T> batch) {
            // 批量插入数据库
            System.out.println("批量处理 " + batch.size() + " 条数据");
        }
    }
    
    /**
     * 异步批处理器 - 自动攒批
     * 
     * 特点：
     * - 达到批量大小时触发
     * - 达到等待时间时触发
     * - 异步执行
     */
    public static class AsyncBatchProcessor<T> {
        private final int batchSize;
        private final long maxWaitMillis;
        private final List<T> buffer;
        private final ScheduledExecutorService scheduler;
        private final ExecutorService executor;
        private long lastFlushTime;
        
        public AsyncBatchProcessor(int batchSize, long maxWaitMillis) {
            this.batchSize = batchSize;
            this.maxWaitMillis = maxWaitMillis;
            this.buffer = new ArrayList<>();
            this.scheduler = Executors.newSingleThreadScheduledExecutor();
            this.executor = Executors.newFixedThreadPool(4);
            this.lastFlushTime = System.currentTimeMillis();
            
            // 定时flush
            scheduler.scheduleAtFixedRate(this::checkAndFlush, maxWaitMillis, maxWaitMillis, TimeUnit.MILLISECONDS);
        }
        
        /**
         * 添加数据
         */
        public synchronized void add(T data) {
            buffer.add(data);
            
            // 达到批量大小，立即flush
            if (buffer.size() >= batchSize) {
                flush();
            }
        }
        
        /**
         * 检查并flush
         */
        private synchronized void checkAndFlush() {
            if (!buffer.isEmpty() && System.currentTimeMillis() - lastFlushTime >= maxWaitMillis) {
                flush();
            }
        }
        
        /**
         * 刷新数据
         */
        private synchronized void flush() {
            if (buffer.isEmpty()) {
                return;
            }
            
            List<T> batch = new ArrayList<>(buffer);
            buffer.clear();
            lastFlushTime = System.currentTimeMillis();
            
            // 异步处理
            executor.submit(() -> processBatch(batch));
        }
        
        protected void processBatch(List<T> batch) {
            System.out.println(Thread.currentThread().getName() + " 处理 " + batch.size() + " 条数据");
            
            // 模拟批量处理
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        /**
         * 关闭
         */
        public void shutdown() {
            flush();  // 最后flush一次
            scheduler.shutdown();
            executor.shutdown();
        }
    }
    
    /**
     * 数据库批量插入示例
     */
    public static class DatabaseBatchInserter {
        
        /**
         * 批量插入（JDBC）
         */
        public void batchInsert(List<User> users) {
            /*
            String sql = "INSERT INTO user (name, age, city) VALUES (?, ?, ?)";
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                
                conn.setAutoCommit(false);
                
                for (User user : users) {
                    ps.setString(1, user.getName());
                    ps.setInt(2, user.getAge());
                    ps.setString(3, user.getCity());
                    ps.addBatch();
                    
                    // 每1000条提交一次
                    if (users.indexOf(user) % 1000 == 0) {
                        ps.executeBatch();
                        conn.commit();
                    }
                }
                
                ps.executeBatch();
                conn.commit();
                
            } catch (SQLException e) {
                e.printStackTrace();
            }
            */
        }
        
        /**
         * MyBatis批量插入
         */
        public void mybatisBatchInsert(List<User> users) {
            /*
            try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
                UserMapper mapper = session.getMapper(UserMapper.class);
                
                for (User user : users) {
                    mapper.insert(user);
                }
                
                session.commit();
            }
            */
        }
    }
    
    static class User {
        private String name;
        private int age;
        private String city;
        
        public User(String name, int age, String city) {
            this.name = name;
            this.age = age;
            this.city = city;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    /**
     * 测试
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== 简单批处理 ==========");
        SimpleBatchProcessor<Integer> simpleBatch = new SimpleBatchProcessor<>(100);
        List<Integer> data = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            data.add(i);
        }
        simpleBatch.process(data);
        
        System.out.println("\n========== 异步批处理 ==========");
        AsyncBatchProcessor<Integer> asyncBatch = new AsyncBatchProcessor<>(50, 1000);
        
        // 模拟添加数据
        for (int i = 0; i < 120; i++) {
            asyncBatch.add(i);
            Thread.sleep(10);
        }
        
        Thread.sleep(2000);  // 等待定时flush
        asyncBatch.shutdown();
    }
}

