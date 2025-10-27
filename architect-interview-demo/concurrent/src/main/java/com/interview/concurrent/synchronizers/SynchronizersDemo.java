package com.interview.concurrent.synchronizers;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 同步器使用示例
 * 
 * 核心同步器：
 * - CountDownLatch - 倒计时门闩
 * - CyclicBarrier - 循环栅栏
 * - Semaphore - 信号量
 * - Exchanger - 交换器
 * - Phaser - 阶段器
 */
public class SynchronizersDemo {
    
    /**
     * CountDownLatch示例 - 等待多个任务完成
     */
    public static class CountDownLatchDemo {
        private final CountDownLatch latch;
        private final AtomicInteger completedTasks = new AtomicInteger(0);
        
        public CountDownLatchDemo(int taskCount) {
            this.latch = new CountDownLatch(taskCount);
        }
        
        /**
         * 执行任务
         */
        public void executeTask(String taskName, int duration) {
            try {
                System.out.println("Task " + taskName + " started");
                Thread.sleep(duration * 1000); // 模拟任务执行
                System.out.println("Task " + taskName + " completed");
                
                completedTasks.incrementAndGet();
                latch.countDown(); // 任务完成，计数减1
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Task " + taskName + " interrupted");
            }
        }
        
        /**
         * 等待所有任务完成
         */
        public void waitForCompletion() throws InterruptedException {
            System.out.println("Waiting for all tasks to complete...");
            latch.await(); // 阻塞直到计数为0
            System.out.println("All tasks completed! Total: " + completedTasks.get());
        }
        
        /**
         * 带超时的等待
         */
        public boolean waitForCompletionWithTimeout(long timeout, TimeUnit unit) throws InterruptedException {
            boolean completed = latch.await(timeout, unit);
            if (completed) {
                System.out.println("All tasks completed within timeout");
            } else {
                System.out.println("Timeout reached, some tasks may not be completed");
            }
            return completed;
        }
    }
    
    /**
     * CyclicBarrier示例 - 多线程同步点
     */
    public static class CyclicBarrierDemo {
        private final CyclicBarrier barrier;
        private final AtomicInteger phase = new AtomicInteger(0);
        
        public CyclicBarrierDemo(int parties) {
            this.barrier = new CyclicBarrier(parties, () -> {
                int currentPhase = phase.incrementAndGet();
                System.out.println("=== Phase " + currentPhase + " completed ===");
            });
        }
        
        /**
         * 执行阶段任务
         */
        public void executePhase(String workerName, int phaseNumber) {
            try {
                System.out.println("Worker " + workerName + " starting phase " + phaseNumber);
                
                // 模拟工作
                Thread.sleep(1000 + (int)(Math.random() * 2000));
                
                System.out.println("Worker " + workerName + " finished phase " + phaseNumber);
                
                // 等待其他线程到达
                int arrivalIndex = barrier.await();
                System.out.println("Worker " + workerName + " arrived at barrier, index: " + arrivalIndex);
                
            } catch (InterruptedException | BrokenBarrierException e) {
                Thread.currentThread().interrupt();
                System.out.println("Worker " + workerName + " interrupted");
            }
        }
        
        /**
         * 重置栅栏
         */
        public void reset() {
            barrier.reset();
            System.out.println("Barrier reset");
        }
    }
    
    /**
     * Semaphore示例 - 控制并发访问数量
     */
    public static class SemaphoreDemo {
        private final Semaphore semaphore;
        private final AtomicInteger activeConnections = new AtomicInteger(0);
        
        public SemaphoreDemo(int permits) {
            this.semaphore = new Semaphore(permits);
        }
        
        /**
         * 获取资源
         */
        public boolean acquireResource(String clientName) {
            try {
                System.out.println("Client " + clientName + " requesting resource...");
                semaphore.acquire(); // 获取许可
                
                int active = activeConnections.incrementAndGet();
                System.out.println("Client " + clientName + " acquired resource. Active: " + active);
                
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Client " + clientName + " interrupted while waiting");
                return false;
            }
        }
        
        /**
         * 尝试获取资源（非阻塞）
         */
        public boolean tryAcquireResource(String clientName) {
            boolean acquired = semaphore.tryAcquire();
            if (acquired) {
                int active = activeConnections.incrementAndGet();
                System.out.println("Client " + clientName + " acquired resource. Active: " + active);
            } else {
                System.out.println("Client " + clientName + " failed to acquire resource (no permits available)");
            }
            return acquired;
        }
        
        /**
         * 释放资源
         */
        public void releaseResource(String clientName) {
            semaphore.release();
            int active = activeConnections.decrementAndGet();
            System.out.println("Client " + clientName + " released resource. Active: " + active);
        }
        
