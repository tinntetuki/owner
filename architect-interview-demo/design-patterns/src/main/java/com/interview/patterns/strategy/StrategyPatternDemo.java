package com.interview.patterns.strategy;

import java.util.HashMap;
import java.util.Map;

/**
 * 策略模式 - 消除大量if-else
 * 
 * 适用场景：
 * - 支付方式选择
 * - 促销策略
 * - 排序算法选择
 * - 文件压缩方式
 */
public class StrategyPatternDemo {
    
    // ========== 策略接口 ==========
    interface PaymentStrategy {
        void pay(double amount);
    }
    
    // ========== 具体策略 ==========
    static class AliPayStrategy implements PaymentStrategy {
        @Override
        public void pay(double amount) {
            System.out.println("使用支付宝支付: " + amount + "元");
        }
    }
    
    static class WeChatPayStrategy implements PaymentStrategy {
        @Override
        public void pay(double amount) {
            System.out.println("使用微信支付: " + amount + "元");
        }
    }
    
    static class CreditCardStrategy implements PaymentStrategy {
        @Override
        public void pay(double amount) {
            System.out.println("使用信用卡支付: " + amount + "元");
        }
    }
    
    // ========== 策略上下文 ==========
    static class PaymentContext {
        private PaymentStrategy strategy;
        
        public void setStrategy(PaymentStrategy strategy) {
            this.strategy = strategy;
        }
        
        public void executePayment(double amount) {
            if (strategy == null) {
                throw new IllegalStateException("Payment strategy not set");
            }
            strategy.pay(amount);
        }
    }
    
    // ========== 策略工厂（推荐） ==========
    static class PaymentStrategyFactory {
        private static final Map<String, PaymentStrategy> STRATEGIES = new HashMap<>();
        
        static {
            STRATEGIES.put("alipay", new AliPayStrategy());
            STRATEGIES.put("wechat", new WeChatPayStrategy());
            STRATEGIES.put("card", new CreditCardStrategy());
        }
        
        public static PaymentStrategy getStrategy(String type) {
            PaymentStrategy strategy = STRATEGIES.get(type);
            if (strategy == null) {
                throw new IllegalArgumentException("Unknown payment type: " + type);
            }
            return strategy;
        }
    }
    
    // ========== 使用示例 ==========
    public static void main(String[] args) {
        // 方式1: 直接设置策略
        PaymentContext context = new PaymentContext();
        context.setStrategy(new AliPayStrategy());
        context.executePayment(100.0);
        
        // 方式2: 使用工厂
        PaymentStrategy strategy = PaymentStrategyFactory.getStrategy("wechat");
        context.setStrategy(strategy);
        context.executePayment(200.0);
    }
}

