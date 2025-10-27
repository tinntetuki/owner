package com.interview.performance.jvm;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JVM性能调优示例
 * 
 * 主要内容：
 * 1. 内存监控
 * 2. GC调优
 * 3. 内存泄漏检测
 * 4. 性能分析工具使用
 */
public class JVMPerformanceDemo {
    
    /**
     * 内存监控
     */
    public static class MemoryMonitor {
        private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        
        /**
         * 获取堆内存使用情况
         */
        public void printHeapMemoryUsage() {
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            
            System.out.println("=== Heap Memory Usage ===");
            System.out.println("Used: " + formatBytes(heapUsage.getUsed()));
            System.out.println("Committed: " + formatBytes(heapUsage.getCommitted()));
            System.out.println("Max: " + formatBytes(heapUsage.getMax()));
            System.out.println("Usage Ratio: " + String.format("%.2f%%", 
                (double) heapUsage.getUsed() / heapUsage.getCommitted() * 100));
        }
        
        /**
         * 获取非堆内存使用情况
         */
        public void printNonHeapMemoryUsage() {
            MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
            
            System.out.println("=== Non-Heap Memory Usage ===");
            System.out.println("Used: " + formatBytes(nonHeapUsage.getUsed()));
            System.out.println("Committed: " + formatBytes(nonHeapUsage.getCommitted()));
            System.out.println("Max: " + formatBytes(nonHeapUsage.getMax()));
        }
        
        /**
         * 监控内存使用趋势
         */
        public void monitorMemoryTrend(int durationSeconds) throws InterruptedException {
            System.out.println("=== Memory Trend Monitoring (" + durationSeconds + "s) ===");
            
            long startTime = System.currentTimeMillis();
            long endTime = startTime + durationSeconds * 1000;
            
            while (System.currentTimeMillis() < endTime) {
                MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
                long used = heapUsage.getUsed();
                long committed = heapUsage.getCommitted();
                
                System.out.printf("Time: %d, Used: %s, Committed: %s, Ratio: %.2f%%\n",
                    (System.currentTimeMillis() - startTime) / 1000,
                    formatBytes(used),
                    formatBytes(committed),
                    (double) used / committed * 100);
                
                Thread.sleep(1000);
            }
        }
        
        private String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
    
    /**
     * GC调优示例
     */
    public static class GCTuningDemo {
        
        /**
         * 创建大量对象触发GC
         */
        public void createObjectsAndTriggerGC() {
            System.out.println("=== Creating objects to trigger GC ===");
            
            List<byte[]> objects = new ArrayList<>();
            
            // 创建对象直到触发GC
            for (int i = 0; i < 1000; i++) {
                byte[] data = new byte[1024 * 1024]; // 1MB
                objects.add(data);
                
                if (i % 100 == 0) {
                    System.out.println("Created " + i + " objects");
                    printGCInfo();
                }
            }
            
            // 清理部分对象
            objects.subList(0, objects.size() / 2).clear();
            System.gc(); // 建议GC
            System.out.println("After cleanup and GC:");
            printGCInfo();
        }
        
        /**
         * 打印GC信息
         */
        private void printGCInfo() {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            
            System.out.printf("Heap: Used=%s, Committed=%s, Max=%s\n",
                formatBytes(heapUsage.getUsed()),
                formatBytes(heapUsage.getCommitted()),
                formatBytes(heapUsage.getMax()));
        }
        
        private String formatBytes(long bytes) {
            if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        }
    }
    
    /**
     * 内存泄漏检测
     */
    public static class MemoryLeakDetector {
        
        /**
         * 模拟内存泄漏 - 静态集合持有对象引用
         */
        static class MemoryLeakExample {
            private static final List<Object> LEAK_LIST = new ArrayList<>();
            
            public void createLeak() {
                // 创建大对象并添加到静态列表
                byte[] largeObject = new byte[1024 * 1024]; // 1MB
                LEAK_LIST.add(largeObject);
                
                System.out.println("Created leak object, list size: " + LEAK_LIST.size());
            }
            
            public void clearLeak() {
                LEAK_LIST.clear();
                System.out.println("Cleared leak list");
            }
        }
        
        /**
         * 模拟内存泄漏 - 监听器未移除
         */
        static class ListenerLeakExample {
            private final List<Runnable> listeners = new ArrayList<>();
            
            public void addListener(Runnable listener) {
                listeners.add(listener);
                System.out.println("Added listener, total: " + listeners.size());
            }
            
            public void removeListener(Runnable listener) {
                listeners.remove(listener);
                System.out.println("Removed listener, total: " + listeners.size());
            }
            
            public void notifyListeners() {
                for (Runnable listener : listeners) {
                    listener.run();
                }
            }
        }
        
        /**
         * 检测内存泄漏
         */
        public void detectMemoryLeak() throws InterruptedException {
            System.out.println("=== Memory Leak Detection ===");
            
            MemoryLeakExample leakExample = new MemoryLeakExample();
            
            // 创建泄漏
            for (int i = 0; i < 10; i++) {
                leakExample.createLeak();
                Thread.sleep(100);
            }
            
            // 强制GC
            System.gc();
            Thread.sleep(1000);
            
            // 检查内存
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            System.out.println("After leak creation: " + formatBytes(heapUsage.getUsed()));
            
            // 清理泄漏
            leakExample.clearLeak();
            System.gc();
            Thread.sleep(1000);
            
            heapUsage = memoryBean.getHeapMemoryUsage();
            System.out.println("After leak cleanup: " + formatBytes(heapUsage.getUsed()));
        }
        
        private String formatBytes(long bytes) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        }
    }
    
    /**
     * 性能分析工具使用示例
     */
    public static class PerformanceProfiler {
        