        /**
         * 获取可用许可数
         */
        public int getAvailablePermits() {
            return semaphore.availablePermits();
        }
    }
    
    /**
     * Exchanger示例 - 线程间交换数据
     */
    public static class ExchangerDemo {
        private final Exchanger<String> exchanger = new Exchanger<>();
        
        /**
         * 生产者线程
         */
        class Producer implements Runnable {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < 3; i++) {
                        String data = "Data-" + i;
                        System.out.println("Producer sending: " + data);
                        
                        // 交换数据
                        String received = exchanger.exchange(data);
                        System.out.println("Producer received: " + received);
                        
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        /**
         * 消费者线程
         */
        class Consumer implements Runnable {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < 3; i++) {
                        String data = "Response-" + i;
                        System.out.println("Consumer sending: " + data);
                        
                        // 交换数据
                        String received = exchanger.exchange(data);
                        System.out.println("Consumer received: " + received);
                        
                        Thread.sleep(1500);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        /**
         * 运行交换示例
         */
        public void runExchange() throws InterruptedException {
            Thread producer = new Thread(new Producer());
            Thread consumer = new Thread(new Consumer());
            
            producer.start();
            consumer.start();
            
            producer.join();
            consumer.join();
        }
    }
    
    /**
     * Phaser示例 - 分阶段同步
     */
    public static class PhaserDemo {
        private final Phaser phaser;
        
        public PhaserDemo(int parties) {
            this.phaser = new Phaser(parties);
        }
        
        /**
         * 工作线程
         */
        class Worker implements Runnable {
            private final String name;
            
            public Worker(String name) {
                this.name = name;
            }
            
            @Override
            public void run() {
                try {
                    // 阶段1：准备
                    System.out.println(name + " preparing...");
                    Thread.sleep(1000);
                    int phase1 = phaser.arriveAndAwaitAdvance();
                    System.out.println(name + " completed phase 1, current phase: " + phase1);
                    
                    // 阶段2：执行
                    System.out.println(name + " executing...");
                    Thread.sleep(1500);
                    int phase2 = phaser.arriveAndAwaitAdvance();
                    System.out.println(name + " completed phase 2, current phase: " + phase2);
                    
                    // 阶段3：清理
                    System.out.println(name + " cleaning up...");
                    Thread.sleep(500);
                    int phase3 = phaser.arriveAndAwaitAdvance();
                    System.out.println(name + " completed phase 3, current phase: " + phase3);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        /**
         * 运行分阶段示例
         */
        public void runPhases() throws InterruptedException {
            ExecutorService executor = Executors.newFixedThreadPool(3);
            
            for (int i = 0; i < 3; i++) {
                executor.submit(new Worker("Worker-" + i));
            }
            
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== CountDownLatch Demo ==========");
        CountDownLatchDemo latchDemo = new CountDownLatchDemo(3);
        
        ExecutorService executor = Executors.newFixedThreadPool(3);
        executor.submit(() -> latchDemo.executeTask("Task-1", 2));
        executor.submit(() -> latchDemo.executeTask("Task-2", 3));
        executor.submit(() -> latchDemo.executeTask("Task-3", 1));
        
        latchDemo.waitForCompletion();
        executor.shutdown();
        
        System.out.println("\n========== CyclicBarrier Demo ==========");
        CyclicBarrierDemo barrierDemo = new CyclicBarrierDemo(3);
        
        ExecutorService barrierExecutor = Executors.newFixedThreadPool(3);
        for (int i = 0; i < 3; i++) {
            final int phase = i;
            barrierExecutor.submit(() -> barrierDemo.executePhase("Worker-" + phase, phase));
        }
        
        Thread.sleep(5000);
        barrierExecutor.shutdown();
        
        System.out.println("\n========== Semaphore Demo ==========");
        SemaphoreDemo semaphoreDemo = new SemaphoreDemo(2);
        
        ExecutorService semaphoreExecutor = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            final int clientId = i;
            semaphoreExecutor.submit(() -> {
                if (semaphoreDemo.acquireResource("Client-" + clientId)) {
                    try {
                        Thread.sleep(2000); // 使用资源
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        semaphoreDemo.releaseResource("Client-" + clientId);
                    }
                }
            });
        }
        
        Thread.sleep(8000);
        semaphoreExecutor.shutdown();
        
        System.out.println("\n========== Exchanger Demo ==========");
        ExchangerDemo exchangerDemo = new ExchangerDemo();
        exchangerDemo.runExchange();
        
        System.out.println("\n========== Phaser Demo ==========");
        PhaserDemo phaserDemo = new PhaserDemo(3);
        phaserDemo.runPhases();
    }
}
