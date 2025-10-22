package com.interview.architecture.ddd;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DDD（领域驱动设计）示例 - 电商订单
 * 
 * 核心概念：
 * 1. Entity（实体）- 有唯一标识
 * 2. Value Object（值对象）- 无标识，不可变
 * 3. Aggregate（聚合）- 一组相关对象的集合
 * 4. Domain Service（领域服务）- 跨实体的业务逻辑
 * 5. Repository（仓储）- 持久化
 */
public class DDDExample {
    
    // ========== 值对象（Value Object） ==========
    
    /**
     * 金额 - 值对象
     * 特点：不可变、无标识
     */
    static class Money {
        private final BigDecimal amount;
        private final String currency;
        
        public Money(BigDecimal amount, String currency) {
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("金额不能为负数");
            }
            this.amount = amount;
            this.currency = currency;
        }
        
        public Money add(Money other) {
            if (!this.currency.equals(other.currency)) {
                throw new IllegalArgumentException("货币类型不匹配");
            }
            return new Money(this.amount.add(other.amount), this.currency);
        }
        
        public Money multiply(int quantity) {
            return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)), this.currency);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Money)) return false;
            Money other = (Money) obj;
            return amount.equals(other.amount) && currency.equals(other.currency);
        }
        
        @Override
        public String toString() {
            return currency + " " + amount;
        }
    }
    
    /**
     * 地址 - 值对象
     */
    static class Address {
        private final String province;
        private final String city;
        private final String detail;
        
        public Address(String province, String city, String detail) {
            this.province = province;
            this.city = city;
            this.detail = detail;
        }
        
        @Override
        public String toString() {
            return province + " " + city + " " + detail;
        }
    }
    
    // ========== 实体（Entity） ==========
    
    /**
     * 订单项 - 实体
     */
    static class OrderItem {
        private final Long id;
        private final Long productId;
        private final String productName;
        private final Money price;
        private final int quantity;
        
        public OrderItem(Long id, Long productId, String productName, Money price, int quantity) {
            this.id = id;
            this.productId = productId;
            this.productName = productName;
            this.price = price;
            this.quantity = quantity;
        }
        
        public Money getTotalPrice() {
            return price.multiply(quantity);
        }
        
        @Override
        public String toString() {
            return productName + " x " + quantity + " = " + getTotalPrice();
        }
    }
    
    // ========== 聚合根（Aggregate Root） ==========
    
    /**
     * 订单 - 聚合根
     * 
     * 聚合根职责：
     * 1. 保证聚合内的业务规则
     * 2. 对外提供统一接口
     * 3. 控制聚合内对象的生命周期
     */
    static class Order {
        private final Long id;
        private final Long userId;
        private final List<OrderItem> items;
        private OrderStatus status;
        private Address shippingAddress;
        
        public Order(Long id, Long userId, Address shippingAddress) {
            this.id = id;
            this.userId = userId;
            this.items = new ArrayList<>();
            this.status = OrderStatus.CREATED;
            this.shippingAddress = shippingAddress;
        }
        
        /**
         * 添加订单项
         */
        public void addItem(OrderItem item) {
            if (status != OrderStatus.CREATED) {
                throw new IllegalStateException("订单已提交，不能修改");
            }
            items.add(item);
        }
        
        /**
         * 提交订单
         */
        public void submit() {
            if (items.isEmpty()) {
                throw new IllegalStateException("订单不能为空");
            }
            if (status != OrderStatus.CREATED) {
                throw new IllegalStateException("订单状态不正确");
            }
            status = OrderStatus.SUBMITTED;
        }
        
        /**
         * 支付订单
         */
        public void pay() {
            if (status != OrderStatus.SUBMITTED) {
                throw new IllegalStateException("订单未提交");
            }
            status = OrderStatus.PAID;
        }
        
        /**
         * 计算总价
         */
        public Money getTotalAmount() {
            return items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(Money::add)
                .orElse(new Money(BigDecimal.ZERO, "CNY"));
        }
        
        public OrderStatus getStatus() {
            return status;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("订单ID: ").append(id).append("\n");
            sb.append("用户ID: ").append(userId).append("\n");
            sb.append("配送地址: ").append(shippingAddress).append("\n");
            sb.append("订单项:\n");
            items.forEach(item -> sb.append("  - ").append(item).append("\n"));
            sb.append("总价: ").append(getTotalAmount()).append("\n");
            sb.append("状态: ").append(status);
            return sb.toString();
        }
    }
    
    /**
     * 订单状态 - 枚举
     */
    enum OrderStatus {
        CREATED,    // 已创建
        SUBMITTED,  // 已提交
        PAID,       // 已支付
        SHIPPED,    // 已发货
        COMPLETED   // 已完成
    }
    
    // ========== 领域服务（Domain Service） ==========
    
    /**
     * 订单定价服务
     * 
     * 当业务逻辑跨越多个实体时，使用领域服务
     */
    static class OrderPricingService {
        
        /**
         * 计算折扣
         */
        public Money calculateDiscount(Order order, User user) {
            Money totalAmount = order.getTotalAmount();
            
            // VIP用户9折
            if (user.isVIP()) {
                BigDecimal discount = totalAmount.amount.multiply(BigDecimal.valueOf(0.1));
                return new Money(discount, totalAmount.currency);
            }
            
            return new Money(BigDecimal.ZERO, totalAmount.currency);
        }
    }
    
    static class User {
        private final Long id;
        private final String name;
        private final boolean vip;
        
        public User(Long id, String name, boolean vip) {
            this.id = id;
            this.name = name;
            this.vip = vip;
        }
        
        public boolean isVIP() {
            return vip;
        }
    }
    
    // ========== 仓储接口（Repository） ==========
    
    /**
     * 订单仓储
     */
    interface OrderRepository {
        void save(Order order);
        Order findById(Long id);
        List<Order> findByUserId(Long userId);
    }
    
    // ========== 应用服务（Application Service） ==========
    
    /**
     * 订单应用服务
     * 
     * 职责：
     * 1. 编排领域对象
     * 2. 事务管理
     * 3. 权限检查
     */
    static class OrderApplicationService {
        private final OrderRepository orderRepository;
        private final OrderPricingService pricingService;
        
        public OrderApplicationService(OrderRepository orderRepository, OrderPricingService pricingService) {
            this.orderRepository = orderRepository;
            this.pricingService = pricingService;
        }
        
        /**
         * 创建订单
         */
        public Long createOrder(Long userId, Address address, List<OrderItem> items) {
            // 1. 创建订单聚合
            Order order = new Order(System.currentTimeMillis(), userId, address);
            
            // 2. 添加订单项
            items.forEach(order::addItem);
            
            // 3. 提交订单
            order.submit();
            
            // 4. 保存订单
            orderRepository.save(order);
            
            return order.id;
        }
        
        /**
         * 支付订单
         */
        public void payOrder(Long orderId, User user) {
            // 1. 查询订单
            Order order = orderRepository.findById(orderId);
            
            // 2. 计算折扣
            Money discount = pricingService.calculateDiscount(order, user);
            System.out.println("折扣金额: " + discount);
            
            // 3. 支付
            order.pay();
            
            // 4. 保存
            orderRepository.save(order);
        }
    }
    
    // ========== 测试 ==========
    
    public static void main(String[] args) {
        // 创建订单项
        Money price1 = new Money(BigDecimal.valueOf(99.99), "CNY");
        Money price2 = new Money(BigDecimal.valueOf(199.00), "CNY");
        
        OrderItem item1 = new OrderItem(1L, 1001L, "《Java编程思想》", price1, 2);
        OrderItem item2 = new OrderItem(2L, 1002L, "《设计模式》", price2, 1);
        
        // 创建订单
        Address address = new Address("北京市", "朝阳区", "xx路xx号");
        Order order = new Order(1L, 10001L, address);
        
        order.addItem(item1);
        order.addItem(item2);
        order.submit();
        
        System.out.println("========== 订单信息 ==========");
        System.out.println(order);
        
        // 支付订单
        System.out.println("\n========== 支付订单 ==========");
        order.pay();
        System.out.println("订单状态: " + order.getStatus());
    }
}

