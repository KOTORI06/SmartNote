package com.smartnote.service;

import com.smartnote.dto.ai.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AiService {

    /**
     * 执行流式笔记分析
     *
     * @param userId 用户ID（用于权限校验）
     * @param noteId 笔记ID（要分析的笔记）
     * @param request 分析请求参数（包含分析类型、自定义Prompt等）
     * @param emitter SSE发射器（用于向客户端推送流式数据）
     */
    void executeNoteAnalysisStream(Long userId, Long noteId, AiAnalysisRequest request, SseEmitter emitter);

    /**
     * 执行流式智能路由对话
     *
     * @param userId 用户ID（用于额度检查和权限控制）
     * @param request 路由请求参数（包含用户查询内容、会话ID等）
     * @param emitter SSE发射器（用于向客户端推送流式数据）
     */
    void executeIntelligentRouteStream(Long userId, RouteRequest request, SseEmitter emitter);

    /**
     * 获取指定会话的历史消息记录
     *
     * @param userId 用户ID（用于权限校验）
     * @param sessionId 会话ID
     * @return 历史消息列表（按时间升序排列）
     */
    List<ChatMessageVO> getSessionMessages(Long userId, Long sessionId);

    /**
     * 创建新会话
     *
     * @param userId 用户ID（会话所属用户）
     * @param request 创建会话请求参数（包含会话名称）
     * @return 创建成功的会话信息
     */
    ChatSessionVO createSession(Long userId, CreateSessionRequest request);

    /**
     * 删除会话（逻辑删除）
     *
     * @param userId 用户ID（用于权限校验）
     * @param sessionId 会话ID（要删除的会话）
     */
    void deleteSession(Long userId, Long sessionId);

    /**
     * 获取用户所有会话列表
     *
     * @param userId 用户ID（查询该用户的会话）
     * @return 会话列表（按更新时间降序排列）
     */
    List<ChatSessionVO> getUserSessions(Long userId);

    /**
     * 重命名会话
     *
     * @param userId 用户ID（权限校验）
     * @param sessionId 会话ID（要重命名的会话）
     * @param sessionName 新的会话名称
     * @return 更新后的会话VO
     */
    ChatSessionVO renameSession(Long userId, Long sessionId, String sessionName);

    /**
     * 清空会话消息
     *
     * @param userId 用户ID（权限校验）
     * @param sessionId 会话ID（要清空消息的会话）
     */
    void clearSessionMessages(Long userId, Long sessionId);

    /**
     * 流式分析 PDF 文件并创建会话
     *
     * 1. 解析 PDF 文件提取文本内容
     * 2. 调用 AI 生成总结
     * 3. 自动创建新会话（名称基于文件名）
     * 4. 将总结保存为聊天消息
     * 5. 流式返回总结内容给前端
     *
     * @param userId 用户ID
     * @param file PDF 文件
     * @param emitter SSE 发射器，用于流式响应
     */
    void analyzePdfStream(Long userId, MultipartFile file, SseEmitter emitter);
}
