package com.interview.systemdesign.im;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 即时通讯系统设计
 * 
 * 核心功能：
 * 1. 用户注册/登录
 * 2. 好友管理
 * 3. 消息发送/接收
 * 4. 群组聊天
 * 5. 消息存储
 * 6. 在线状态
 */
public class IMSystemDesign {
    
    /**
     * 用户实体
     */
    static class User {
        private String userId;
        private String username;
        private String password;
        private boolean online;
        private long lastSeen;
        
        public User(String userId, String username, String password) {
            this.userId = userId;
            this.username = username;
            this.password = password;
            this.online = false;
            this.lastSeen = System.currentTimeMillis();
        }
        
        // getters and setters
        public String getUserId() { return userId; }
        public String getUsername() { return username; }
        public boolean isOnline() { return online; }
        public void setOnline(boolean online) { this.online = online; }
        public long getLastSeen() { return lastSeen; }
        public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
    }
    
    /**
     * 消息实体
     */
    static class Message {
        private String messageId;
        private String senderId;
        private String receiverId;
        private String content;
        private MessageType type;
        private long timestamp;
        private boolean delivered;
        
        public Message(String messageId, String senderId, String receiverId, String content, MessageType type) {
            this.messageId = messageId;
            this.senderId = senderId;
            this.receiverId = receiverId;
            this.content = content;
            this.type = type;
            this.timestamp = System.currentTimeMillis();
            this.delivered = false;
        }
        
        // getters and setters
        public String getMessageId() { return messageId; }
        public String getSenderId() { return senderId; }
        public String getReceiverId() { return receiverId; }
        public String getContent() { return content; }
        public MessageType getType() { return type; }
        public long getTimestamp() { return timestamp; }
        public boolean isDelivered() { return delivered; }
        public void setDelivered(boolean delivered) { this.delivered = delivered; }
    }
    
    /**
     * 消息类型
     */
    enum MessageType {
        TEXT, IMAGE, FILE, SYSTEM
    }
    
    /**
     * 群组实体
     */
    static class Group {
        private String groupId;
        private String groupName;
        private String creatorId;
        private Set<String> members;
        private long createdAt;
        
        public Group(String groupId, String groupName, String creatorId) {
            this.groupId = groupId;
            this.groupName = groupName;
            this.creatorId = creatorId;
            this.members = new HashSet<>();
            this.members.add(creatorId);
            this.createdAt = System.currentTimeMillis();
        }
        
        public void addMember(String userId) {
            members.add(userId);
        }
        
        public void removeMember(String userId) {
            members.remove(userId);
        }
        
        public boolean isMember(String userId) {
            return members.contains(userId);
        }
        
        // getters
        public String getGroupId() { return groupId; }
        public String getGroupName() { return groupName; }
        public Set<String> getMembers() { return members; }
    }
    
    /**
     * 用户服务
     */
    static class UserService {
        private final Map<String, User> users = new ConcurrentHashMap<>();
        private final Map<String, Set<String>> friends = new ConcurrentHashMap<>();
        
        /**
         * 用户注册
         */
        public boolean register(String userId, String username, String password) {
            if (users.containsKey(userId)) {
                return false; // 用户已存在
            }
            
            User user = new User(userId, username, password);
            users.put(userId, user);
            friends.put(userId, new HashSet<>());
            
            System.out.println("User registered: " + username);
            return true;
        }
        
        /**
         * 用户登录
         */
        public boolean login(String userId, String password) {
            User user = users.get(userId);
            if (user != null && user.password.equals(password)) {
                user.setOnline(true);
                user.setLastSeen(System.currentTimeMillis());
                System.out.println("User logged in: " + user.getUsername());
                return true;
            }
            return false;
        }
        
        /**
         * 用户登出
         */
        public void logout(String userId) {
            User user = users.get(userId);
            if (user != null) {
                user.setOnline(false);
                user.setLastSeen(System.currentTimeMillis());
                System.out.println("User logged out: " + user.getUsername());
            }
        }
        
