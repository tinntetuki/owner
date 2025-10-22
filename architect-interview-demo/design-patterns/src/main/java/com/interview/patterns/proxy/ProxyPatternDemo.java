package com.interview.patterns.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 代理模式 - 静态代理、动态代理、CGLIB代理
 * 
 * 适用场景：
 * - AOP实现
 * - 权限控制
 * - 日志记录
 * - 性能监控
 * - 远程调用（RPC）
 */
public class ProxyPatternDemo {
    
    // ========== 接口 ==========
    interface UserService {
        void save(String username);
        String query(Long id);
    }
    
    // ========== 真实对象 ==========
    static class UserServiceImpl implements UserService {
        @Override
        public void save(String username) {
            System.out.println("保存用户: " + username);
        }
        
        @Override
        public String query(Long id) {
            System.out.println("查询用户: " + id);
            return "User" + id;
        }
    }
    
    // ========== 1. 静态代理 ==========
    static class UserServiceStaticProxy implements UserService {
        private final UserService target;
        
        public UserServiceStaticProxy(UserService target) {
            this.target = target;
        }
        
        @Override
        public void save(String username) {
            System.out.println("[静态代理] 开始事务");
            long start = System.currentTimeMillis();
            
            target.save(username);
            
            long end = System.currentTimeMillis();
            System.out.println("[静态代理] 提交事务，耗时: " + (end - start) + "ms");
        }
        
        @Override
        public String query(Long id) {
            System.out.println("[静态代理] 查询前置处理");
            String result = target.query(id);
            System.out.println("[静态代理] 查询后置处理");
            return result;
        }
    }
    
    // ========== 2. JDK动态代理 ==========
    static class DynamicProxyHandler implements InvocationHandler {
        private final Object target;
        
        public DynamicProxyHandler(Object target) {
            this.target = target;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            System.out.println("[动态代理] 方法调用前: " + method.getName());
            long start = System.currentTimeMillis();
            
            Object result = method.invoke(target, args);
            
            long end = System.currentTimeMillis();
            System.out.println("[动态代理] 方法调用后, 耗时: " + (end - start) + "ms");
            
            return result;
        }
        
        @SuppressWarnings("unchecked")
        public static <T> T createProxy(T target) {
            return (T) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                target.getClass().getInterfaces(),
                new DynamicProxyHandler(target)
            );
        }
    }
    
    public static void main(String[] args) {
        UserService userService = new UserServiceImpl();
        
        // 1. 静态代理
        System.out.println("========== 静态代理 ==========");
        UserService staticProxy = new UserServiceStaticProxy(userService);
        staticProxy.save("张三");
        staticProxy.query(1L);
        
        // 2. 动态代理
        System.out.println("\n========== 动态代理 ==========");
        UserService dynamicProxy = DynamicProxyHandler.createProxy(userService);
        dynamicProxy.save("李四");
        dynamicProxy.query(2L);
    }
}

