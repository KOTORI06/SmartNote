package com.smartnote.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartnote.dto.chat.CreateConversationRequest;
import com.smartnote.dto.chat.ChatMessageVO;
import com.smartnote.dto.chat.ConversationVO;

import java.util.List;

/**
 * 聊天服务接口
 *
 * 1. 管理聊天会话（私聊、群聊）的创建和查询
 * 2. 管理聊天消息的查询和清空
 *
 * - 消息持久化通过 WebSocket 触发
 */

public interface ChatService {

    /**
     * 创建私聊会话
     *
     * 业务逻辑：
     * 1. 检查两人之间是否已存在私聊会话
     * 2. 如果存在，返回已有会话ID
     * 3. 如果不存在，创建新会话并添加两个成员
     *
     * @param userId 当前用户ID（发起者）
     * @param friendId 好友用户ID（接收者）
     * @return 会话ID（conversationId）
     */
    Long createPrivateChat(Long userId, Long friendId);

    /**
     * 创建群聊会话
     *
     * 业务逻辑：
     * 1. 创建新的群聊会话记录
     * 2. 将创建者添加为群成员
     * 3. 将所有指定的成员ID添加到群聊
     * 4. 返回新创建的会话ID
     * @param userId 当前用户ID（群主/创建者）
     * @param request 创建群聊请求参数（包含群名称和成员ID列表）
     * @return 会话ID（conversationId）
     */
    Long createGroupChat(Long userId, CreateConversationRequest request);

    /**
     * 获取用户的会话列表
     *
     * 业务逻辑：
     * 1. 查询用户参与的所有会话（私聊 + 群聊）
     * 2. 按最后更新时间降序排列（最新的会话在前）
     * 3. 填充每个会话的附加信息：
     *    - 成员数量
     *    - 最后一条消息内容
     *    - 最后消息时间
     * 4. 转换为 VO 对象返回
     *
     * @param userId 当前用户ID
     * @return 会话列表（按更新时间降序）
     */
    List<ConversationVO> getUserConversations(Long userId);

    /**
     * 获取会话的消息列表（分页）
     *
     * 业务逻辑：
     * 1. 验证用户是否是该会话的成员（权限控制）
     * 2. 分页查询该会话的所有消息
     * 3. 按创建时间升序排列（旧消息在前，新消息在后）
     * 4. 填充发送者姓名
     * 5. 转换为 VO 对象返回
     *
     * - 前端通常从最后一页开始加载（查看最新消息）
     *
     * @param userId 当前用户ID（用于权限验证）
     * @param conversationId 会话ID
     * @param page 页码（从 1 开始）
     * @param size 每页大小（建议 20-50）
     * @return 分页的消息列表（按时间升序）
     */
    Page<ChatMessageVO> getSessionMessages(Long userId, Long conversationId, Integer page, Integer size);

    /**
     * 清空会话的所有消息
     *
     * 业务逻辑：
     * 1. 验证用户是否是该会话的成员（权限控制）
     * 2. 删除该会话的所有消息记录（物理删除）
     * 3. 更新会话的更新时间
     *
     * - 此操作不可逆
     * - 建议前端二次确认
     *
     * @param userId 当前用户ID（用于权限验证）
     * @param conversationId 会话ID
     */
    void clearSessionMessages(Long userId, Long conversationId);
}
