package com.interview.distributed.transaction;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分布式事务解决方案示例
 * 
 * 常见方案：
 * 1. 2PC - 两阶段提交
 * 2. 3PC - 三阶段提交
 * 3. TCC - Try-Confirm-Cancel
 * 4. Saga - 长事务
 * 5. 最终一致性
 */
public class DistributedTransactionDemo {
    
    /**
     * 2PC - 两阶段提交
     */
    public static class TwoPhaseCommit {
        
        /**
         * 事务协调者
         */
        static class Coordinator {
            private final java.util.List<Participant> participants = new java.util.ArrayList<>();
            
            public void addParticipant(Participant participant) {
                participants.add(participant);
            }
            
            /**
             * 执行两阶段提交
             */
            public boolean executeTransaction() {
                System.out.println("=== 2PC Transaction Start ===");
                
                // 阶段1：准备阶段
                System.out.println("Phase 1: Prepare");
                boolean allPrepared = true;
                
                for (Participant participant : participants) {
                    if (!participant.prepare()) {
                        allPrepared = false;
                        System.out.println("Participant " + participant.getName() + " prepare failed");
                        break;
                    }
                }
                
                // 阶段2：提交/回滚阶段
                if (allPrepared) {
                    System.out.println("Phase 2: Commit");
                    for (Participant participant : participants) {
                        participant.commit();
                    }
                    System.out.println("Transaction committed successfully");
                    return true;
                } else {
                    System.out.println("Phase 2: Rollback");
                    for (Participant participant : participants) {
                        participant.rollback();
                    }
                    System.out.println("Transaction rolled back");
                    return false;
                }
            }
        }
        
        /**
         * 事务参与者
         */
        static class Participant {
            private final String name;
            private boolean prepared = false;
            private boolean committed = false;
            
            public Participant(String name) {
                this.name = name;
            }
            
            public String getName() {
                return name;
            }
            
            /**
             * 准备阶段
             */
            public boolean prepare() {
                try {
                    System.out.println(name + " preparing...");
                    Thread.sleep(500); // 模拟准备时间
                    
                    // 模拟准备成功/失败
                    boolean success = Math.random() > 0.2; // 80%成功率
                    if (success) {
                        prepared = true;
                        System.out.println(name + " prepared successfully");
                    } else {
                        System.out.println(name + " prepare failed");
                    }
                    return success;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            
            /**
             * 提交
             */
            public void commit() {
                if (prepared) {
                    System.out.println(name + " committing...");
                    committed = true;
                    System.out.println(name + " committed");
                }
            }
            
            /**
             * 回滚
             */
            public void rollback() {
                if (prepared && !committed) {
                    System.out.println(name + " rolling back...");
                    prepared = false;
                    System.out.println(name + " rolled back");
                }
            }
        }
    }
    
    /**
     * TCC - Try-Confirm-Cancel
     */
    public static class TCCTransaction {
        
        /**
         * TCC服务接口
         */
        interface TCCService {
            boolean tryOperation();
            void confirmOperation();
            void cancelOperation();
        }
        
        /**
         * 订单服务
         */
        static class OrderService implements TCCService {
            private final String name;
            private boolean tried = false;
            private boolean confirmed = false;
            
            public OrderService(String name) {
                this.name = name;
            }
            
