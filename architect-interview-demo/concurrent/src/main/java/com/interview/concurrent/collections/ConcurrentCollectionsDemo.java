package com.interview.concurrent.collections;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 并发集合使用示例
 * 
 * 核心集合：
 * - ConcurrentHashMap - 线程安全的HashMap
 * - CopyOnWriteArrayList - 写时复制List
 * - BlockingQueue - 阻塞队列
 * - ConcurrentLinkedQueue - 无锁队列
 */
public class ConcurrentCollectionsDemo {
    
    /**
     * ConcurrentHashMap示例
     */
    public static class ConcurrentHashMapDemo {
        private final ConcurrentHashMap<String, AtomicInteger> counterMap = new ConcurrentHashMap<>();
        
        /**
         * 线程安全的计数
         */
        public void increment(String key) {
            counterMap.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
        }
        
        /**
         * 获取计数
         */
        public int getCount(String key) {
            return counterMap.getOrDefault(key, new AtomicInteger(0)).get();
        }
        
        /**
         * 批量操作
         */
        public void batchIncrement(String[] keys) {
            for (String key : keys) {
                counterMap.compute(key, (k, v) -> {
                    if (v == null) {
                        return new AtomicInteger(1);
                    } else {
                        v.incrementAndGet();
                        return v;
                    }
                });
            }
        }
        
        /**
         * 搜索操作
         */
        public String findMaxKey() {
            return counterMap.reduceEntries(1, (entry1, entry2) -> 
                entry1.getValue().get() > entry2.getValue().get() ? entry1 : entry2
            ).getKey();
        }
    }
    
    /**
     * CopyOnWriteArrayList示例
     */
    public static class CopyOnWriteListDemo {
        private final CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
        
        /**
         * 添加元素（写操作会复制整个数组）
         */
        public void add(String item) {
            list.add(item);
        }
        
        /**
         * 读取元素（读操作不需要锁）
         */
        public String get(int index) {
            return list.get(index);
        }
        
        /**
         * 遍历（快照遍历，不会受并发修改影响）
         */
        public void iterate() {
            for (String item : list) {
                System.out.println("Item: " + item);
            }
        }
        
        /**
         * 批量添加
         */
        public void addAll(String[] items) {
            list.addAll(java.util.Arrays.asList(items));
        }
    }
    
    /**
     * BlockingQueue示例
     */
    public static class BlockingQueueDemo {
        private final BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);
        
        /**
         * 生产者
         */
        public void produce(String item) throws InterruptedException {
            queue.put(item); // 阻塞直到有空间
            System.out.println("Produced: " + item + ", Queue size: " + queue.size());
        }
        
        /**
         * 消费者
         */
        public String consume() throws InterruptedException {
            String item = queue.take(); // 阻塞直到有元素
            System.out.println("Consumed: " + item + ", Queue size: " + queue.size());
            return item;
        }
        
        /**
         * 非阻塞消费
         */
        public String poll() {
            String item = queue.poll(); // 非阻塞，可能返回null
            if (item != null) {
                System.out.println("Polled: " + item + ", Queue size: " + queue.size());
            }
            return item;
        }
        
        /**
         * 非阻塞生产
         */
        public boolean offer(String item) {
            boolean success = queue.offer(item); // 非阻塞，可能失败
            if (success) {
                System.out.println("Offered: " + item + ", Queue size: " + queue.size());
            } else {
                System.out.println("Failed to offer: " + item + " (queue full)");
            }
            return success;
        }
    }
    
    /**
     * ConcurrentLinkedQueue示例
     */
    public static class ConcurrentLinkedQueueDemo {
        private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
        
        /**
         * 添加元素
         */
        public void offer(String item) {
            queue.offer(item);
            System.out.println("Offered: " + item + ", Queue size: " + queue.size());
        }
        
        /**
         * 获取并移除元素
         */
        public String poll() {
            String item = queue.poll();
            if (item != null) {
                System.out.println("Polled: " + item + ", Queue size: " + queue.size());
            }
            return item;
        }
        
        /**
         * 获取但不移除元素
         */
        public String peek() {
            return queue.peek();
        }
        
        /**
         * 检查是否为空
         */
        public boolean isEmpty() {
            return queue.isEmpty();
        }
    }
    
    /**
     * 生产者-消费者示例
     */
    public static class ProducerConsumerDemo {
        private final BlockingQueue<String> queue = new ArrayBlockingQueue<>(5);
        private volatile boolean running = true;
        
        /**
         * 生产者线程
         */
        class Producer implements Runnable {
            private final String name;
            
            public Producer(String name) {
                this.name = name;
            }
            
            @Override
            public void run() {
                int count = 0;
                while (running) {
                    try {
                        String item = name + "-item-" + (++count);
                        queue.put(item);
                        Thread.sleep(1000); // 模拟生产时间
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                System.out.println("Producer " + name + " stopped");
            }
        }
        
        /**
         * 消费者线程
         */
        class Consumer implements Runnable {
            private final String name;
            
            public Consumer(String name) {
                this.name = name;
            }
            
            @Override
            public void run() {
                while (running || !queue.isEmpty()) {
                    try {
                        String item = queue.poll(500, TimeUnit.MILLISECONDS);
                        if (item != null) {
                            System.out.println("Consumer " + name + " consumed: " + item);
                            Thread.sleep(1500); // 模拟消费时间
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                System.out.println("Consumer " + name + " stopped");
            }
        }
        
        /**
         * 运行生产者-消费者示例
         */
        public void runDemo() throws InterruptedException {
            ExecutorService executor = Executors.newFixedThreadPool(4);
            
            // 启动生产者
            executor.submit(new Producer("P1"));
            executor.submit(new Producer("P2"));
            
            // 启动消费者
            executor.submit(new Consumer("C1"));
            executor.submit(new Consumer("C2"));
            
            // 运行10秒
            Thread.sleep(10000);
            
            // 停止
            running = false;
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== ConcurrentHashMap Demo ==========");
        ConcurrentHashMapDemo chmDemo = new ConcurrentHashMapDemo();
        
        // 多线程计数
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 100; i++) {
            final int threadId = i % 3;
            executor.submit(() -> chmDemo.increment("counter-" + threadId));
        }
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        System.out.println("Counter-0: " + chmDemo.getCount("counter-0"));
        System.out.println("Counter-1: " + chmDemo.getCount("counter-1"));
        System.out.println("Counter-2: " + chmDemo.getCount("counter-2"));
        
        System.out.println("\n========== CopyOnWriteArrayList Demo ==========");
        CopyOnWriteListDemo cowDemo = new CopyOnWriteListDemo();
        cowDemo.addAll(new String[]{"item1", "item2", "item3"});
        cowDemo.iterate();
        
        System.out.println("\n========== BlockingQueue Demo ==========");
        BlockingQueueDemo bqDemo = new BlockingQueueDemo();
        
        // 生产者线程
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    bqDemo.produce("item-" + i);
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // 消费者线程
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    bqDemo.consume();
                    Thread.sleep(800);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        producer.start();
        consumer.start();
        
        producer.join();
        consumer.join();
        
        System.out.println("\n========== Producer-Consumer Demo ==========");
        ProducerConsumerDemo pcDemo = new ProducerConsumerDemo();
        pcDemo.runDemo();
    }
}
