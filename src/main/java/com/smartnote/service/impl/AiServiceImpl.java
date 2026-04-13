package com.smartnote.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartnote.constant.AiPromptConstant;
import com.smartnote.dto.ai.*;
import com.smartnote.entity.AiAnalysis;
import com.smartnote.entity.ChatMessage;
import com.smartnote.entity.ChatSession;
import com.smartnote.entity.Note;
import com.smartnote.exception.BusinessException;
import com.smartnote.mapper.AiMapper;
import com.smartnote.mapper.ChatMessageMapper;
import com.smartnote.mapper.ChatSessionMapper;
import com.smartnote.mapper.NoteMapper;
import com.smartnote.service.AiService;
import com.smartnote.util.AiTools;
import com.smartnote.util.PdfUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor//构造器注入
public class AiServiceImpl implements AiService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final AiMapper aiMapper;
    private final NoteMapper noteMapper;
    private final ChatClient chatClient;
    private final ChatClient simpleChatClient;
    private final AiTools aiTools;

    /**
     * 执行流式笔记分析的核心业务逻辑
     *  使用简单的 ChatClient 进行流式 AI 笔记分析
     * （因为笔记内容已经在提示词拼接了）
     *
     * 1. 验证笔记存在性和用户权限
     * 2. 检查是否已有相同类型的分析记录（决定新增还是更新）
     * 3. 根据分析类型构建 AI Prompt
     * 4. 调用 ChatClient 进行流式 AI 分析
     * 5. 实时将 AI 返回的内容片段推送给前端
     * 6. 分析完成后自动保存到数据库
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @param request 分析请求参数
     * @param emitter SSE发射器
     */
    @Override
    public void executeNoteAnalysisStream(Long userId, Long noteId, AiAnalysisRequest request, SseEmitter emitter) {
        try {
            // 根据笔记ID查询笔记信息
            Note note = noteMapper.selectById(noteId);

            // 验证笔记是否存在或已被删除
            if (note == null || note.getIsDeleted() == 1) {
                sendError(emitter, "笔记不存在");
                return;
            }

            // 验证当前用户是否有权限分析该笔记
            if (!note.getUserId().equals(userId)) {
                sendError(emitter, "无权分析该笔记");
                return;
            }

            // 构建查询条件，查找该笔记的指定类型的最新分析记录
            LambdaQueryWrapper<AiAnalysis> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AiAnalysis::getNoteId, noteId)//笔记ID
                    .eq(AiAnalysis::getAnalysisType, request.getAnalysisType())//分析类型
                    .orderByDesc(AiAnalysis::getCreateTime)//按创建时间降序
                    .last("LIMIT 1");//只返回一条记录

            // 执行查询，获取已存在的分析记录
            AiAnalysis existingAnalysis = aiMapper.selectOne(wrapper);

            // 根据分析类型和笔记内容构建完整的提示词
            String prompt = buildPrompt(request, note);

            // 使用 StringBuilder 累积 AI 返回的所有内容片段
            StringBuilder fullContent = new StringBuilder();

            // 使用 Spring AI 的 ChatClient 发起流式请求
            simpleChatClient.prompt()//创建一个提示词构建器对象
                    .user(prompt)//设置用户消息
                    .stream()//启用流式响应模式
                    .content()//从 AI 响应中提取纯文本内容
                    // doOnNext: 每当接收到一个内容片段（chunk）时触发
                    .doOnNext(chunk -> {
                        try {
                            // 将当前片段追加到完整内容中
                            fullContent.append(chunk);

                            // 立即通过 SSE 推送给前端，实现"打字机"效果
                            emitter.send(SseEmitter.event().name("message").data(chunk));
                        } catch (IOException e) {
                            log.error("发送流式数据失败", e);
                            //抛出一个运行时异常，用于中断 SSE 链接
                            throw new RuntimeException("SSE 连接中断", e);
                        }
                    })
                    // doOnError: 当 AI 调用发生错误时触发
                    .doOnError(error -> {
                        log.error("AI 分析失败", error);
                        try {
                            sendError(emitter, "AI 分析失败: " + error.getMessage());
                        } catch (Exception e) {
                            emitter.completeWithError(e);// 异常关闭 SSE 链接
                        }
                    })
                    // doOnComplete: 当流式传输完成时触发（所有内容都已接收）
                    .doOnComplete(() -> {
                        try {
                            // 创建新的分析记录对象
                            AiAnalysis analysis = new AiAnalysis();
                            analysis.setNoteId(noteId);//笔记ID
                            analysis.setAnalysisType(request.getAnalysisType());//分析类型
                            analysis.setAnalysisContent(fullContent.toString());//分析内容
                            analysis.setPromptUsed(prompt);//使用的提示词
                            analysis.setCreateTime(LocalDateTime.now());//创建时间

                            // 判断是更新还是新增
                            if (existingAnalysis != null) {
                                // 重新分析 - 更新已有记录
                                analysis.setId(existingAnalysis.getId());//设置已有ID
                                aiMapper.updateById(analysis);
                                log.info("更新AI分析记录: id={}", analysis.getId());
                            } else {
                                // 首次分析 - 插入新记录
                                aiMapper.insert(analysis);
                                log.info("创建AI分析记录: id={}", analysis.getId());
                            }

                            // 发送完成事件，告知前端所有数据已传输完毕
                            emitter.send(SseEmitter.event().name("done").data("{\"message\":\"分析完成\"}"));

                            // 正常关闭 SSE 连接
                            emitter.complete();

                        } catch (IOException e) {
                            emitter.completeWithError(e);// 异常关闭 SSE 链接
                        }
                    })
                    //以上都是配置，下面开始执行
                    // subscribe: 订阅流并开始执行
                    .subscribe();
        } catch (Exception e) {
            log.error("执行笔记分析失败", e);
            try {
                sendError(emitter, "系统错误: " + e.getMessage());//发送错误信息给前端
            } catch (Exception ex) {
                emitter.completeWithError(ex);//异常关闭 SSE 链接
            }
        }
    }

    /**
     * 执行流式智能路由对话的核心业务逻辑
     *
     * 先调用一次智能路由对话，获取用户意图（调用简单配置的 simpleChatClient）
     * 再根据用户意图，调用智能路由对话（调用配置了工具类的 ChatClient）
     * (当前版本有BUG,注解识别失败，先用简单版了)
     *
     * 1. 获取或创建会话
     * 2. 保存用户消息到数据库
     * 3. 获取历史消息构建上下文
     * 4. 调用 ChatClient 进行流式对话
     * 5. 实时将 AI 返回的内容片段推送给前端
     * 6. 对话完成后自动保存聊天记录
     *
     * @param userId 用户ID
     * @param request 路由请求参数
     * @param emitter SSE发射器
     */
    @Override
    public void executeIntelligentRouteStream(Long userId, RouteRequest request, SseEmitter emitter) {
        try {
            //设置当前用户ID（用于AI自调用后续查询会话）
            AiTools.setCurrentUser(userId);
            Long sessionId;

            // 如果请求中指定了会话ID，验证其有效性
            if (request.getSessionId() != null) {
                //查询会话ID
                ChatSession session = chatSessionMapper.selectById(request.getSessionId());

                // 验证会话是否存在
                if (session == null || session.getIsDeleted() == 1) {
                    sendError(emitter, "会话不存在");//发送错误信息给前端
                    return;
                }

                // 验证会话是否属于当前用户
                if (!session.getUserId().equals(userId)) {
                    sendError(emitter, "无权访问该会话");
                    return;
                }

                // 使用指定的会话ID
                sessionId = session.getId();
            } else {
                // 如果没有指定会话ID，创建一个新会话
                ChatSession newSession = new ChatSession();
                newSession.setUserId(userId);//设置用户ID
                // 使用用户查询内容的前10个字符作为会话名称
                String sessionName = request.getQuery().length() > 10
                        ? request.getQuery().substring(0, 10) + "..."
                        : request.getQuery();//会话名称截取
                newSession.setSessionName(sessionName);//设置会话名称
                newSession.setCreateTime(LocalDateTime.now());//设置创建时间
                newSession.setUpdateTime(LocalDateTime.now());//设置更新时间
                newSession.setIsDeleted(0);//设置未删除

                // 插入数据库
                chatSessionMapper.insert(newSession);
                sessionId = newSession.getId();//获取新会话ID

                log.info("创建新会话: sessionId={}, userId={}", sessionId, userId);
            }

            //保存用户消息到数据库
            saveChatMessage(sessionId, "user", request.getQuery());

            // 识别用户意图
            String intent = recognizeIntentByAI(request.getQuery());
            log.info("识别用户意图: userId={}, intent={}", userId, intent);

            //获取历史消息构建上下文
            List<ChatMessageVO> historyMessages = getSessionMessages(userId, sessionId);
            //构建完整的对话提示词（传入意图）
            String prompt = buildRoutingPrompt(request, historyMessages, intent);
            //准备接收流式数据
            StringBuilder fullContent = new StringBuilder();
            //调用 AI 模型进行流式对话
            simpleChatClient.prompt()//创建一个 Prompt 构造器
                    .user(prompt)//用户消息（提示词）
                    .stream()//设置为流式对话
                    .content()//提取纯文本内容
                    // doOnNext: 每当接收到一个内容片段时触发
                    .doOnNext(chunk -> {
                        try {
                            //拼接内容片段
                            fullContent.append(chunk);
                            //发送内容片段给前端
                            emitter.send(SseEmitter.event().name("message").data(chunk));
                        } catch (IOException e) {
                            log.error("发送流式数据失败", e);
                            //抛出一个运行时异常，用于中断 SSE 链接
                            throw new RuntimeException("SSE 连接中断", e);
                        }
                    })
                    // doOnError: 当 AI 调用发生错误时触发
                    .doOnError(error -> {
                        log.error("AI 对话失败", error);
                        try {
                            //发送错误信息给前端
                            sendError(emitter, "AI 对话失败: " + error.getMessage());
                        } catch (Exception e) {
                            //异常关闭 SSE 链接
                            emitter.completeWithError(e);
                        }
                    })
                    // doOnComplete: 当流式传输完成时触发
                    .doOnComplete(() -> {
                        try {
                            //保存 AI 回复到数据库
                            saveChatMessage(sessionId, "assistant", fullContent.toString());
                            log.info("对话完成并保存: sessionId={}, contentLength={}", sessionId, fullContent.length());

                            //第七步：通知前端对话完成
                            emitter.send(SseEmitter.event().name("done").data("{\"message\":\"对话完成\"}"));
                            emitter.complete();//完成 SSE 链接

                        } catch (IOException e) {
                            emitter.completeWithError(e);//异常关闭 SSE 链接
                        }
                    })
                    //以上都是配置，下面开始执行
                    // subscribe: 订阅流并开始执行
                    .subscribe();
        } catch (Exception e) {
            log.error("执行智能路由对话失败", e);
            try {
                //发送错误信息给前端
                sendError(emitter, "系统错误: " + e.getMessage());
            } catch (Exception ex) {
                //异常关闭 SSE 链接
                emitter.completeWithError(ex);
            }
        } finally {
            //最后清除当前用户
            AiTools.clearCurrentUser();
        }
    }

    /**
     * 获取指定会话的历史消息记录
     *
     * 功能说明：
     * 1. 验证会话是否存在且属于当前用户（权限控制）
     * 2. 查询该会话下的所有消息记录
     * 3. 按时间升序排列（保证对话顺序正确）
     * 4. 转换为 VO 对象返回给前端
     *
     * 技术要点：
     * - 严格的权限校验，防止越权访问他人会话
     * - 使用 LambdaQueryWrapper 构建查询条件
     * - 消息按创建时间升序，符合对话的自然顺序
     * - 实体转 VO，隐藏敏感字段
     *
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return 历史消息列表
     */
    @Override
    public List<ChatMessageVO> getSessionMessages(Long userId, Long sessionId) {
        log.info("获取会话消息记录: userId={}, sessionId={}", userId, sessionId);
        //根据会话ID查询会话信息
        ChatSession session = chatSessionMapper.selectById(sessionId);

        //检查会话是否存在或已被删除
        if (session == null || session.getIsDeleted() == 1) {
            // 会话不存在或已删除，抛出业务异常
            throw new BusinessException("会话不存在");
        }

        // 验证当前用户是否为会话的拥有者
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该会话");
        }

        // 构建查询条件：查找指定会话的所有消息
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)// 会话ID匹配
                .orderByAsc(ChatMessage::getCreateTime);// 按创建时间升序排列（为了前端展示方便）

        // 执行查询，获取消息列表
        // 升序排列确保消息按对话发生的先后顺序返回
        List<ChatMessage> messages = chatMessageMapper.selectList(wrapper);

        //实体转VO，VO只暴露必要字段，隐藏数据库主键等内部信息
        return messages.stream()
                .map(ChatMessageVO::fromEntity)
                .toList();//转换为VOList
    }

    /**
     * 创建新会话的核心业务逻辑
     *
     * 1. 接收用户指定的会话名称
     * 2. 创建会话记录并关联到当前用户
     * 3. 初始化会话的时间字段和删除标记
     * 4. 保存到数据库
     * 5. 返回创建成功的会话信息（VO对象）
     *
     * @param userId 用户ID
     * @param request 创建会话请求参数
     * @return 创建成功的会话VO对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)//不加括号内容的话只回滚不受检异常
    public ChatSessionVO createSession(Long userId, CreateSessionRequest request) {
        log.info("创建新会话: userId={}, sessionName={}", userId, request.getSessionName());

        // 创建会话记录
        ChatSession session = new ChatSession();
        // 设置会话所属的用户ID（关联到当前登录用户）
        session.setUserId(userId);
        // 设置会话名称
        session.setSessionName(request.getSessionName());
        // 设置创建时间为当前时间
        session.setCreateTime(LocalDateTime.now());
        // 设置更新时间为当前时间（初始时与创建时间相同）
        session.setUpdateTime(LocalDateTime.now());
        // 设置逻辑删除标记为 0（未删除状态）
        session.setIsDeleted(0);

        // 保存会话记录到数据库
        chatSessionMapper.insert(session);
        // 返回创建成功的会话VO对象
        return ChatSessionVO.fromEntity(session);
    }

    /**
     * 删除会话的核心业务逻辑（逻辑删除）
     *
     * 1. 验证会话是否存在
     * 2. 验证当前用户是否有权限删除该会话
     * 3. 将会话的 isDeleted 字段标记为 1（逻辑删除）
     * 4. 更新会话的更新时间
     * 5. 会话关联的历史消息保留在数据库中（不物理删除）
     * @param userId 用户ID
     * @param sessionId 会话ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)  // 开启事务，确保数据一致性
    public void deleteSession(Long userId, Long sessionId) {
        log.info("删除会话: userId={}, sessionId={}", userId, sessionId);

        // 根据会话ID查询会话信息
        ChatSession session = chatSessionMapper.selectById(sessionId);

        // 验证会话是否存在，或是否已经被删除
        if (session == null || session.getIsDeleted() == 1) {
            // 抛出业务异常，全局异常处理器会捕获并返回错误信息
            throw new BusinessException("会话不存在");
        }
        // 验证当前用户是否是会话的所有者
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权删除该会话");
        }
        // 物理删除会话下的所有消息(去掉孤儿消息)
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId);
        chatMessageMapper.delete(wrapper);

        // 逻辑删除会话
        chatSessionMapper.deleteById(sessionId);
    }

    /**
     * 获取用户所有会话列表的核心业务逻辑
     *
     * 1. 根据用户ID查询该用户的所有会话
     * 2. 过滤掉已删除的会话（isDeleted=0）
     * 3. 按更新时间降序排列（最新的会话排在前面）
     * 4. 将 Entity 对象转换为 VO 对象返回
     *
     * @param userId 用户ID
     * @return 会话VO列表
     */
    @Override
    public List<ChatSessionVO> getUserSessions(Long userId) {
        log.info("获取用户会话列表: userId={}", userId);

        // 创建查询条件构造器
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, userId)// 用户ID
                .eq(ChatSession::getIsDeleted, 0)// 未删除状态
                .orderByDesc(ChatSession::getUpdateTime);// 按更新时间降序排列（最新的会话排在前面）
        // 执行查询
        List<ChatSession> sessions = chatSessionMapper.selectList(wrapper);
        // Entity 转 VO
        return sessions.stream()
                .map(ChatSessionVO::fromEntity)// 转换为VO
                .toList();
    }

    /**
     * 重命名会话的核心业务逻辑
     *
     * 1. 验证会话是否存在且未被删除
     * 2. 验证当前用户是否有权限修改该会话
     * 3. 更新会话名称和更新时间
     * 4. 返回更新后的会话信息
     *
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param sessionName 新的会话名称
     * @return 更新后的会话VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatSessionVO renameSession(Long userId, Long sessionId, String sessionName) {
        log.info("重命名会话: userId={}, sessionId={}, newName={}", userId, sessionId, sessionName);

        // 根据会话ID查询会话信息
        ChatSession session = chatSessionMapper.selectById(sessionId);

        // 会话是否存在
        if (session == null || session.getIsDeleted() == 1) {
            // 抛出业务异常，全局异常处理器会捕获并返回错误信息
            throw new BusinessException("会话不存在");
        }
        // 当前用户是否是会话的所有者
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权修改该会话");
        }
        // 设置新的会话名称
        session.setSessionName(sessionName);
        // 更新修改时间
        session.setUpdateTime(LocalDateTime.now());
        // 数据库更新
        chatSessionMapper.updateById(session);
        // 将更新后的 Entity 转换为 VO 返回
        return ChatSessionVO.fromEntity(session);
    }

    /**
     * 清空会话消息的核心业务逻辑
     *
     * 功能说明：
     * 1. 验证会话是否存在且未被删除
     * 2. 验证当前用户是否有权限操作该会话（防止越权操作）
     * 3. 删除该会话下的所有消息记录
     * 4. 更新会话的更新时间（标记为最近修改）
     * @param userId 用户ID
     * @param sessionId 会话ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearSessionMessages(Long userId, Long sessionId) {
        log.info("清空会话消息: userId={}, sessionId={}", userId, sessionId);

        // 根据会话ID查询会话信息
        ChatSession session = chatSessionMapper.selectById(sessionId);

        // 会话是否存在
        if (session == null || session.getIsDeleted() == 1) {
            // 抛出业务异常，全局异常处理器会捕获并返回错误信息
            throw new BusinessException("会话不存在");
        }
        // 当前用户是否是会话的所有者
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该会话");
        }

        // 构建删除条件
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId);// 会话ID

        // 执行批量删除操作
        chatMessageMapper.delete(wrapper);
        // 更新会话的最后修改时间
        session.setUpdateTime(LocalDateTime.now());
        // 执行数据库更新操作
        chatSessionMapper.updateById(session);
    }

    /**
     * 流式分析 PDF 文件并创建会话
     * 不需要调用AI工具类，使用简单配置的ChatClient
     *
     * 1. 使用 PdfUtils 提取 PDF 文本
     * 2. 基于文件名创建新会话
     * 3. 构建 PDF 总结提示词
     * 4. 调用 ChatClient 流式生成总结
     * 5. 实时推送总结片段到前端
     * 6. 收集完整总结内容
     * 7. 保存总结为 assistant 消息
     * 8. 发送完成信号
     *
     * @param userId 用户ID
     * @param file PDF 文件
     * @param emitter SSE 发射器，用于流式响应
     */
    @Override
    public void analyzePdfStream(Long userId, MultipartFile file, SseEmitter emitter) {
        try {
            // 调用 PDF 工具类提取文本内容
            String pdfText = PdfUtils.extractTextFromPdf(file);
            // 获取原始文件名（用于生成会话名称）
            // getOriginalFilename() 返回用户上传时的文件名
            String originalFilename = file.getOriginalFilename();
            // 从文件名中提取不带扩展名的部分
            String fileName = PdfUtils.extractFileNameWithoutExtension(originalFilename);

            // 构建会话名称：前缀 + 文件名
            String sessionName = "PDF总结: " + fileName;

            // 创建会话请求对象
            CreateSessionRequest sessionRequest = new CreateSessionRequest();
            sessionRequest.setSessionName(sessionName);

            // 调用创建会话方法
            ChatSessionVO session = createSession(userId, sessionRequest);
            log.info("PDF 分析会话创建成功: sessionId={}, userId={}, fileName={}", session.getId(), userId, fileName);
            // 调用构建 PDF 总结提示词的方法
            String prompt = buildPdfSummaryPrompt(pdfText);

            // 使用 StringBuilder 收集完整的总结内容
            StringBuilder fullSummary = new StringBuilder();
            // 调用 Spring AI 的 ChatClient 进行流式对话
            simpleChatClient.prompt()//创建一个新的提示请求构建器
                      .user(prompt)//设置用户消息（提示词）
                      .stream()//启用流式模式，返回 Flux 响应式流
                      .content()//只提取文本内容部分
                    // doOnNext: 每当接收到一个文本片段时触发
                    .doOnNext(chunk -> {
                        try {
                            // 将片段追加到完整总结中
                            fullSummary.append(chunk);
                            /**通过 SSE 推送片段到前端
                             * SseEmitter.event() 创建一个事件对象
                             * .name("message") 设置事件名称
                             * .data(chunk) 设置事件数据（当前文本片段）
                             */
                            emitter.send(SseEmitter.event().name("message").data(chunk));
                        } catch (IOException e) {
                            // 如果推送失败
                            log.error("推送 PDF 总结片段失败", e);
                            // 抛出运行时异常，中断流式处理
                            throw new RuntimeException("SSE 连接中断", e);//
                        }
                    })
                    // doOnError: 当 AI 调用发生错误时触发
                    .doOnError(error -> {
                        // 记录错误日志
                        log.error("AI 生成 PDF 总结失败", error);
                        try {
                            // 向前端发送错误信息
                            sendError(emitter, "AI 分析失败: " + error.getMessage());
                        } catch (Exception e) {
                            // 如果发送错误也失败，强制关闭连接
                            emitter.completeWithError(e);
                        }
                    })
                    // doOnComplete: 当流式输出全部完成时触发
                    .doOnComplete(() -> {
                        try {
                            // 获取完整的总结文本(转换为字符串)
                            String summaryText = fullSummary.toString();
                            // 检查总结是否为空
                            if (summaryText.isEmpty()) {
                                sendError(emitter, "AI 未生成有效总结");
                                return;
                            }
                            // 调用 saveChatMessage() 保存总结
                            saveChatMessage(session.getId(), "assistant", summaryText);
                            log.info("PDF 总结保存成功: sessionId={}, contentLength={}", session.getId(), summaryText.length());

                            // 向前端发送完成事件
                            emitter.send(SseEmitter.event().name("done").data("{\"message\":\"PDF 分析完成\",\"sessionId\":" + session.getId() + "}"));
                            // 正常关闭 SSE 连接
                            emitter.complete();
                        } catch (IOException e) {
                            log.error("发送 PDF 分析完成信号失败", e);// 如果发送完成信号失败
                            emitter.completeWithError(e);// 异常关闭 SSE 连接
                        }
                    })
                    // subscribe(): 启动响应式流，开始实际执行
                    .subscribe();

        } catch (BusinessException e) {
            // 捕获业务异常（文件验证失败等）
            log.warn("PDF 分析业务异常: {}", e.getMessage());
            try {
                sendError(emitter, e.getMessage());// 发送错误信息给前端

            } catch (Exception ex) {
                emitter.completeWithError(ex);// 如果发送也失败，强制关闭

            }
        } catch (Exception e) {
            // 捕获所有未处理的异常
            log.error("PDF 分析过程发生未知错误", e);
            try {
                // 发送错误信息
                sendError(emitter, "系统错误: " + e.getMessage());

            } catch (Exception ex) {
                // 异常关闭 SSE 连接
                emitter.completeWithError(ex);
            }
        }
    }

    /**
     * 构建 PDF 总结的 AI 提示词
     *
     * 提示词设计原则：
     * 1. 明确任务目标：告诉 AI 要做什么
     * 2. 指定输出格式：规定返回的结构
     * 3. 设定字数限制：控制输出长度
     * 4. 强调关键要素：突出重点内容
     * 5. 提供示例模板：让 AI 理解期望的输出
     * 输出格式（Markdown）
     * @param pdfText PDF 提取的文本内容
     * @return 完整的提示词字符串
     */
    private String buildPdfSummaryPrompt(String pdfText) {

        // 使用文本块（Text Block）语法（Java 15+）
        // """ 三引号可以保留换行和缩进，适合长文本
        //String.format() 是 Java 的字符串格式化方法，用于将占位符替换为实际值
        return String.format(AiPromptConstant.PDF_SUMMARY_TEMPLATE, pdfText);
    }

    /**
     * 根据意图构建不同的 Prompt
     *
     * @param request 当前请求参数
     * @param historyMessages 历史消息列表
     * @param intent 识别出的意图类型
     * @return 构建好的完整 Prompt
     */
    private String buildRoutingPrompt(RouteRequest request, List<ChatMessageVO> historyMessages, String intent) {

        // 创建一个 StringBuilder 用于构建 Prompt
        StringBuilder promptBuilder = new StringBuilder();

        // 根据意图添加角色设定
        switch (intent) {
            case "NOTE_ANALYSIS":
                promptBuilder.append(AiPromptConstant.NOTE_ANALYSIS_ROLE);
                break;
            case "KNOWLEDGE_SEARCH":
                promptBuilder.append(AiPromptConstant.KNOWLEDGE_SEARCH_ROLE);
                break;
            case "CHAT":
            default:
                promptBuilder.append(AiPromptConstant.CHAT_ROLE);
                break;
        }

        // 如果有历史消息，添加上下文
        if (historyMessages != null && !historyMessages.isEmpty()) {
            promptBuilder.append("以下是之前的对话历史：\n\n");

            // 只保留最近 10 条消息，避免超出 Token 限制
            int startIndex = Math.max(0, historyMessages.size() - 10);//获取最近 10 条消息的开始索引
            List<ChatMessageVO> recentMessages = historyMessages.subList(startIndex, historyMessages.size());//获取最近 10 条消息

            for (ChatMessageVO msg : recentMessages) {
                String rolePrefix = "user".equals(msg.getRole()) ? "用户" : "助手";//根据角色，设置前缀
                promptBuilder.append(rolePrefix).append("：").append(msg.getContent()).append("\n\n");
            }

            promptBuilder.append("--- 以上是历史对话 ---\n\n");
        }

        // 添加当前用户的问题
        promptBuilder.append("用户当前问题：").append(request.getQuery());

        // 返回完整的 Prompt 字符串
        return promptBuilder.toString();
    }

    /**
     * 使用 AI 模型识别用户意图
     *
     * @param query 用户输入
     * @return 意图类型：NOTE_ANALYSIS / KNOWLEDGE_SEARCH / CHAT
     */
    private String recognizeIntentByAI(String query) {
        String intentPrompt = AiPromptConstant.INTENT_RECOGNITION_TEMPLATE;

        try {
            String intent = simpleChatClient.prompt()
                    .user(intentPrompt)//设置用户输入
                    .call()//阻塞式输出获取短文本
                    .content();//获取生成的回答文本内容

            return intent != null ? intent.trim().toUpperCase() : "CHAT";//转换为大写并返回,默认为闲聊模式
        } catch (Exception e) {
            // 发生异常时，默认使用闲聊模式
            log.warn("AI 意图识别失败，默认使用闲聊模式", e);
            return "CHAT";
        }
    }

    /**
     * 保存聊天消息到数据库
     *
     * @param sessionId 会话ID
     * @param role 角色（user/assistant）
     * @param content 消息内容
     */
    public void saveChatMessage(Long sessionId, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);//会话ID
        message.setRole(role);//角色
        message.setContent(content);//消息内容
        message.setCreateTime(LocalDateTime.now());//设置创建时间

        // 插入数据库
        chatMessageMapper.insert(message);

        // 更新会话的更新时间
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session != null) {
            session.setUpdateTime(LocalDateTime.now());//更新时间
            chatSessionMapper.updateById(session);//更新数据库
        }
    }

    /**
     * 根据分析类型和笔记内容构建 AI Prompt
     *
     * @param request 分析请求参数
     * @param note 笔记实体对象
     * @return 构建好的完整 Prompt
     */
    private String buildPrompt(AiAnalysisRequest request, Note note) {

        // 根据分析类型选择基础 Prompt 模板（转换全部大写）
        String basePrompt = switch (request.getAnalysisType().toUpperCase()) {
            case "SUMMARY" ->
                    AiPromptConstant.NOTE_SUMMARY_PROMPT;

            case "KEY_POINTS" ->
                    AiPromptConstant.NOTE_KEY_POINTS_PROMPT;

            case "TAGS" ->
                    AiPromptConstant.NOTE_TAGS_PROMPT;

            default ->
                    AiPromptConstant.NOTE_DEFAULT_PROMPT;
        };

        // 如果用户提供了自定义 Prompt，则覆盖默认模板
        if (request.getCustomPrompt() != null && !request.getCustomPrompt().isEmpty()) {
            basePrompt = request.getCustomPrompt() + "\n\n";
        }

        // 拼接最终的 Prompt：指令 + 笔记标题 + 笔记内容
        return basePrompt + "标题：" + note.getTitle() + "\n\n内容：" + note.getContent();
    }

    /**
     * 向客户端发送错误信息并关闭 SSE 连接
     *
     * @param emitter SSE发射器
     * @param message 错误消息
     */
    private void sendError(SseEmitter emitter, String message) {
        try {
            // 发送错误信息
            emitter.send(SseEmitter.event().name("error").data("{\"message\":\"" + message + "\"}"));
            emitter.complete();// 正常关闭 SSE 链接
        } catch (IOException e) {
            emitter.completeWithError(e);// 异常关闭 SSE 链接
        }
    }


}