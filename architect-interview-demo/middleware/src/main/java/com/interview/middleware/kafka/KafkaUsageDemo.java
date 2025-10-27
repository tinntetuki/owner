package com.interview.middleware.kafka;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Kafka使用示例
 * 
 * 核心功能：
 * 1. 生产者发送消息
 * 2. 消费者消费消息
 * 3. 分区策略
 * 4. 消息确认机制
 * 5. 批量处理
 */
public class KafkaUsageDemo {
    
    /**
     * 消息实体
     */
    static class Message {
        private String key;
        private String value;
        private String topic;
        private int partition;
        private long offset;
        private long timestamp;
        
        public Message(String key, String value, String topic) {
            this.key = key;
            this.value = value;
            this.topic = topic;
            this.timestamp = System.currentTimeMillis();
        }
        
        // getters and setters
        public String getKey() { return key; }
        public String getValue() { return value; }
        public String getTopic() { return topic; }
        public int getPartition() { return partition; }
        public void setPartition(int partition) { this.partition = partition; }
        public long getOffset() { return offset; }
        public void setOffset(long offset) { this.offset = offset; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * Kafka生产者
     */
    static class KafkaProducer {
        private final String producerId;
        private final Map<String, AtomicInteger> topicOffsets = new ConcurrentHashMap<>();
        private final Map<String, List<Message>> topicMessages = new ConcurrentHashMap<>();
        
        public KafkaProducer(String producerId) {
            this.producerId = producerId;
        }
        
        /**
         * 发送消息
         */
        public CompletableFuture<Message> send(String topic, String key, String value) {
            return CompletableFuture.supplyAsync(() -> {
                Message message = new Message(key, value, topic);
                
                // 模拟分区分配
                int partition = calculatePartition(key, topic);
                message.setPartition(partition);
                
                // 模拟offset分配
                AtomicInteger offset = topicOffsets.computeIfAbsent(topic, k -> new AtomicInteger(0));
                message.setOffset(offset.getAndIncrement());
                
                // 存储消息
                topicMessages.computeIfAbsent(topic, k -> new ArrayList<>()).add(message);
                
                System.out.println("Producer " + producerId + " sent message to topic " + topic + 
                    ", partition " + partition + ", offset " + message.getOffset() + ": " + value);
                
                return message;
            });
        }
        
        /**
         * 批量发送消息
         */
        public CompletableFuture<List<Message>> sendBatch(String topic, List<Map<String, String>> messages) {
            return CompletableFuture.supplyAsync(() -> {
                List<Message> sentMessages = new ArrayList<>();
                
                for (Map<String, String> msg : messages) {
                    String key = msg.get("key");
                    String value = msg.get("value");
                    
                    Message message = new Message(key, value, topic);
                    int partition = calculatePartition(key, topic);
                    message.setPartition(partition);
                    
                    AtomicInteger offset = topicOffsets.computeIfAbsent(topic, k -> new AtomicInteger(0));
                    message.setOffset(offset.getAndIncrement());
                    
                    topicMessages.computeIfAbsent(topic, k -> new ArrayList<>()).add(message);
                    sentMessages.add(message);
                }
                
                System.out.println("Producer " + producerId + " sent batch of " + messages.size() + 
                    " messages to topic " + topic);
                
                return sentMessages;
            });
        }
        
        /**
         * 计算分区
         */
        private int calculatePartition(String key, String topic) {
            if (key == null) {
                return 0; // 默认分区
            }
            return Math.abs(key.hashCode()) % 3; // 3个分区
        }
        
        /**
         * 获取主题消息
         */
        public List<Message> getTopicMessages(String topic) {
            return topicMessages.getOrDefault(topic, new ArrayList<>());
        }
    }
    
    /**
     * Kafka消费者
     */
    static class KafkaConsumer {
        private final String consumerId;
        private final String groupId;
        private final Map<String, Long> topicOffsets = new ConcurrentHashMap<>();
        private final Map<String, List<Message>> consumedMessages = new ConcurrentHashMap<>();
        
        public KafkaConsumer(String consumerId, String groupId) {
            this.consumerId = consumerId;
            this.groupId = groupId;
        }
        
        /**
         * 订阅主题
         */
        public void subscribe(String topic, KafkaProducer producer) {
            System.out.println("Consumer " + consumerId + " subscribed to topic " + topic);
            
            // 模拟消费消息
            List<Message> messages = producer.getTopicMessages(topic);
            long lastOffset = topicOffsets.getOrDefault(topic, 0L);
            
            for (Message message : messages) {
                if (message.getOffset() >= lastOffset) {
                    consumeMessage(message);
                    topicOffsets.put(topic, message.getOffset() + 1);
                }
            }
        }
        
        /**
         * 消费消息
         */
        private void consumeMessage(Message message) {
            consumedMessages.computeIfAbsent(message.getTopic(), k -> new ArrayList<>()).add(message);
            
            System.out.println("Consumer " + consumerId + " consumed message from topic " + 
                message.getTopic() + ", partition " + message.getPartition() + 
                ", offset " + message.getOffset() + ": " + message.getValue());
        }
        
        /**
         * 批量消费
         */
        public void consumeBatch(String topic, int batchSize) {
            List<Message> messages = consumedMessages.getOrDefault(topic, new ArrayList<>());
            
            System.out.println("Consumer " + consumerId + " processing batch of " + 
                Math.min(batchSize, messages.size()) + " messages from topic " + topic);
            
            // 模拟批量处理
            for (int i = 0; i < Math.min(batchSize, messages.size()); i++) {
                Message message = messages.get(i);
                processMessage(message);
            }
        }
        
        /**
         * 处理消息
         */
        private void processMessage(Message message) {
            try {
                Thread.sleep(100); // 模拟处理时间
                System.out.println("Processed message: " + message.getValue());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        /**
         * 获取消费的消息
         */
        public List<Message> getConsumedMessages(String topic) {
            return consumedMessages.getOrDefault(topic, new ArrayList<>());
        }
        
        /**
         * 获取当前offset
         */
        public long getCurrentOffset(String topic) {
            return topicOffsets.getOrDefault(topic, 0L);
        }
    }
    
    /**
     * 分区策略示例
     */
    static class PartitionStrategy {
        
        /**
         * 轮询分区策略
         */
        public static int roundRobinPartition(String topic, int partitionCount, long messageCount) {
            return (int) (messageCount % partitionCount);
        }
        
        /**
         * 基于key的分区策略
         */
        public static int keyBasedPartition(String key, int partitionCount) {
            if (key == null) {
                return 0;
            }
            return Math.abs(key.hashCode()) % partitionCount;
        }
        
        /**
         * 随机分区策略
         */
        public static int randomPartition(int partitionCount) {
            return new Random().nextInt(partitionCount);
        }
        
        /**
         * 自定义分区策略
         */
        public static int customPartition(String key, String topic, int partitionCount) {
            // 根据业务逻辑自定义分区
            if (topic.equals("user-events")) {
                return keyBasedPartition(key, partitionCount);
            } else if (topic.equals("system-logs")) {
                return roundRobinPartition(topic, partitionCount, System.currentTimeMillis());
            } else {
                return randomPartition(partitionCount);
            }
        }
    }
    
    /**
     * 消息确认机制
     */
    static class MessageAcknowledgment {
        private final Map<String, Set<Long>> acknowledgedMessages = new ConcurrentHashMap<>();
        
        /**
         * 确认消息
         */
        public void acknowledge(String topic, long offset) {
            acknowledgedMessages.computeIfAbsent(topic, k -> ConcurrentHashMap.newKeySet()).add(offset);
            System.out.println("Acknowledged message: topic=" + topic + ", offset=" + offset);
        }
        
        /**
         * 批量确认消息
         */
        public void acknowledgeBatch(String topic, List<Long> offsets) {
            Set<Long> topicAcks = acknowledgedMessages.computeIfAbsent(topic, k -> ConcurrentHashMap.newKeySet());
            topicAcks.addAll(offsets);
            System.out.println("Acknowledged batch: topic=" + topic + ", count=" + offsets.size());
        }
        
        /**
         * 检查消息是否已确认
         */
        public boolean isAcknowledged(String topic, long offset) {
            return acknowledgedMessages.getOrDefault(topic, Collections.emptySet()).contains(offset);
        }
        
        /**
         * 获取已确认的消息数量
         */
        public int getAcknowledgedCount(String topic) {
            return acknowledgedMessages.getOrDefault(topic, Collections.emptySet()).size();
        }
    }
    
    /**
     * 流处理示例
     */
    static class StreamProcessor {
        private final String processorId;
        private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();
        
        public StreamProcessor(String processorId) {
            this.processorId = processorId;
        }
        
        /**
         * 处理消息流
         */
        public void processStream(String topic, List<Message> messages) {
            System.out.println("StreamProcessor " + processorId + " processing " + messages.size() + " messages");
            
            for (Message message : messages) {
                processMessage(message);
            }
        }
        
        /**
         * 处理单个消息
         */
        private void processMessage(Message message) {
            String key = message.getKey();
            String value = message.getValue();
            
            // 模拟不同的处理逻辑
            if (value.contains("error")) {
                handleError(key, value);
            } else if (value.contains("warning")) {
                handleWarning(key, value);
            } else {
                handleNormal(key, value);
            }
        }
        
        private void handleError(String key, String value) {
            counters.computeIfAbsent("error", k -> new AtomicInteger(0)).incrementAndGet();
            System.out.println("ERROR: " + key + " - " + value);
        }
        
        private void handleWarning(String key, String value) {
            counters.computeIfAbsent("warning", k -> new AtomicInteger(0)).incrementAndGet();
            System.out.println("WARNING: " + key + " - " + value);
        }
        
        private void handleNormal(String key, String value) {
            counters.computeIfAbsent("normal", k -> new AtomicInteger(0)).incrementAndGet();
            System.out.println("NORMAL: " + key + " - " + value);
        }
        
        /**
         * 获取统计信息
         */
        public Map<String, Integer> getStatistics() {
            Map<String, Integer> stats = new HashMap<>();
            for (Map.Entry<String, AtomicInteger> entry : counters.entrySet()) {
                stats.put(entry.getKey(), entry.getValue().get());
            }
            return stats;
        }
    }
    
    /**
     * Kafka使用演示
     */
    public static class KafkaDemo {
        public void runDemo() throws InterruptedException {
            System.out.println("=== Kafka Usage Demo ===");
            
            // 创建生产者和消费者
            KafkaProducer producer = new KafkaProducer("producer-1");
            KafkaConsumer consumer1 = new KafkaConsumer("consumer-1", "group-1");
            KafkaConsumer consumer2 = new KafkaConsumer("consumer-2", "group-1");
            
            // 发送消息
            System.out.println("\n=== Sending Messages ===");
            CompletableFuture<Message> future1 = producer.send("user-events", "user1", "User logged in");
            CompletableFuture<Message> future2 = producer.send("user-events", "user2", "User registered");
            CompletableFuture<Message> future3 = producer.send("system-logs", "system", "System started");
            
            // 批量发送
            List<Map<String, String>> batchMessages = Arrays.asList(
                Map.of("key", "user3", "value", "User purchased item"),
                Map.of("key", "user4", "value", "User updated profile"),
                Map.of("key", "user5", "value", "User logged out")
            );
            
            CompletableFuture<List<Message>> batchFuture = producer.sendBatch("user-events", batchMessages);
            
            // 等待消息发送完成
            CompletableFuture.allOf(future1, future2, future3, batchFuture).join();
            
            // 消费者订阅
            System.out.println("\n=== Consumer Subscription ===");
            consumer1.subscribe("user-events", producer);
            consumer2.subscribe("system-logs", producer);
            
            // 批量消费
            System.out.println("\n=== Batch Consumption ===");
            consumer1.consumeBatch("user-events", 3);
            
            // 分区策略演示
            System.out.println("\n=== Partition Strategy Demo ===");
            String topic = "test-topic";
            int partitionCount = 3;
            
            for (int i = 0; i < 10; i++) {
                String key = "key" + i;
                int partition = PartitionStrategy.keyBasedPartition(key, partitionCount);
                System.out.println("Key: " + key + " -> Partition: " + partition);
            }
            
            // 消息确认
            System.out.println("\n=== Message Acknowledgment ===");
            MessageAcknowledgment ack = new MessageAcknowledgment();
            ack.acknowledge("user-events", 0);
            ack.acknowledge("user-events", 1);
            ack.acknowledgeBatch("user-events", Arrays.asList(2L, 3L, 4L));
            
            // 流处理
            System.out.println("\n=== Stream Processing ===");
            StreamProcessor processor = new StreamProcessor("processor-1");
            List<Message> messages = producer.getTopicMessages("user-events");
            processor.processStream("user-events", messages);
            
            System.out.println("Processing statistics: " + processor.getStatistics());
            
            Thread.sleep(1000);
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        KafkaDemo demo = new KafkaDemo();
        demo.runDemo();
    }
}