        /**
         * 添加好友
         */
        public boolean addFriend(String userId, String friendId) {
            if (!users.containsKey(userId) || !users.containsKey(friendId)) {
                return false;
            }
            
            friends.get(userId).add(friendId);
            friends.get(friendId).add(userId);
            
            System.out.println("Friends added: " + userId + " <-> " + friendId);
            return true;
        }
        
        /**
         * 获取好友列表
         */
        public Set<String> getFriends(String userId) {
            return friends.getOrDefault(userId, new HashSet<>());
        }
        
        /**
         * 获取用户信息
         */
        public User getUser(String userId) {
            return users.get(userId);
        }
    }
    
    /**
     * 消息服务
     */
    static class MessageService {
        private final Map<String, BlockingQueue<Message>> userMessageQueues = new ConcurrentHashMap<>();
        private final Map<String, List<Message>> messageHistory = new ConcurrentHashMap<>();
        private final Map<String, Set<String>> groupMembers = new ConcurrentHashMap<>();
        
        /**
         * 发送私聊消息
         */
        public boolean sendMessage(String senderId, String receiverId, String content, MessageType type) {
            String messageId = UUID.randomUUID().toString();
            Message message = new Message(messageId, senderId, receiverId, content, type);
            
            // 添加到接收者消息队列
            userMessageQueues.computeIfAbsent(receiverId, k -> new LinkedBlockingQueue<>()).offer(message);
            
            // 保存到历史记录
            String conversationKey = getConversationKey(senderId, receiverId);
            messageHistory.computeIfAbsent(conversationKey, k -> new ArrayList<>()).add(message);
            
            System.out.println("Message sent from " + senderId + " to " + receiverId + ": " + content);
            return true;
        }
        
        /**
         * 发送群组消息
         */
        public boolean sendGroupMessage(String senderId, String groupId, String content, MessageType type) {
            Set<String> members = groupMembers.get(groupId);
            if (members == null || !members.contains(senderId)) {
                return false; // 群组不存在或用户不在群组中
            }
            
            String messageId = UUID.randomUUID().toString();
            
            // 向群组所有成员发送消息
            for (String memberId : members) {
                if (!memberId.equals(senderId)) { // 不给自己发送
                    Message message = new Message(messageId, senderId, memberId, content, type);
                    userMessageQueues.computeIfAbsent(memberId, k -> new LinkedBlockingQueue<>()).offer(message);
                }
            }
            
            // 保存到群组历史记录
            String groupKey = "group:" + groupId;
            Message groupMessage = new Message(messageId, senderId, groupId, content, type);
            messageHistory.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(groupMessage);
            
            System.out.println("Group message sent from " + senderId + " to group " + groupId + ": " + content);
            return true;
        }
        
        /**
         * 接收消息
         */
        public Message receiveMessage(String userId) throws InterruptedException {
            BlockingQueue<Message> queue = userMessageQueues.get(userId);
            if (queue != null) {
                Message message = queue.poll(1, TimeUnit.SECONDS);
                if (message != null) {
                    message.setDelivered(true);
                    System.out.println("Message received by " + userId + ": " + message.getContent());
                }
                return message;
            }
            return null;
        }
        
        /**
         * 获取消息历史
         */
        public List<Message> getMessageHistory(String userId1, String userId2) {
            String conversationKey = getConversationKey(userId1, userId2);
            return messageHistory.getOrDefault(conversationKey, new ArrayList<>());
        }
        
        /**
         * 获取群组消息历史
         */
        public List<Message> getGroupMessageHistory(String groupId) {
            String groupKey = "group:" + groupId;
            return messageHistory.getOrDefault(groupKey, new ArrayList<>());
        }
        
        private String getConversationKey(String userId1, String userId2) {
            // 确保对话键的一致性
            return userId1.compareTo(userId2) < 0 ? 
                userId1 + ":" + userId2 : userId2 + ":" + userId1;
        }
    }
    
    /**
     * 群组服务
     */
    static class GroupService {
        private final Map<String, Group> groups = new ConcurrentHashMap<>();
        
