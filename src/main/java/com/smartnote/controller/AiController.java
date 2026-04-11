package com.smartnote.controller;

import com.smartnote.dto.ai.*;
import com.smartnote.exception.BusinessException;
import com.smartnote.service.AiService;
import com.smartnote.util.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;


@Slf4j
@Validated
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /**
     * 流式 PDF 总结接口
     * POST /api/ai/files/pdf/analyze
     *///111
    @PostMapping(value = "/files/pdf/analyze", produces = "text/event-stream")
    public SseEmitter analyzePdfStream(@RequestAttribute Long userId,
                                       @RequestParam("file") MultipartFile file) {
        log.info("发起 PDF 流式分析: userId={}, fileName={}", userId, file.getOriginalFilename());

        if (!"application/pdf".equals(file.getContentType())) {
            throw new BusinessException("仅支持 PDF 格式文件");
        }

        SseEmitter emitter = new SseEmitter(120000L); // PDF 处理较慢，超时设长一点

        // 调用 Service 层处理
        aiService.analyzePdfStream(userId, file, emitter);

        return emitter;
    }

    /**
     * 统一的流式笔记分析接口
     * POST /api/ai/notes/{noteId}/analysis/stream
     *
     * 功能整合：
     * 1. 自动处理“首次分析”与“重新分析”逻辑（后端根据是否存在旧记录判断）。
     * 2. 实时流式返回分析内容。
     * 3. 传输完成后自动保存至数据库。
     */
    @PostMapping(value = "/notes/{noteId}/analysis/stream", produces = "text/event-stream")
    public SseEmitter streamNoteAnalysis(@RequestAttribute Long userId,
                                         @PathVariable Long noteId,
                                         @Valid @RequestBody AiAnalysisRequest request) {
        log.info("发起流式笔记分析: userId={}, noteId={}, type={}", userId, noteId, request.getAnalysisType());

        //创建流式发射器
        SseEmitter emitter = new SseEmitter(60000L);

        //执行分析与存储
        aiService.executeNoteAnalysisStream(userId, noteId, request, emitter);

        return emitter;
    }

    /**
     * 流式智能路由对话接口
     * POST /api/ai/chat/completions
     *
     * 实战经验说明：
     * 2. 意图识别：后端自动判断是“笔记分析”、“闲聊”还是“代码辅助”。
     * 3. 流式响应：使用 SSE (Server-Sent Events) 实现打字机效果。
     * 4. 统一入口：前端无需维护多个 AI 接口，全部走这里。
     */
    @PostMapping(value = "/chat/completions", produces = "text/event-stream")
    public SseEmitter streamChatCompletion(@RequestAttribute Long userId,
                                           @Valid @RequestBody RouteRequest request) {
        log.info("发起流式智能路由: userId={}, query={}", userId, request.getQuery());

        //设置较长的超时时间，防止长文本生成中断
        SseEmitter emitter = new SseEmitter(60000L);

        //执行智能路由
        aiService.executeIntelligentRouteStream(userId, request, emitter);

        return emitter;
    }

    /**
     * 获取用户所有会话列表
     * GET /api/ai/sessions
     */
    @GetMapping("/sessions")
    public Result<List<ChatSessionVO>> getUserSessions(@RequestAttribute Long userId) {
        log.info("获取用户会话列表: userId={}", userId);
        List<ChatSessionVO> sessions = aiService.getUserSessions(userId);
        return Result.success(sessions);
    }

    /**
     * 创建新会话
     * POST /api/ai/sessions
     */
    @PostMapping("/sessions")
    public Result<ChatSessionVO> createSession(@RequestAttribute Long userId,
                                               @Valid @RequestBody CreateSessionRequest request) {
        log.info("创建新会话: userId={}, sessionName={}", userId, request.getSessionName());
        ChatSessionVO session = aiService.createSession(userId, request);
        return Result.success("会话创建成功", session);
    }

    /**
     * 获取指定会话的历史消息
     * GET /api/ai/sessions/{sessionId}/messages
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<ChatMessageVO>> getSessionMessages(@RequestAttribute Long userId,
                                                          @PathVariable Long sessionId) {
        log.info("获取会话消息: userId={}, sessionId={}", userId, sessionId);
        List<ChatMessageVO> messages = aiService.getSessionMessages(userId, sessionId);
        return Result.success(messages);
    }

    /**
     * 删除会话
     * DELETE /api/ai/sessions/{sessionId}
     */
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(@RequestAttribute Long userId,
                                      @PathVariable Long sessionId) {
        log.info("删除会话: userId={}, sessionId={}", userId, sessionId);
        aiService.deleteSession(userId, sessionId);
        return Result.success("会话删除成功", null);
    }

    /**
     * 重命名会话
     * PUT /api/ai/sessions/{sessionId}
     */
    @PutMapping("/sessions/{sessionId}")
    public Result<ChatSessionVO> renameSession(@RequestAttribute Long userId,
                                               @PathVariable Long sessionId,
                                               @Valid @RequestBody CreateSessionRequest request) {
        log.info("重命名会话: userId={}, sessionId={}, newName={}", userId, sessionId, request.getSessionName());
        ChatSessionVO session = aiService.renameSession(userId, sessionId, request.getSessionName());
        return Result.success("会话重命名成功", session);
    }

    /**
     * 清空会话消息
     * DELETE /api/ai/sessions/{sessionId}/messages
     */
    @DeleteMapping("/sessions/{sessionId}/messages")
    public Result<Void> clearSessionMessages(@RequestAttribute Long userId,
                                             @PathVariable Long sessionId) {
        log.info("清空会话消息: userId={}, sessionId={}", userId, sessionId);
        aiService.clearSessionMessages(userId, sessionId);
        return Result.success("会话消息已清空", null);
    }

}
