package com.interview.concurrent.threadpool;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自定义线程池示例
 * 
 * 核心参数：
 * - corePoolSize: 核心线程数
 * - maximumPoolSize: 最大线程数
 * - keepAliveTime: 非核心线程空闲存活时间
 * - workQueue: 任务队列
 * - threadFactory: 线程工厂
 * - handler: 拒绝策略
 */
public class CustomThreadPoolDemo {
    
    /**
     * 自定义线程工厂 - 设置线程名称和异常处理
     */
    static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        
        public NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }
        
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, namePrefix + "-" + threadNumber.getAndIncrement());
            
            // 设置为非守护线程
            thread.setDaemon(false);
            
            // 设置优先级
            thread.setPriority(Thread.NORM_PRIORITY);
            
            // 设置未捕获异常处理器
            thread.setUncaughtExceptionHandler((t, e) -> {
                System.err.println("线程 " + t.getName() + " 发生异常: " + e.getMessage());
                e.printStackTrace();
            });
            
            return thread;
        }
    }
    
    /**
     * 自定义拒绝策略
     */
    static class CustomRejectedExecutionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            System.err.println("任务被拒绝: " + r.toString());
            System.err.println("线程池状态 - 核心线程数: " + executor.getCorePoolSize() +
                    ", 活跃线程数: " + executor.getActiveCount() +
                    ", 队列大小: " + executor.getQueue().size());
            
            // 自定义处理逻辑：记录日志、告警、重试等
        }
    }
    
    /**
     * 创建标准线程池
     */
    public static ThreadPoolExecutor createStandardThreadPool() {
        return new ThreadPoolExecutor(
            5,                                  // 核心线程数
            10,                                 // 最大线程数
            60L,                                // 非核心线程存活时间
            TimeUnit.SECONDS,                   // 时间单位
            new LinkedBlockingQueue<>(100),     // 有界队列
            new NamedThreadFactory("worker"),   // 线程工厂
            new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略：调用者运行
        );
    }
    
    /**
     * 创建IO密集型线程池（线程数多）
     */
    public static ThreadPoolExecutor createIOThreadPool() {
        int processors = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(
            processors * 2,                     // 核心线程数 = CPU核心数 * 2
            processors * 4,                     // 最大线程数 = CPU核心数 * 4
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new NamedThreadFactory("io-worker"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
    
    /**
     * 创建CPU密集型线程池（线程数少）
     */
    public static ThreadPoolExecutor createCPUThreadPool() {
        int processors = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(
            processors,                         // 核心线程数 = CPU核心数
            processors + 1,                     // 最大线程数 = CPU核心数 + 1
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(500),
            new NamedThreadFactory("cpu-worker"),
            new ThreadPoolExecutor.AbortPolicy()  // 拒绝策略：抛异常
        );
    }
    
    /**
     * 监控线程池
     */
    public static void monitorThreadPool(ThreadPoolExecutor executor) {
        ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor();
        
        monitor.scheduleAtFixedRate(() -> {
            System.out.println("========== 线程池监控 ==========");
            System.out.println("核心线程数: " + executor.getCorePoolSize());
            System.out.println("最大线程数: " + executor.getMaximumPoolSize());
            System.out.println("当前线程数: " + executor.getPoolSize());
            System.out.println("活跃线程数: " + executor.getActiveCount());
            System.out.println("已完成任务数: " + executor.getCompletedTaskCount());
            System.out.println("总任务数: " + executor.getTaskCount());
            System.out.println("队列大小: " + executor.getQueue().size());
            System.out.println("==============================\n");
        }, 0, 2, TimeUnit.SECONDS);
    }
    
    public static void main(String[] args) throws InterruptedException {
        ThreadPoolExecutor executor = createStandardThreadPool();
        
        // 开启监控
        monitorThreadPool(executor);
        
        // 提交任务
        for (int i = 0; i < 20; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("任务 " + taskId + " 开始执行，线程: " + Thread.currentThread().getName());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("任务 " + taskId + " 执行完成");
            });
        }
        
        // 等待任务完成
        Thread.sleep(10000);
        
        // 优雅关闭
        executor.shutdown();
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
        
        System.out.println("线程池已关闭");
    }
}