        /**
         * 创建群组
         */
        public String createGroup(String groupName, String creatorId) {
            String groupId = UUID.randomUUID().toString();
            Group group = new Group(groupId, groupName, creatorId);
            groups.put(groupId, group);
            
            System.out.println("Group created: " + groupName + " by " + creatorId);
            return groupId;
        }
        
        /**
         * 加入群组
         */
        public boolean joinGroup(String groupId, String userId) {
            Group group = groups.get(groupId);
            if (group != null) {
                group.addMember(userId);
                System.out.println("User " + userId + " joined group " + group.getGroupName());
                return true;
            }
            return false;
        }
        
        /**
         * 离开群组
         */
        public boolean leaveGroup(String groupId, String userId) {
            Group group = groups.get(groupId);
            if (group != null && group.isMember(userId)) {
                group.removeMember(userId);
                System.out.println("User " + userId + " left group " + group.getGroupName());
                return true;
            }
            return false;
        }
        
        /**
         * 获取群组信息
         */
        public Group getGroup(String groupId) {
            return groups.get(groupId);
        }
        
        /**
         * 获取用户加入的群组
         */
        public List<Group> getUserGroups(String userId) {
            return groups.values().stream()
                .filter(group -> group.isMember(userId))
                .collect(java.util.stream.Collectors.toList());
        }
    }
    
    /**
     * IM系统主类
     */
    static class IMSystem {
        private final UserService userService;
        private final MessageService messageService;
        private final GroupService groupService;
        
        public IMSystem() {
            this.userService = new UserService();
            this.messageService = new MessageService();
            this.groupService = new GroupService();
        }
        
        /**
         * 运行IM系统演示
         */
        public void runDemo() throws InterruptedException {
            System.out.println("=== IM System Demo ===");
            
            // 1. 用户注册和登录
            userService.register("user1", "Alice", "password1");
            userService.register("user2", "Bob", "password2");
            userService.register("user3", "Charlie", "password3");
            
            userService.login("user1", "password1");
            userService.login("user2", "password2");
            userService.login("user3", "password3");
            
            // 2. 添加好友
            userService.addFriend("user1", "user2");
            userService.addFriend("user1", "user3");
            
            // 3. 发送私聊消息
            messageService.sendMessage("user1", "user2", "Hello Bob!", MessageType.TEXT);
            messageService.sendMessage("user2", "user1", "Hi Alice!", MessageType.TEXT);
            
            // 4. 创建群组
            String groupId = groupService.createGroup("Friends", "user1");
            groupService.joinGroup(groupId, "user2");
            groupService.joinGroup(groupId, "user3");
            
            // 5. 发送群组消息
            messageService.sendGroupMessage("user1", groupId, "Welcome to our group!", MessageType.TEXT);
            messageService.sendGroupMessage("user2", groupId, "Thanks for inviting me!", MessageType.TEXT);
            
            // 6. 接收消息
            Thread.sleep(1000);
            messageService.receiveMessage("user2");
            messageService.receiveMessage("user1");
            messageService.receiveMessage("user2");
            messageService.receiveMessage("user3");
            
            // 7. 查看消息历史
            System.out.println("\n=== Message History ===");
            List<Message> history = messageService.getMessageHistory("user1", "user2");
            for (Message msg : history) {
                System.out.println("[" + msg.getTimestamp() + "] " + msg.getSenderId() + ": " + msg.getContent());
            }
            
            // 8. 查看群组消息历史
            System.out.println("\n=== Group Message History ===");
            List<Message> groupHistory = messageService.getGroupMessageHistory(groupId);
            for (Message msg : groupHistory) {
                System.out.println("[" + msg.getTimestamp() + "] " + msg.getSenderId() + " (Group): " + msg.getContent());
            }
            
            // 9. 用户登出
            userService.logout("user1");
            userService.logout("user2");
            userService.logout("user3");
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        IMSystem imSystem = new IMSystem();
        imSystem.runDemo();
    }
}
