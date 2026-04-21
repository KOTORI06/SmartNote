package com.smartnote.ws;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartnote.entity.ChatConversationMember;
import com.smartnote.entity.ChatMessageNew;
import com.smartnote.mapper.ChatConversationMemberMapper;
import com.smartnote.mapper.ChatMessageNewMapper;
import com.smartnote.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * WebSocket 聊天室
 * 功能说明：
 * 1. 实现实时双向通信，支持在线聊天
 * 2. 处理用户上下线事件，广播状态通知
 * 3. 推送离线消息给刚上线的用户
 * 4. 验证用户身份和会话权限
 *
 * 技术要点：
 * - 使用 @ServerEndpoint 注解声明 WebSocket 端点
 * - 使用 ConcurrentHashMap 存储在线用户会话（线程安全）
 * - 通过静态方法注入 Mapper（因为 WebSocket 由容器管理，不是 Spring Bean）
 * - ObjectMapper 用于 Java 对象和 JSON 字符串之间的相互转换
 *
 * WebSocket 必须多实例：因为每个连接有独立的 Session 对象，需要隔离存储
 */
@Slf4j
@Component
@ServerEndpoint("/ws/chat/{userId}")
public class ChatWebSocket {
    /**
     * Spring 只会处理三种注入方式：
     * 1.构造器注入
     * 2.Setter 方法注入（带 @Autowired 的方法）
     * 3.字段注入（但要求对象本身是 Spring Bean）
     *
     * 但是 WebSocket 不是 Spring 创建的
     * WebSocket 容器创建实例，Spring 不知道
     */

    /**
     * 存储所有在线用户的 WebSocket 会话
     * Key: 用户ID, Value: WebSocket Session 对象
     *
     * 用 static
     * - WebSocket 实例由容器创建，每个连接一个实例
     * - static 保证所有实例共享同一个在线用户映射
     *
     * 用 ConcurrentHashMap
     * - 多线程环境下保证线程安全
     */
    private static final ConcurrentHashMap<Long, Session> onlineUsers = new ConcurrentHashMap<>();

    /**
     * 消息 Mapper（静态变量）
     * 用于查询和保存聊天消息
     */
    private static ChatMessageNewMapper messageMapper;

    /**
     * 会话成员 Mapper（静态变量）
     * 用于查询会话成员列表和验证权限
     */
    private static ChatConversationMemberMapper memberMapper;

    /**
     * JSON 序列化工具
     * 用于将 Java 对象转换为 JSON 字符串，或反向解析
     */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 通过依赖注入设置 Mapper 实例
     *
     * - WebSocket 由 Jakarta EE 容器管理，不是 Spring Bean
     * - 无法直接使用 @Autowired 注入依赖
     * - 通过静态方法手动注入，让所有 WebSocket 实例都能使用
     *
     * Spring 启动时：
     * 1. 扫描到 @Component，识别为 Bean
     * 2. 发现 @Autowired 方法
     * 3. 调用这个方法，传入 Mapper
     * @param msgMapper 消息 Mapper
     * @param memMapper 会话成员 Mapper
     */
    @Autowired
    public void setMappers(ChatMessageNewMapper msgMapper, ChatConversationMemberMapper memMapper) {
        ChatWebSocket.messageMapper = msgMapper;
        ChatWebSocket.memberMapper = memMapper;
    }

    /**
     * 连接建立成功时调用
     *
     * 执行流程：
     * 1. 从 URL 参数中提取 Token
     * 2. 验证 Token 有效性
     * 3. 验证 Token 中的用户ID是否匹配
     * 4. 将用户会话存入在线用户映射
     * 5. 推送该用户的离线消息
     * 6. 广播用户上线状态给其他用户
     *
     * @param session WebSocket 会话对象，代表当前连接
     * @param userId 路径参数中的用户ID（从 URL 中提取）
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        /**
         * WebSocket 和 HTTP 不同，握手阶段才支持 Header，建立连接后无法访问 Header
         * 只有 URL 路径参数和查询参数能传递到 @ServerEndpoint
         */

        // 从 URL 查询参数中提取 Token
        String token = getTokenFromQuery(session.getQueryString());

