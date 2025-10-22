package com.interview.concurrent.lock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;

/**
 * 锁的使用示例
 * 
 * 1. ReentrantLock - 可重入锁
 * 2. ReadWriteLock - 读写锁
 * 3. StampedLock - 改进的读写锁（JDK 8+）
 */
public class LockDemo {
    
    /**
     * ReentrantLock示例 - 可重入、可中断、可超时
     */
    static class ReentrantLockDemo {
        private final Lock lock = new ReentrantLock();
        private int count = 0;
        
        public void increment() {
            lock.lock();
            try {
                count++;
                System.out.println(Thread.currentThread().getName() + " increment: " + count);
            } finally {
                lock.unlock();
            }
        }
        
        /**
         * 尝试获取锁（非阻塞）
         */
        public boolean tryIncrement() {
            if (lock.tryLock()) {
                try {
                    count++;
                    return true;
                } finally {
                    lock.unlock();
                }
            }
            return false;
        }
        
        /**
         * 可中断的锁获取
         */
        public void incrementInterruptibly() throws InterruptedException {
            lock.lockInterruptibly();
            try {
                count++;
            } finally {
                lock.unlock();
            }
        }
    }
    
    /**
     * ReadWriteLock示例 - 读多写少场景
     */
    static class ReadWriteLockDemo {
        private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
        private final Lock readLock = rwLock.readLock();
        private final Lock writeLock = rwLock.writeLock();
        private int value = 0;
        
        /**
         * 读操作 - 共享锁
         */
        public int read() {
            readLock.lock();
            try {
                System.out.println(Thread.currentThread().getName() + " 读取: " + value);
                Thread.sleep(100);  // 模拟读操作
                return value;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return -1;
            } finally {
                readLock.unlock();
            }
        }
        
        /**
         * 写操作 - 排他锁
         */
        public void write(int newValue) {
            writeLock.lock();
            try {
                System.out.println(Thread.currentThread().getName() + " 写入: " + newValue);
                value = newValue;
                Thread.sleep(200);  // 模拟写操作
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                writeLock.unlock();
            }
        }
    }
    
    /**
     * StampedLock示例 - 乐观读
     */
    static class StampedLockDemo {
        private final StampedLock lock = new StampedLock();
        private double x, y;
        
        /**
         * 写操作
         */
        public void move(double deltaX, double deltaY) {
            long stamp = lock.writeLock();
            try {
                x += deltaX;
                y += deltaY;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        /**
         * 乐观读
         */
        public double distanceFromOrigin() {
            long stamp = lock.tryOptimisticRead();  // 乐观读
            double currentX = x;
            double currentY = y;
            
            if (!lock.validate(stamp)) {  // 检查是否有写操作
                stamp = lock.readLock();  // 升级为悲观读
                try {
                    currentX = x;
                    currentY = y;
                } finally {
                    lock.unlockRead(stamp);
                }
            }
            
            return Math.sqrt(currentX * currentX + currentY * currentY);
        }
        
        /**
         * 悲观读
         */
        public double distanceFromOriginPessimistic() {
            long stamp = lock.readLock();
            try {
                return Math.sqrt(x * x + y * y);
            } finally {
                lock.unlockRead(stamp);
            }
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        // 测试ReadWriteLock
        ReadWriteLockDemo rwDemo = new ReadWriteLockDemo();
        
        // 启动多个读线程
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                for (int j = 0; j < 3; j++) {
                    rwDemo.read();
                }
            }, "Reader-" + i).start();
        }
        
        // 启动写线程
        new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                rwDemo.write(i);
            }
        }, "Writer").start();
    }
}

