package com.interview.patterns.facade;

/**
 * 外观模式 - 为复杂子系统提供统一接口
 * 
 * 适用场景：
 * - 简化复杂系统
 * - 提供统一入口
 * - 降低耦合度
 */
public class FacadePatternDemo {
    
    /**
     * CPU子系统
     */
    static class CPU {
        public void freeze() {
            System.out.println("CPU freeze");
        }
        
        public void jump(long position) {
            System.out.println("CPU jump to position: " + position);
        }
        
        public void execute() {
            System.out.println("CPU execute");
        }
    }
    
    /**
     * 内存子系统
     */
    static class Memory {
        public void load(long position, byte[] data) {
            System.out.println("Memory load data at position: " + position);
        }
    }
    
    /**
     * 硬盘子系统
     */
    static class HardDrive {
        public byte[] read(long lba, int size) {
            System.out.println("HardDrive read " + size + " bytes from LBA: " + lba);
            return new byte[size];
        }
    }
    
    /**
     * 计算机外观类
     */
    static class ComputerFacade {
        private CPU cpu;
        private Memory memory;
        private HardDrive hardDrive;
        
        public ComputerFacade() {
            this.cpu = new CPU();
            this.memory = new Memory();
            this.hardDrive = new HardDrive();
        }
        
        /**
         * 启动计算机
         */
        public void startComputer() {
            System.out.println("=== Starting Computer ===");
            
            // 1. CPU冻结
            cpu.freeze();
            
            // 2. 从硬盘读取引导程序
            byte[] bootData = hardDrive.read(0, 1024);
            
            // 3. 加载到内存
            memory.load(0, bootData);
            
            // 4. CPU跳转到引导程序
            cpu.jump(0);
            
            // 5. CPU开始执行
            cpu.execute();
            
            System.out.println("=== Computer Started ===");
        }
        
        /**
         * 关闭计算机
         */
        public void shutdownComputer() {
            System.out.println("=== Shutting Down Computer ===");
            // 关闭逻辑
            System.out.println("=== Computer Shutdown ===");
        }
    }
    
    /**
     * 订单处理外观
     */
    static class OrderFacade {
        private InventoryService inventoryService;
        private PaymentService paymentService;
        private ShippingService shippingService;
        private NotificationService notificationService;
        
        public OrderFacade() {
            this.inventoryService = new InventoryService();
            this.paymentService = new PaymentService();
            this.shippingService = new ShippingService();
            this.notificationService = new NotificationService();
        }
        
        /**
         * 处理订单
         */
        public boolean processOrder(String productId, int quantity, String paymentInfo) {
            System.out.println("=== Processing Order ===");
            
            try {
                // 1. 检查库存
                if (!inventoryService.checkStock(productId, quantity)) {
                    System.out.println("Insufficient stock");
                    return false;
                }
                
                // 2. 处理支付
                if (!paymentService.processPayment(paymentInfo)) {
                    System.out.println("Payment failed");
                    return false;
                }
                
                // 3. 扣减库存
                inventoryService.reduceStock(productId, quantity);
                
                // 4. 安排发货
                String trackingNumber = shippingService.ship(productId, quantity);
                
                // 5. 发送通知
                notificationService.sendOrderConfirmation(trackingNumber);
                
                System.out.println("=== Order Processed Successfully ===");
                return true;
                
            } catch (Exception e) {
                System.out.println("Order processing failed: " + e.getMessage());
                return false;
            }
        }
    }
    
    // 订单处理相关服务
    static class InventoryService {
        public boolean checkStock(String productId, int quantity) {
            System.out.println("Checking stock for product: " + productId + ", quantity: " + quantity);
            return true; // 模拟有库存
        }
        
        public void reduceStock(String productId, int quantity) {
            System.out.println("Reducing stock for product: " + productId + ", quantity: " + quantity);
        }
    }
    
    static class PaymentService {
        public boolean processPayment(String paymentInfo) {
            System.out.println("Processing payment: " + paymentInfo);
            return true; // 模拟支付成功
        }
    }
    
    static class ShippingService {
        public String ship(String productId, int quantity) {
            String trackingNumber = "TRK" + System.currentTimeMillis();
            System.out.println("Shipping product: " + productId + ", quantity: " + quantity + ", tracking: " + trackingNumber);
            return trackingNumber;
        }
    }
    
    static class NotificationService {
        public void sendOrderConfirmation(String trackingNumber) {
            System.out.println("Sending order confirmation with tracking: " + trackingNumber);
        }
    }
    
    public static void main(String[] args) {
        // 计算机启动示例
        ComputerFacade computer = new ComputerFacade();
        computer.startComputer();
        computer.shutdownComputer();
        
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // 订单处理示例
        OrderFacade orderFacade = new OrderFacade();
        boolean success = orderFacade.processOrder("PROD001", 2, "CARD123456");
        System.out.println("Order result: " + (success ? "Success" : "Failed"));
    }
}