        // 验证 Token 是否存在且有效
        if (token == null || !JwtUtil.validateToken(token)) {
            // 认证失败
            log.warn("WebSocket 认证失败: userId={}", userId);
            // 关闭会话
            closeSession(session, "认证失败");
            return;
        }

        // 验证 Token 中的用户ID是否与路径参数一致
        // 防止用户伪造他人身份
        Long tokenUserId = JwtUtil.getUserIdFromToken(token);
        if (!tokenUserId.equals(userId)) {
            log.warn("用户ID不匹配: userId={}, tokenUserId={}", userId, tokenUserId);
            closeSession(session, "认证失败");
            return;
        }

        // 将用户会话存入在线用户映射
        onlineUsers.put(userId, session);
        log.info("用户上线: userId={}, 在线人数: {}", userId, onlineUsers.size());

        // 查询并推送该用户的未读消息
        pushOfflineMessages(userId);
        // 通知其他用户该用户已上线
        broadcastOnlineStatus(userId, true);
    }

    /**
     * 连接关闭时调用
     *
     * 执行流程：
     * 1. 从在线用户映射中移除
     * 2. 广播用户下线状态
     *
     * @param userId 路径参数中的用户ID
     */
    @OnClose
    public void onClose(@PathParam("userId") Long userId) {
        // 从在线用户映射中移除
        onlineUsers.remove(userId);
        log.info("用户下线: userId={}, 在线人数: {}", userId, onlineUsers.size());
        // 通知其他用户该用户已下线
        broadcastOnlineStatus(userId, false);
    }

    /**
     * 收到客户端消息时调用
     *
     * 执行流程：
     * 1. 解析 JSON 消息
     * 2. 验证用户是否是会话成员
     * 3. 保存消息到数据库
     * 4. 构建响应消息
     * 5. 推送给会话中的所有其他成员
     *
     * @param message 客户端发送的 JSON 字符串
     * @param userId 路径参数中的用户ID（发送者）
     */
    @OnMessage
    public void onMessage(String message, @PathParam("userId") Long userId) {
        try {
            // 将 JSON 字符串解析为 Map 对象
            Map<String, Object> msgData = objectMapper.readValue(message, Map.class);

            // 提取消息字段
            Long conversationId = Long.valueOf(msgData.get("conversationId").toString());//会话ID
            Integer messageType = (Integer) msgData.get("messageType");//消息内容类型(0: 文本消息, 1: 图片消息, 2: 文件消息)
            String content = (String) msgData.get("content");//消息内容
            String mediaUrl = msgData.containsKey("mediaUrl") ? (String) msgData.get("mediaUrl") : null;//媒体文件URL

            // 验证用户是否是会话成员
            if (!isMemberOfConversation(userId, conversationId)) {
                sendError(userId, "无权在此会话中发送消息");
                return;
            }

            // 保存消息到数据库
            ChatMessageNew chatMsg = new ChatMessageNew();
            chatMsg.setConversationId(conversationId);//会话ID
            chatMsg.setSenderId(userId);//发送者ID
            chatMsg.setMessageType(messageType);//消息内容类型(1: 文本消息, 2: 图片消息)
            chatMsg.setContent(content != null ? content : "");//消息内容
            chatMsg.setMediaUrl(mediaUrl);//媒体文件URL
            chatMsg.setIsRead(0);//未读
            chatMsg.setCreatedAt(LocalDateTime.now());//创建时间

            // 保存消息到数据库
            messageMapper.insert(chatMsg);

            // 构建要推送给其他用户的消息对象
            Map<String, Object> response = new HashMap<>();
            response.put("type", "chat");//消息类型：实时聊天
            response.put("messageId", chatMsg.getId());//消息ID
            response.put("conversationId", conversationId);//会话ID
            response.put("senderId", userId);//发送者ID
            response.put("messageType", messageType);//消息内容类型(1: 文本消息, 2: 图片消息)
            response.put("content", content != null ? content : "");//消息内容
            response.put("mediaUrl", mediaUrl != null ? mediaUrl : "");//媒体文件URL
            response.put("createdAt", chatMsg.getCreatedAt().toString());//创建时间

            // 将响应对象序列化为 JSON 字符串
            String jsonResponse = objectMapper.writeValueAsString(response);

            // 获取会话的所有成员ID
            List<Long> memberIds = getConversationMembers(conversationId);

            for (Long memberId : memberIds) {
                // 排除发送者
                if (!memberId.equals(userId)) {
                    // 调用方法发送消息给对应成员
                    sendToUser(memberId, jsonResponse);
                }
            }

            // 记录消息发送成功
            log.info("消息发送成功: conversationId={}, senderId={}, messageId={}", conversationId, userId, chatMsg.getId());

        } catch (Exception e) {
            // 记录处理消息失败
            log.error("处理消息失败: userId={}", userId, e);
            sendError(userId, "消息发送失败");
        }
    }

    /**
     * 发生错误时调用
     *
     * - 网络异常
     * - 协议错误
     * - 服务端处理异常
     *
     * @param session WebSocket 会话对象
     * @param error 异常对象
     * @param userId 路径参数中的用户ID
     */
    @OnError
    public void onError(Session session, Throwable error, @PathParam("userId") Long userId) {
        log.error("WebSocket 错误: userId={}", userId, error);
        // 调用方法关闭会话
        closeSession(session, "发生错误");
    }

    /**
     * 推送离线消息给指定用户
     *
     * 执行流程：
     * 1. 查询数据库中所有未读消息（is_read=0）
     * 2. 过滤出该用户有权限查看的消息
     * 3. 逐条推送给用户
     * 4. 标记消息为已读
     *
     * 注意：
     * - 只在用户上线时调用一次
     * - 推送后立即标记为已读，避免重复推送
     *
     * @param userId 目标用户ID
     */
    private void pushOfflineMessages(Long userId) {
        try {
            // 查询所有未读消息
            List<ChatMessageNew> allUnread = messageMapper.selectList(
                new LambdaQueryWrapper<ChatMessageNew>()// 创建查询条件对象
                    .eq(ChatMessageNew::getIsRead, 0)// 未读消息
                    .orderByDesc(ChatMessageNew::getCreatedAt)// 按创建时间降序（确保最新消息优先推送）
            );

            // 要推送的消息计数
            int pushedCount = 0;
            // 遍历所有未读消息
            for (ChatMessageNew msg : allUnread) {
                // 检查用户是否是该消息所属会话的成员
                if (isMemberOfConversation(userId, msg.getConversationId())) {
                    // 创建推送消息的响应
                    Map<String, Object> response = new HashMap<>();// 创建响应对象
                    response.put("type", "offline_message");// 消息类型(离线消息)
                    response.put("messageId", msg.getId());// 消息ID
                    response.put("conversationId", msg.getConversationId());// 会话ID
                    response.put("senderId", msg.getSenderId());// 发送者ID
                    response.put("messageType", msg.getMessageType());// 消息类型(文本,图片)
                    response.put("content", msg.getContent() != null ? msg.getContent() : "");// 消息内容
                    response.put("mediaUrl", msg.getMediaUrl() != null ? msg.getMediaUrl() : "");// 媒体URL
                    response.put("createdAt", msg.getCreatedAt().toString());// 创建时间

                    String json = objectMapper.writeValueAsString(response);// 将响应对象序列化为JSON
                    sendToUser(userId, json);// 发送消息给用户

                    msg.setIsRead(1);// 标记为已读
                    messageMapper.updateById(msg);// 更新数据库
                    pushedCount++;// 计数
                }
            }

            log.info("推送离线消息完成: userId={}, count={}", userId, pushedCount);

        } catch (Exception e) {
            // 处理异常
            log.error("推送离线消息失败: userId={}", userId, e);
        }
    }

    /**
     * 广播用户在线状态
     *
     * 用途：
     * - 通知其他用户某人上线或下线
     * - 前端可以据此更新好友列表的在线状态
     *
     * @param userId 状态变化的用户ID
     * @param isOnline true-上线，false-下线
     */
    private void broadcastOnlineStatus(Long userId, boolean isOnline) {
        // 构建状态消息
        Map<String, Object> statusMsg = new HashMap<>();// 创建状态消息对象
        statusMsg.put("type", "user_status");// 消息类型(用户状态)
        statusMsg.put("userId", userId);// 用户ID
        statusMsg.put("isOnline", isOnline);// 是否在线

        try {
            // 将状态消息序列化为JSON
            String json = objectMapper.writeValueAsString(statusMsg);
            // 遍历在线用户列表，发送状态消息给其他用户(不包括自己)
            // onlineUsers.forEach((uid, session) -> {})Lambda表达式
            onlineUsers.forEach((uid, session) -> {
                // 排除自己
                if (!uid.equals(userId) && session.isOpen()) {
                    try {
                        /**
                         * getBasicRemote().sendText()同步发送，阻塞直到发送完成
                         * getAsyncRemote().sendText()异步发送，立即返回，后台发送
                         */
                        session.getBasicRemote().sendText(json);
                    } catch (IOException e) {
                        // 处理异常
                        log.error("广播状态失败: uid={}", uid, e);
                    }
                }
            });
        } catch (Exception e) {
            // 处理异常
            log.error("序列化状态消息失败", e);
        }
    }

    /**
     * 发送消息给指定用户
     *
     * @param userId 目标用户ID
     * @param message JSON 格式的消息内容
     */
    private void sendToUser(Long userId, String message) {
        // 从在线用户映射中获取会话
        Session session = onlineUsers.get(userId);
        // 检查会话是否存在且处于打开状态
        if (session != null && session.isOpen()) {
            try {
                // 同步发送文本消息
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                // 处理异常
                log.error("发送消息失败: userId={}", userId, e);
            }
        }
        // 如果用户不在线，什么都不做（消息已存入数据库）
    }

    /**
     * 发送错误消息给指定用户
     *
     * @param userId 目标用户ID
     * @param errorMsg 错误信息
     */
    private void sendError(Long userId, String errorMsg) {
        // 构建错误消息
        Map<String, Object> error = new HashMap<>();
        error.put("type", "error");// 消息类型(错误)
        error.put("message", errorMsg);// 错误信息

        try {
            // 将错误消息序列化为JSON
            String json = objectMapper.writeValueAsString(error);
            // 发送消息给用户
            sendToUser(userId, json);
        } catch (Exception e) {
            // 处理异常
            log.error("发送错误消息失败", e);
        }
    }

    /**
     * 关闭 WebSocket 会话
     *
     * @param session 要关闭的会话
     * @param reason 关闭原因
     */
    private void closeSession(Session session, String reason) {
        try {
            if (session.isOpen()) {
                // 正常关闭连接，并附带原因
                session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, reason));
            }
        } catch (IOException e) {
            /**
             * 不抛出异常因为：
             * 1. 连接可能已经断开，无法关闭
             * 2. 即使关闭失败，资源也会被容器回收
             * 3. 抛出异常没有实际意义，调用方也无法处理
             */
            log.error("关闭会话失败", e);//清理操作不抛异常
        }
    }

    /**
     * 从 URL 查询字符串中提取 Token
     *
     * @param queryString URL 查询字符串
     * @return Token 值，如果不存在返回 null
     */
    private String getTokenFromQuery(String queryString) {
        // 如果查询字符串为空，则返回 null
        if (queryString == null || queryString.isEmpty()) {
            return null;
        }

        // 按 & 分割参数
        String[] params = queryString.split("&");
        // 增强for遍历参数
        for (String param : params) {
            // 查找以 "token=" 开头的参数
            if (param.startsWith("token=")) {
                // 去掉 "token=" 前缀
                return param.substring(6);
            }
        }
        return null;
    }

    /**
     * 获取会话的所有成员ID
     *
     * @param conversationId 会话ID
     * @return 成员用户ID列表
     */
    private List<Long> getConversationMembers(Long conversationId) {
        // 构建查询条件
        LambdaQueryWrapper<ChatConversationMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatConversationMember::getConversationId, conversationId);// 会话ID
        // 查询所有会话成员
        List<ChatConversationMember> members = memberMapper.selectList(wrapper);
        return members.stream()
                .map(ChatConversationMember::getUserId)// 用户ID
                .toList();
    }

    /**
     * 检查用户是否是会话成员
     *
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @return true-是成员，false-不是成员
     */
    private boolean isMemberOfConversation(Long userId, Long conversationId) {
        // 构建查询条件
        LambdaQueryWrapper<ChatConversationMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatConversationMember::getConversationId, conversationId)// 会话ID
                .eq(ChatConversationMember::getUserId, userId);// 用户ID
        // 统计符合条件的记录数
        return memberMapper.selectCount(wrapper) > 0;
    }
}
