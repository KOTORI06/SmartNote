package com.smartnote.controller;

import com.smartnote.dto.ai.AiAnalysisRequest;
import com.smartnote.dto.ai.ChatRequest;
import com.smartnote.dto.ai.RouteRequest;
import com.smartnote.entity.AiAnalysis;
import com.smartnote.service.AiService;
import com.smartnote.util.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /**
     * 发起智能分析
     * POST /api/ai/notes/{noteId}/analyze
     * 对指定笔记发起AI分析请求
     */
    @PostMapping("/notes/{noteId}/analyze")
    public Result<AiAnalysis> analyzeNote(@RequestAttribute Long userId,
                                          @PathVariable Long noteId,
                                          @Valid @RequestBody AiAnalysisRequest request) {
        log.info("发起AI分析: userId={}, noteId={}, analysisType={}",
                userId, noteId, request.getAnalysisType());

        // 检查用户调用次数限制
        if (!aiService.checkDailyQuota(userId)) {
            return Result.error(429, "今日AI调用次数已达上限");
        }

        AiAnalysis analysis = aiService.analyzeNote(userId, noteId, request);
        return Result.success("分析完成", analysis);
    }

    /**
     * 流式分析（SSE）
     * GET /api/ai/notes/{noteId}/analyze/stream
     * 以流式方式返回AI分析结果
     *///111
    @GetMapping(value = "/notes/{noteId}/analyze/stream", produces = "text/event-stream")
    public SseEmitter analyzeNoteStream(@RequestAttribute Long userId,
                                        @PathVariable Long noteId,
                                        @RequestParam String analysisType) {
        log.info("发起流式AI分析: userId={}, noteId={}, analysisType={}",
                userId, noteId, analysisType);

        SseEmitter emitter = new SseEmitter(30000L); // 30秒超时

        // 异步处理流式分析
        aiService.analyzeNoteStream(userId, noteId, analysisType, emitter);

        return emitter;
    }

    /**
     * 获取笔记分析记录
     * GET /api/ai/notes/{noteId}/analysis
     * 获取笔记的所有AI分析记录，支持获取最新记录
     *///111
    @GetMapping("/notes/{noteId}/analysis")
    public Result<Object> getNoteAnalysis(@RequestAttribute Long userId,
                                          @PathVariable Long noteId,
                                          @RequestParam(defaultValue = "true") Boolean latest) {
        log.info("获取分析记录: userId={}, noteId={}, latest={}", userId, noteId, latest);

        if (latest) {
            // 获取最新的分析结果
            AiAnalysis latestAnalysis = aiService.getLatestAnalysis(userId, noteId);
            return Result.success(latestAnalysis);
        } else {
            // 获取所有分析记录
            List<AiAnalysis> allAnalyses = aiService.getAllAnalyses(userId, noteId);
            return Result.success(allAnalyses);
        }
    }

    /**
     * 重新分析笔记
     * POST /api/ai/notes/{noteId}/reanalyze
     * 重新分析笔记，覆盖旧的分析结果
     */
    @PostMapping("/notes/{noteId}/reanalyze")
    public Result<AiAnalysis> reanalyzeNote(@RequestAttribute Long userId,
                                            @PathVariable Long noteId,
                                            @Valid @RequestBody AiAnalysisRequest request) {
        log.info("重新分析笔记: userId={}, noteId={}, analysisType={}",
                userId, noteId, request.getAnalysisType());

        // 检查用户调用次数限制
        if (!aiService.checkDailyQuota(userId)) {
            return Result.error(429, "今日AI调用次数已达上限");
        }

        AiAnalysis analysis = aiService.reanalyzeNote(userId, noteId, request);
        return Result.success("重新分析完成", analysis);
    }

    /**
     * 全局AI对话
     * POST /api/ai/chat
     * 与AI进行自由对话
     *///111
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestAttribute Long userId,
                                            @Valid @RequestBody ChatRequest request) {
        log.info("AI对话: userId={}, messageLength={}", userId, request.getMessage().length());

        // 检查用户调用次数限制
        if (!aiService.checkDailyQuota(userId)) {
            return Result.error(429, "今日AI调用次数已达上限");
        }

        Map<String, Object> response = aiService.chat(userId, request);
        return Result.success(response);
    }

    /**
     * 流式AI对话
     * POST /api/ai/chat/stream
     * 以流式方式与AI对话
     *///111
    @PostMapping(value = "/chat/stream", produces = "text/event-stream")
    public SseEmitter chatStream(@RequestAttribute Long userId,
                                 @Valid @RequestBody ChatRequest request) {
        log.info("流式AI对话: userId={}", userId);

        SseEmitter emitter = new SseEmitter(60000L); // 60秒超时

        // 异步处理流式对话
        aiService.chatStream(userId, request, emitter);

        return emitter;
    }

    /**
     * 智能路由分析
     * POST /api/ai/route
     * 根据用户意图使用不同的Prompt方案
     *///111
    @PostMapping("/route")
    public Result<Map<String, Object>> intelligentRoute(@RequestAttribute Long userId,
                                                        @Valid @RequestBody RouteRequest request) {
        log.info("智能路由: userId={}, query={}", userId, request.getQuery());

        // 检查用户调用次数限制
        if (!aiService.checkDailyQuota(userId)) {
            return Result.error(429, "今日AI调用次数已达上限");
        }

        Map<String, Object> response = aiService.intelligentRoute(userId, request);
        return Result.success(response);
    }

    /**
     * 获取用户AI使用统计
     * GET /api/ai/usage
     * 获取用户今日AI调用次数和限制
     *///111
    @GetMapping("/usage")
    public Result<Map<String, Object>> getUsageStats(@RequestAttribute Long userId) {
        log.info("获取AI使用统计: userId={}", userId);
        Map<String, Object> stats = aiService.getUsageStats(userId);
        return Result.success(stats);
    }

}
