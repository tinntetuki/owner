package com.interview.patterns.singleton;

/**
 * 单例模式 - 多种实现方式
 * 
 * 适用场景：
 * - 数据库连接池
 * - 线程池
 * - 配置管理器
 * - 日志对象
 */
public class SingletonDemo {
    
    /**
     * 1. 饿汉式 - 线程安全，但可能浪费内存
     */
    public static class EagerSingleton {
        private static final EagerSingleton INSTANCE = new EagerSingleton();
        
        private EagerSingleton() {}
        
        public static EagerSingleton getInstance() {
            return INSTANCE;
        }
    }
    
    /**
     * 2. 懒汉式 - 双重检查锁（推荐）
     */
    public static class LazySingleton {
        private static volatile LazySingleton instance;
        
        private LazySingleton() {}
        
        public static LazySingleton getInstance() {
            if (instance == null) {
                synchronized (LazySingleton.class) {
                    if (instance == null) {
                        instance = new LazySingleton();
                    }
                }
            }
            return instance;
        }
    }
    
    /**
     * 3. 静态内部类 - 推荐，线程安全且延迟加载
     */
    public static class StaticInnerSingleton {
        private StaticInnerSingleton() {}
        
        private static class SingletonHolder {
            private static final StaticInnerSingleton INSTANCE = new StaticInnerSingleton();
        }
        
        public static StaticInnerSingleton getInstance() {
            return SingletonHolder.INSTANCE;
        }
    }
    
    /**
     * 4. 枚举单例 - 最简单，防止反序列化
     */
    public enum EnumSingleton {
        INSTANCE;
        
        public void doSomething() {
            System.out.println("Enum Singleton");
        }
    }
}