        /**
         * CPU密集型任务
         */
        public void cpuIntensiveTask() {
            System.out.println("=== CPU Intensive Task ===");
            
            long startTime = System.nanoTime();
            
            // 计算斐波那契数列
            long result = fibonacci(40);
            
            long endTime = System.nanoTime();
            long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
            
            System.out.println("Fibonacci(40) = " + result);
            System.out.println("Duration: " + duration + " ms");
        }
        
        private long fibonacci(int n) {
            if (n <= 1) return n;
            return fibonacci(n - 1) + fibonacci(n - 2);
        }
        
        /**
         * 内存密集型任务
         */
        public void memoryIntensiveTask() {
            System.out.println("=== Memory Intensive Task ===");
            
            long startTime = System.nanoTime();
            
            // 创建大量对象
            List<int[]> arrays = new ArrayList<>();
            for (int i = 0; i < 10000; i++) {
                int[] array = new int[1000];
                for (int j = 0; j < 1000; j++) {
                    array[j] = i * j;
                }
                arrays.add(array);
            }
            
            long endTime = System.nanoTime();
            long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
            
            System.out.println("Created " + arrays.size() + " arrays");
            System.out.println("Duration: " + duration + " ms");
        }
        
        /**
         * I/O密集型任务
         */
        public void ioIntensiveTask() {
            System.out.println("=== I/O Intensive Task ===");
            
            long startTime = System.nanoTime();
            
            // 模拟I/O操作
            for (int i = 0; i < 1000; i++) {
                try {
                    Thread.sleep(1); // 模拟I/O等待
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            long endTime = System.nanoTime();
            long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
            
            System.out.println("I/O operations completed");
            System.out.println("Duration: " + duration + " ms");
        }
        
        /**
         * 混合任务性能测试
         */
        public void mixedTaskPerformanceTest() {
            System.out.println("=== Mixed Task Performance Test ===");
            
            // CPU任务
            long cpuStart = System.nanoTime();
            fibonacci(35);
            long cpuDuration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - cpuStart);
            
            // 内存任务
            long memStart = System.nanoTime();
            List<String> strings = new ArrayList<>();
            for (int i = 0; i < 100000; i++) {
                strings.add("String-" + i);
            }
            long memDuration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - memStart);
            
            // I/O任务
            long ioStart = System.nanoTime();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            long ioDuration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - ioStart);
            
            System.out.println("CPU Task: " + cpuDuration + " ms");
            System.out.println("Memory Task: " + memDuration + " ms");
            System.out.println("I/O Task: " + ioDuration + " ms");
        }
    }
    
    /**
     * JVM参数建议
     */
    public static class JVMParameterSuggestions {
        
        /**
         * 打印JVM参数建议
         */
        public void printJVMSuggestions() {
            System.out.println("=== JVM Parameter Suggestions ===");
            
            System.out.println("\n1. 堆内存设置:");
            System.out.println("   -Xms2g -Xmx4g  # 初始堆2GB，最大堆4GB");
            System.out.println("   -XX:NewRatio=2  # 新生代与老年代比例1:2");
            System.out.println("   -XX:SurvivorRatio=8  # Eden与Survivor比例8:1:1");
            
            System.out.println("\n2. GC设置:");
            System.out.println("   -XX:+UseG1GC  # 使用G1垃圾收集器");
            System.out.println("   -XX:MaxGCPauseMillis=200  # 最大GC暂停时间200ms");
            System.out.println("   -XX:+UseConcMarkSweepGC  # 使用CMS收集器");
            
            System.out.println("\n3. 性能监控:");
            System.out.println("   -XX:+PrintGC  # 打印GC信息");
            System.out.println("   -XX:+PrintGCDetails  # 打印详细GC信息");
            System.out.println("   -XX:+PrintGCTimeStamps  # 打印GC时间戳");
            System.out.println("   -XX:+HeapDumpOnOutOfMemoryError  # OOM时生成堆转储");
            
            System.out.println("\n4. 优化设置:");
            System.out.println("   -XX:+UseCompressedOops  # 压缩指针");
            System.out.println("   -XX:+UseCompressedClassPointers  # 压缩类指针");
            System.out.println("   -XX:+TieredCompilation  # 分层编译");
            
            System.out.println("\n5. 调试设置:");
            System.out.println("   -XX:+PrintCompilation  # 打印编译信息");
            System.out.println("   -XX:+PrintInlining  # 打印内联信息");
            System.out.println("   -XX:+TraceClassLoading  # 跟踪类加载");
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== JVM Performance Demo ==========");
        
        // 内存监控
        MemoryMonitor monitor = new MemoryMonitor();
        monitor.printHeapMemoryUsage();
        monitor.printNonHeapMemoryUsage();
        
        // GC调优
        System.out.println("\n");
        GCTuningDemo gcDemo = new GCTuningDemo();
        gcDemo.createObjectsAndTriggerGC();
        
        // 内存泄漏检测
        System.out.println("\n");
        MemoryLeakDetector leakDetector = new MemoryLeakDetector();
        leakDetector.detectMemoryLeak();
        
        // 性能分析
        System.out.println("\n");
        PerformanceProfiler profiler = new PerformanceProfiler();
        profiler.cpuIntensiveTask();
        
        System.out.println("\n");
        profiler.memoryIntensiveTask();
        
        System.out.println("\n");
        profiler.ioIntensiveTask();
        
        System.out.println("\n");
        profiler.mixedTaskPerformanceTest();
        
        // JVM参数建议
        System.out.println("\n");
        JVMParameterSuggestions suggestions = new JVMParameterSuggestions();
        suggestions.printJVMSuggestions();
        
        // 内存趋势监控
        System.out.println("\n");
        monitor.monitorMemoryTrend(5);
    }
}
