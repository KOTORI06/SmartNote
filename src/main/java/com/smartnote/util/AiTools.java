package com.smartnote.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartnote.entity.Note;
import com.smartnote.mapper.NoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI 工具类 - 提供给 AI 模型调用的函数集合
 *
 * 允许 AI 在对话过程中主动调用后端功能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiTools {

    private final NoteMapper noteMapper;

    /**
     * ThreadLocal 存储当前请求的用户ID
     * - Spring AI 的工具方法签名不支持直接传递 userId
     * ThreadLocal<Long> 是 Java 提供的线程局部变量机制，用于在同一个线程内共享数据
     */
    private static final ThreadLocal<Long> currentUserContext = new ThreadLocal<>();

    /**
     * 设置当前用户ID
     *
     * @param userId 用户ID
     */
    public static void setCurrentUser(Long userId) {
        currentUserContext.set(userId);
    }
    /**
     * 清除当前用户ID
     */
    public static void clearCurrentUser() {
        currentUserContext.remove();
    }
    /**
     * 获取当前用户ID
     *
     * 从 ThreadLocal 中提取 userId，供工具方法使用
     * @return 用户ID，如果未设置则返回 0（防御性编程）
     */
    private Long getCurrentUserId() {
        Long userId = currentUserContext.get();
        if (userId == null) {
            log.warn("未设置用户上下文，使用默认值 0");
            return 0L;
        }
        return userId;
    }

    /**
     * 根据关键词搜索用户的笔记
     * @param keyword 搜索关键词（支持标题和内容模糊匹配）
     * @return 格式化的笔记列表文本（便于 AI 理解和展示）
     */
    @Tool(description = "根据关键词搜索用户的笔记，支持标题和内容的模糊匹配")
    public String searchNotesByKeyword(@ToolParam(description = "关键词") String keyword) {
        // 从 ThreadLocal 获取当前用户ID
        Long userId = getCurrentUserId();
        log.info("AI 调用工具: searchNotesByKeyword, userId={}, keyword={}", userId, keyword);
        // 检查关键词是否为空或只包含空白字符
        if (keyword == null || keyword.trim().isEmpty()) {
            return "错误：搜索关键词不能为空";
        }
        // 限制关键词长度
        if (keyword.length() > 100) {
            return "错误：搜索关键词过长（最多100个字符）";
        }
        // 构建查询条件
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        // 只查询当前用户的笔记
        wrapper.eq(Note::getUserId, userId)
                .eq(Note::getIsDeleted, 0)//只查询未删除的笔记
                //标题或内容包含关键词（模糊查询）
                .and(w -> w.like(Note::getTitle, keyword)// 标题包含关键词
                        .or()
                        .like(Note::getContent, keyword));// 内容包含关键词

        // 按更新时间降序排列，最新的笔记排在前面，更符合用户需求
        wrapper.orderByDesc(Note::getUpdateTime);
        // 执行查询
        List<Note> notes = noteMapper.selectList(wrapper);
        log.info("笔记搜索结果: userId={}, keyword={}, count={}", userId, keyword, notes.size());

        // 如果没有找到匹配的笔记，返回提示信息
        if (notes.isEmpty()) {
            return "未找到与 \"" + keyword + "\" 相关的笔记";
        }

        // 将笔记列表转换为可读的文本格式
        // AI 模型更容易理解结构化的文本而非 JSON
        List<String> noteTexts = notes.stream()
                .map(note -> {
                    // 构建单篇笔记的展示文本
                    StringBuilder noteBuilder = new StringBuilder();
                    // 序号（列表索引加一）
                    int index = notes.indexOf(note) + 1;
                    noteBuilder.append(index).append(". ");
                    noteBuilder.append(note.getTitle()).append("\n");// 标题
                    String contentPreview = note.getContent();// 内容预览
                    // 内容预览长度限制
                    if (contentPreview != null && contentPreview.length() > 100) {
                        contentPreview = contentPreview.substring(0, 100) + "...";
                    }
                    // 加入内容预览
                    noteBuilder.append("   内容：").append(contentPreview != null ? contentPreview : "无内容").append("\n");
                    // 更新时间
                    noteBuilder.append("   更新时间：").append(note.getUpdateTime()).append("\n");
                    // 返回单篇笔记的展示文本(字符串)
                    return noteBuilder.toString();
                })
                .toList();

        // 使用 String.join 拼接所有笔记文本，中间用空行分隔
        String joinedNotes = String.join("\n", noteTexts);

        // 返回完整结果
        return "找到 " + notes.size() + " 篇相关笔记：\n\n" + joinedNotes;
    }

}