            @Override
            public boolean tryOperation() {
                System.out.println(name + " trying to create order...");
                try {
                    Thread.sleep(300);
                    boolean success = Math.random() > 0.1; // 90%成功率
                    if (success) {
                        tried = true;
                        System.out.println(name + " order created (tentative)");
                    } else {
                        System.out.println(name + " order creation failed");
                    }
                    return success;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            
            @Override
            public void confirmOperation() {
                if (tried && !confirmed) {
                    System.out.println(name + " confirming order...");
                    confirmed = true;
                    System.out.println(name + " order confirmed");
                }
            }
            
            @Override
            public void cancelOperation() {
                if (tried && !confirmed) {
                    System.out.println(name + " cancelling order...");
                    tried = false;
                    System.out.println(name + " order cancelled");
                }
            }
        }
        
        /**
         * 库存服务
         */
        static class InventoryService implements TCCService {
            private final String name;
            private boolean tried = false;
            private boolean confirmed = false;
            
            public InventoryService(String name) {
                this.name = name;
            }
            
            @Override
            public boolean tryOperation() {
                System.out.println(name + " trying to reserve inventory...");
                try {
                    Thread.sleep(400);
                    boolean success = Math.random() > 0.15; // 85%成功率
                    if (success) {
                        tried = true;
                        System.out.println(name + " inventory reserved (tentative)");
                    } else {
                        System.out.println(name + " inventory reservation failed");
                    }
                    return success;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            
            @Override
            public void confirmOperation() {
                if (tried && !confirmed) {
                    System.out.println(name + " confirming inventory reservation...");
                    confirmed = true;
                    System.out.println(name + " inventory reservation confirmed");
                }
            }
            
            @Override
            public void cancelOperation() {
                if (tried && !confirmed) {
                    System.out.println(name + " cancelling inventory reservation...");
                    tried = false;
                    System.out.println(name + " inventory reservation cancelled");
                }
            }
        }
        
        /**
         * TCC事务管理器
         */
        static class TCCTransactionManager {
            private final java.util.List<TCCService> services = new java.util.ArrayList<>();
            
            public void addService(TCCService service) {
                services.add(service);
            }
            
            /**
             * 执行TCC事务
             */
            public boolean executeTransaction() {
                System.out.println("=== TCC Transaction Start ===");
                
                // Try阶段
                System.out.println("Try Phase:");
                java.util.List<TCCService> triedServices = new java.util.ArrayList<>();
                
                for (TCCService service : services) {
                    if (service.tryOperation()) {
                        triedServices.add(service);
                    } else {
                        // Try失败，取消已尝试的服务
                        System.out.println("Try failed, cancelling previous services...");
                        for (TCCService triedService : triedServices) {
                            triedService.cancelOperation();
                        }
                        System.out.println("TCC Transaction failed");
                        return false;
                    }
                }
                
                // Confirm阶段
                System.out.println("Confirm Phase:");
                for (TCCService service : triedServices) {
                    service.confirmOperation();
                }
                
                System.out.println("TCC Transaction completed successfully");
                return true;
            }
        }
    }
    
    /**
     * Saga模式 - 长事务
     */
    public static class SagaTransaction {
        
        /**
         * Saga步骤
         */
        static class SagaStep {
            private final String name;
            private final Runnable action;
            private final Runnable compensation;
            private boolean executed = false;
            
            public SagaStep(String name, Runnable action, Runnable compensation) {
                this.name = name;
                this.action = action;
                this.compensation = compensation;
            }
            
            public boolean execute() {
                try {
                    System.out.println("Executing step: " + name);
                    action.run();
                    executed = true;
                    System.out.println("Step " + name + " executed successfully");
                    return true;
                } catch (Exception e) {
                    System.out.println("Step " + name + " failed: " + e.getMessage());
                    return false;
                }
            }
            
            public void compensate() {
                if (executed) {
                    try {
                        System.out.println("Compensating step: " + name);
                        compensation.run();
                        System.out.println("Step " + name + " compensated");
                    } catch (Exception e) {
                        System.out.println("Compensation failed for step " + name + ": " + e.getMessage());
                    }
                }
            }
        }
        
        /**
         * Saga事务管理器
         */
        static class SagaTransactionManager {
            private final java.util.List<SagaStep> steps = new java.util.ArrayList<>();
            
            public void addStep(SagaStep step) {
                steps.add(step);
            }
            
            /**
             * 执行Saga事务
             */
            public boolean executeTransaction() {
                System.out.println("=== Saga Transaction Start ===");
                
                java.util.List<SagaStep> executedSteps = new java.util.ArrayList<>();
                
                // 正向执行
                for (SagaStep step : steps) {
                    if (step.execute()) {
                        executedSteps.add(step);
                    } else {
                        // 执行失败，开始补偿
                        System.out.println("Execution failed, starting compensation...");
                        compensate(executedSteps);
                        System.out.println("Saga Transaction failed");
                        return false;
                    }
                }
                
                System.out.println("Saga Transaction completed successfully");
                return true;
            }
            
            /**
             * 补偿执行
             */
            private void compensate(java.util.List<SagaStep> executedSteps) {
                // 逆序补偿
                for (int i = executedSteps.size() - 1; i >= 0; i--) {
                    executedSteps.get(i).compensate();
                }
            }
        }
    }
    
    /**
     * 最终一致性示例
     */
    public static class EventualConsistency {
        
        /**
         * 事件
         */
        static class Event {
            private final String id;
            private final String type;
            private final Object data;
            private final long timestamp;
            
            public Event(String id, String type, Object data) {
                this.id = id;
                this.type = type;
                this.data = data;
                this.timestamp = System.currentTimeMillis();
            }
            
            // getters...
            public String getId() { return id; }
            public String getType() { return type; }
            public Object getData() { return data; }
            public long getTimestamp() { return timestamp; }
        }
        
        /**
         * 事件存储
         */
        static class EventStore {
            private final java.util.List<Event> events = new java.util.ArrayList<>();
            
            public void appendEvent(Event event) {
                events.add(event);
                System.out.println("Event appended: " + event.getType() + " - " + event.getId());
            }
            
            public java.util.List<Event> getEvents() {
                return new java.util.ArrayList<>(events);
            }
        }
        
        /**
         * 事件处理器
         */
        static class EventProcessor {
            private final String name;
            private final EventStore eventStore;
            
            public EventProcessor(String name, EventStore eventStore) {
                this.name = name;
                this.eventStore = eventStore;
            }
            
            public void processEvents() {
                java.util.List<Event> events = eventStore.getEvents();
                for (Event event : events) {
                    System.out.println(name + " processing event: " + event.getType());
                    // 模拟处理时间
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        /**
         * 最终一致性管理器
         */
        static class EventualConsistencyManager {
            private final EventStore eventStore;
            private final java.util.List<EventProcessor> processors = new java.util.ArrayList<>();
            
            public EventualConsistencyManager() {
                this.eventStore = new EventStore();
            }
            
            public void addProcessor(EventProcessor processor) {
                processors.add(processor);
            }
            
            public void publishEvent(Event event) {
                eventStore.appendEvent(event);
                
                // 异步处理事件
                for (EventProcessor processor : processors) {
                    new Thread(() -> processor.processEvents()).start();
                }
            }
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== 2PC Demo ==========");
        TwoPhaseCommit.Coordinator coordinator = new TwoPhaseCommit.Coordinator();
        coordinator.addParticipant(new TwoPhaseCommit.Participant("OrderService"));
        coordinator.addParticipant(new TwoPhaseCommit.Participant("InventoryService"));
        coordinator.addParticipant(new TwoPhaseCommit.Participant("PaymentService"));
        
        coordinator.executeTransaction();
        
        Thread.sleep(1000);
        
        System.out.println("\n========== TCC Demo ==========");
        TCCTransaction.TCCTransactionManager tccManager = new TCCTransaction.TCCTransactionManager();
        tccManager.addService(new TCCTransaction.OrderService("OrderService"));
        tccManager.addService(new TCCTransaction.InventoryService("InventoryService"));
        
        tccManager.executeTransaction();
        
        Thread.sleep(1000);
        
        System.out.println("\n========== Saga Demo ==========");
        SagaTransaction.SagaTransactionManager sagaManager = new SagaTransaction.SagaTransactionManager();
        
        sagaManager.addStep(new SagaTransaction.SagaStep(
            "CreateOrder",
            () -> System.out.println("Creating order..."),
            () -> System.out.println("Cancelling order...")
        ));
        
        sagaManager.addStep(new SagaTransaction.SagaStep(
            "ReserveInventory",
            () -> System.out.println("Reserving inventory..."),
            () -> System.out.println("Releasing inventory...")
        ));
        
        sagaManager.addStep(new SagaTransaction.SagaStep(
            "ProcessPayment",
            () -> System.out.println("Processing payment..."),
            () -> System.out.println("Refunding payment...")
        ));
        
        sagaManager.executeTransaction();
        
        Thread.sleep(1000);
        
        System.out.println("\n========== Eventual Consistency Demo ==========");
        EventualConsistency.EventualConsistencyManager ecManager = new EventualConsistency.EventualConsistencyManager();
        
        EventualConsistency.EventStore eventStore = new EventualConsistency.EventStore();
        ecManager.addProcessor(new EventualConsistency.EventProcessor("OrderProcessor", eventStore));
        ecManager.addProcessor(new EventualConsistency.EventProcessor("InventoryProcessor", eventStore));
        
        ecManager.publishEvent(new EventualConsistency.Event("1", "OrderCreated", "Order data"));
        ecManager.publishEvent(new EventualConsistency.Event("2", "InventoryReserved", "Inventory data"));
        
        Thread.sleep(2000);
    }
}
