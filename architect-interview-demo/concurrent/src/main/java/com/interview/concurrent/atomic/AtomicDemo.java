package com.interview.concurrent.atomic;

import java.util.concurrent.atomic.*;

/**
 * 原子类使用示例
 * 
 * 1. 基本类型: AtomicInteger, AtomicLong, AtomicBoolean
 * 2. 数组类型: AtomicIntegerArray
 * 3. 引用类型: AtomicReference, AtomicStampedReference
 * 4. 字段更新器: AtomicIntegerFieldUpdater
 * 5. 累加器: LongAdder (JDK 8+)
 */
public class AtomicDemo {
    
    /**
     * AtomicInteger示例 - CAS操作
     */
    static class AtomicIntegerDemo {
        private final AtomicInteger count = new AtomicInteger(0);
        
        public void increment() {
            count.incrementAndGet();  // count++
        }
        
        public void addTen() {
            count.addAndGet(10);
        }
        
        /**
         * CAS操作 - Compare and Swap
         */
        public boolean compareAndSet(int expect, int update) {
            return count.compareAndSet(expect, update);
        }
        
        public int get() {
            return count.get();
        }
    }
    
    /**
     * AtomicReference示例 - 引用类型的原子操作
     */
    static class AtomicReferenceDemo {
        static class User {
            private String name;
            private int age;
            
            public User(String name, int age) {
                this.name = name;
                this.age = age;
            }
            
            @Override
            public String toString() {
                return "User{name='" + name + "', age=" + age + '}';
            }
        }
        
        private final AtomicReference<User> userRef = new AtomicReference<>();
        
        public void setUser(User user) {
            userRef.set(user);
        }
        
        public boolean updateUser(User expect, User update) {
            return userRef.compareAndSet(expect, update);
        }
        
        public User getUser() {
            return userRef.get();
        }
    }
    
    /**
     * AtomicStampedReference - 解决ABA问题
     */
    static class AtomicStampedReferenceDemo {
        private final AtomicStampedReference<Integer> ref = 
            new AtomicStampedReference<>(100, 0);
        
        public boolean updateValue(Integer expect, Integer update) {
            int[] stampHolder = new int[1];
            Integer current = ref.get(stampHolder);
            int stamp = stampHolder[0];
            
            if (current.equals(expect)) {
                return ref.compareAndSet(expect, update, stamp, stamp + 1);
            }
            return false;
        }
        
        public Integer getValue() {
            return ref.getReference();
        }
    }
    
    /**
     * LongAdder示例 - 高性能累加器
     * 
     * 原理：分段累加，减少竞争
     * 适用场景：高并发计数
     */
    static class LongAdderDemo {
        private final LongAdder counter = new LongAdder();
        
        public void increment() {
            counter.increment();
        }
        
        public void add(long value) {
            counter.add(value);
        }
        
        public long sum() {
            return counter.sum();
        }
        
        public void reset() {
            counter.reset();
        }
    }
    
    /**
     * 性能对比：AtomicLong vs LongAdder
     */
    static class PerformanceTest {
        private static final int THREAD_COUNT = 10;
        private static final int ITERATIONS = 1_000_000;
        
        public static void testAtomicLong() throws InterruptedException {
            AtomicLong counter = new AtomicLong(0);
            long start = System.currentTimeMillis();
            
            Thread[] threads = new Thread[THREAD_COUNT];
            for (int i = 0; i < THREAD_COUNT; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < ITERATIONS; j++) {
                        counter.incrementAndGet();
                    }
                });
                threads[i].start();
            }
            
            for (Thread thread : threads) {
                thread.join();
            }
            
            long end = System.currentTimeMillis();
            System.out.println("AtomicLong: " + counter.get() + ", 耗时: " + (end - start) + "ms");
        }
        
        public static void testLongAdder() throws InterruptedException {
            LongAdder counter = new LongAdder();
            long start = System.currentTimeMillis();
            
            Thread[] threads = new Thread[THREAD_COUNT];
            for (int i = 0; i < THREAD_COUNT; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < ITERATIONS; j++) {
                        counter.increment();
                    }
                });
                threads[i].start();
            }
            
            for (Thread thread : threads) {
                thread.join();
            }
            
            long end = System.currentTimeMillis();
            System.out.println("LongAdder: " + counter.sum() + ", 耗时: " + (end - start) + "ms");
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        // 性能测试
        System.out.println("========== 性能对比 ==========");
        PerformanceTest.testAtomicLong();
        PerformanceTest.testLongAdder();
        
        // AtomicReference示例
        System.out.println("\n========== AtomicReference ==========");
        AtomicReferenceDemo.User user1 = new AtomicReferenceDemo.User("张三", 25);
        AtomicReferenceDemo.User user2 = new AtomicReferenceDemo.User("李四", 30);
        
        AtomicReferenceDemo refDemo = new AtomicReferenceDemo();
        refDemo.setUser(user1);
        System.out.println("当前用户: " + refDemo.getUser());
        
        boolean updated = refDemo.updateUser(user1, user2);
        System.out.println("更新结果: " + updated + ", 当前用户: " + refDemo.getUser());
    }
}

