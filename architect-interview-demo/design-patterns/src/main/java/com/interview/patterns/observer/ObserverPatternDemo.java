package com.interview.patterns.observer;

import java.util.ArrayList;
import java.util.List;

/**
 * 观察者模式（发布-订阅模式）
 * 
 * 适用场景：
 * - 消息通知系统
 * - 事件驱动系统
 * - MVC架构中的View更新
 * - 分布式事件总线
 */
public class ObserverPatternDemo {
    
    // ========== 观察者接口 ==========
    interface Observer {
        void update(String message);
    }
    
    // ========== 主题接口 ==========
    interface Subject {
        void attach(Observer observer);
        void detach(Observer observer);
        void notifyObservers(String message);
    }
    
    // ========== 具体主题 ==========
    static class EventPublisher implements Subject {
        private final List<Observer> observers = new ArrayList<>();
        
        @Override
        public void attach(Observer observer) {
            observers.add(observer);
        }
        
        @Override
        public void detach(Observer observer) {
            observers.remove(observer);
        }
        
        @Override
        public void notifyObservers(String message) {
            for (Observer observer : observers) {
                observer.update(message);
            }
        }
        
        public void publishEvent(String event) {
            System.out.println("发布事件: " + event);
            notifyObservers(event);
        }
    }
    
    // ========== 具体观察者 ==========
    static class EmailNotifier implements Observer {
        private final String email;
        
        public EmailNotifier(String email) {
            this.email = email;
        }
        
        @Override
        public void update(String message) {
            System.out.println("发送邮件到 " + email + ": " + message);
        }
    }
    
    static class SMSNotifier implements Observer {
        private final String phone;
        
        public SMSNotifier(String phone) {
            this.phone = phone;
        }
        
        @Override
        public void update(String message) {
            System.out.println("发送短信到 " + phone + ": " + message);
        }
    }
    
    static class WeChatNotifier implements Observer {
        private final String wechatId;
        
        public WeChatNotifier(String wechatId) {
            this.wechatId = wechatId;
        }
        
        @Override
        public void update(String message) {
            System.out.println("发送微信到 " + wechatId + ": " + message);
        }
    }
    
    // ========== 使用示例 ==========
    public static void main(String[] args) {
        EventPublisher publisher = new EventPublisher();
        
        // 注册观察者
        Observer emailNotifier = new EmailNotifier("user@example.com");
        Observer smsNotifier = new SMSNotifier("13800138000");
        Observer wechatNotifier = new WeChatNotifier("wx_user123");
        
        publisher.attach(emailNotifier);
        publisher.attach(smsNotifier);
        publisher.attach(wechatNotifier);
        
        // 发布事件
        publisher.publishEvent("您的订单已发货");
        
        System.out.println("\n取消短信通知后:");
        publisher.detach(smsNotifier);
        publisher.publishEvent("您的订单已签收");
    }
}

