package com.smartnote.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartnote.dto.chat.CreateConversationRequest;
import com.smartnote.dto.chat.ChatMessageVO;
import com.smartnote.dto.chat.ConversationVO;
import com.smartnote.service.ChatService;
import com.smartnote.util.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 创建私聊\
     *
     * @param userId
     * @param friendId
     */
    @PostMapping("/private")
    public Result<Long> createPrivateChat(@RequestAttribute Long userId,
                                          @RequestParam Long friendId) {
        log.info("创建私聊: userId={}, friendId={}", userId, friendId);
        Long conversationId = chatService.createPrivateChat(userId, friendId);
        return Result.success("私聊创建成功", conversationId);
    }

    /**
     * 创建群聊、
     *
     * @param userId
     * @param request
     */
    @PostMapping("/group")
    public Result<Long> createGroupChat(@RequestAttribute Long userId,
                                        @Valid @RequestBody CreateConversationRequest request) {
        log.info("创建群聊: userId={}, name={}", userId, request.getName());
        Long conversationId = chatService.createGroupChat(userId, request);
        return Result.success("群聊创建成功", conversationId);
    }

    /**
     * 获取会话列表
     *
     * @param userId
     */
    @GetMapping("/conversations")
    public Result<List<ConversationVO>> getConversations(@RequestAttribute Long userId) {
        log.info("获取会话列表: userId={}", userId);
        List<ConversationVO> conversations = chatService.getUserConversations(userId);
        return Result.success(conversations);
    }

    /**
     * 获取会话消息（分页查询历史消息）
     *
     * @param userId
     * @param conversationId
     * @param page
     * @param size
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public Result<Page<ChatMessageVO>> getMessages(@RequestAttribute Long userId,
                                                    @PathVariable Long conversationId,
                                                    @RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "20") Integer size) {
        log.info("获取会话消息: userId={}, conversationId={}", userId, conversationId);
        Page<ChatMessageVO> messages = chatService.getSessionMessages(userId, conversationId, page, size);
        return Result.success(messages);
    }

    /**
     * 清空会话消息
     *
     * @param userId
     * @param conversationId
     */
    @DeleteMapping("/conversations/{conversationId}/messages")
    public Result<String> clearMessages(@RequestAttribute Long userId,
                                        @PathVariable Long conversationId) {
        log.info("清空会话消息: userId={}, conversationId={}", userId, conversationId);
        chatService.clearSessionMessages(userId, conversationId);
        return Result.success("消息已清空", null);
    }
}
