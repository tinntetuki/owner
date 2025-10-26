package com.interview.im.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.im.dto.MessageDTO;
import com.interview.im.service.MessageService;
import com.interview.im.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IM WebSocket处理器
 * 
 * @author interview
 * @since 2024-01-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImWebSocketHandler implements WebSocketHandler {

    private final MessageService messageService;
    private final UserSessionService userSessionService;
    private final ObjectMapper objectMapper;

    // 存储用户会话信息
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    // 存储会话ID和用户ID的映射
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket连接建立: sessionId={}", session.getId());
        
        // 从连接参数中获取用户ID
        String userIdStr = session.getUri().getQuery();
        if (userIdStr != null && userIdStr.startsWith("userId=")) {
            Long userId = Long.parseLong(userIdStr.substring(7));
            userSessions.put(session.getId(), session);
            sessionUserMap.put(session.getId(), userId);
            
            log.info("用户连接成功: userId={}, sessionId={}", userId, session.getId());
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        log.info("收到WebSocket消息: sessionId={}, message={}", session.getId(), message.getPayload());

        if (message instanceof TextMessage) {
            handleTextMessage(session, (TextMessage) message);
        } else if (message instanceof BinaryMessage) {
            handleBinaryMessage(session, (BinaryMessage) message);
        } else if (message instanceof PongMessage) {
            handlePongMessage(session, (PongMessage) message);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: sessionId={}", session.getId(), exception);
        closeSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        log.info("WebSocket连接关闭: sessionId={}, closeStatus={}", session.getId(), closeStatus);
        closeSession(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 处理文本消息
     */
    private void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            MessageDTO messageDTO = objectMapper.readValue(payload, MessageDTO.class);
            
            // 设置发送者ID
            Long senderId = sessionUserMap.get(session.getId());
            if (senderId == null) {
                log.warn("未找到用户ID: sessionId={}", session.getId());
                return;
            }
            messageDTO.setSenderId(senderId);
            
            // 处理消息
            processMessage(messageDTO);
            
        } catch (Exception e) {
            log.error("处理文本消息异常: sessionId={}", session.getId(), e);
            sendErrorMessage(session, "消息格式错误");
        }
    }

    /**
     * 处理二进制消息
     */
    private void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        log.info("收到二进制消息: sessionId={}, payloadSize={}", session.getId(), message.getPayload().remaining());
        // 处理文件、图片等二进制消息
    }

    /**
     * 处理Pong消息
     */
    private void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        log.debug("收到Pong消息: sessionId={}", session.getId());
        // 处理心跳消息
    }

    /**
     * 处理消息
     */
    private void processMessage(MessageDTO messageDTO) {
        try {
            // 保存消息到数据库
            messageService.saveMessage(messageDTO);
            
            // 发送消息给接收者
            sendMessageToReceiver(messageDTO);
            
            // 更新会话信息
            updateUserSession(messageDTO);
            
        } catch (Exception e) {
            log.error("处理消息异常: messageDTO={}", messageDTO, e);
        }
    }

    /**
     * 发送消息给接收者
     */
    private void sendMessageToReceiver(MessageDTO messageDTO) {
        Long receiverId = messageDTO.getReceiverId();
        if (receiverId != null) {
            // 查找接收者的WebSocket会话
            WebSocketSession receiverSession = findUserSession(receiverId);
            if (receiverSession != null && receiverSession.isOpen()) {
                try {
                    String messageJson = objectMapper.writeValueAsString(messageDTO);
                    receiverSession.sendMessage(new TextMessage(messageJson));
                    log.info("消息发送成功: receiverId={}, messageId={}", receiverId, messageDTO.getId());
                } catch (Exception e) {
                    log.error("发送消息异常: receiverId={}", receiverId, e);
                }
            } else {
                log.warn("接收者不在线: receiverId={}", receiverId);
                // 可以存储离线消息
            }
        }
    }

    /**
     * 更新用户会话信息
     */
    private void updateUserSession(MessageDTO messageDTO) {
        try {
            userSessionService.updateLastMessage(messageDTO);
        } catch (Exception e) {
            log.error("更新用户会话异常: messageDTO={}", messageDTO, e);
        }
    }

    /**
     * 查找用户会话
     */
    private WebSocketSession findUserSession(Long userId) {
        for (Map.Entry<String, Long> entry : sessionUserMap.entrySet()) {
            if (userId.equals(entry.getValue())) {
                return userSessions.get(entry.getKey());
            }
        }
        return null;
    }

    /**
     * 发送错误消息
     */
    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage("{\"error\":\"" + errorMessage + "\"}"));
            }
        } catch (Exception e) {
            log.error("发送错误消息异常: sessionId={}", session.getId(), e);
        }
    }

    /**
     * 关闭会话
     */
    private void closeSession(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (Exception e) {
            log.error("关闭会话异常: sessionId={}", session.getId(), e);
        } finally {
            userSessions.remove(session.getId());
            sessionUserMap.remove(session.getId());
        }
    }

    /**
     * 发送消息给指定用户
     */
    public void sendMessageToUser(Long userId, String message) {
        WebSocketSession session = findUserSession(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                log.info("消息发送成功: userId={}", userId);
            } catch (Exception e) {
                log.error("发送消息异常: userId={}", userId, e);
            }
        } else {
            log.warn("用户不在线: userId={}", userId);
        }
    }

    /**
     * 广播消息给所有在线用户
     */
    public void broadcastMessage(String message) {
        for (WebSocketSession session : userSessions.values()) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (Exception e) {
                    log.error("广播消息异常: sessionId={}", session.getId(), e);
                }
            }
        }
    }

    /**
     * 获取在线用户数量
     */
    public int getOnlineUserCount() {
        return userSessions.size();
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Long userId) {
        return findUserSession(userId) != null;
    }
}
