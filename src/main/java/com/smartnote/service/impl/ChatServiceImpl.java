package com.smartnote.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartnote.dto.chat.CreateConversationRequest;
import com.smartnote.dto.chat.ChatMessageVO;
import com.smartnote.dto.chat.ConversationVO;
import com.smartnote.entity.ChatConversation;
import com.smartnote.entity.ChatConversationMember;
import com.smartnote.entity.ChatMessageNew;
import com.smartnote.entity.User;
import com.smartnote.exception.BusinessException;
import com.smartnote.mapper.ChatConversationMapper;
import com.smartnote.mapper.ChatConversationMemberMapper;
import com.smartnote.mapper.ChatMessageNewMapper;
import com.smartnote.mapper.UserMapper;
import com.smartnote.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天服务实现类
 *
 * 1. 管理聊天会话（创建私聊、群聊）
 * 2. 获取用户的会话列表（包含最后一条消息预览）
 * 3. 查询会话的历史消息（分页）
 * 4. 清空会话消息
 * 5. 权限控制（验证用户是否是会话成员）
 *
 * @author SmartNote Team
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatConversationMapper conversationMapper;
    private final ChatConversationMemberMapper memberMapper;
    private final ChatMessageNewMapper messageMapper;
    private final UserMapper userMapper;

    /**
     * 创建私聊会话
     *
     * 1. 检查两人之间是否已存在私聊会话
     * 2. 如果存在，直接返回已有会话ID（避免重复创建）
     * 3. 如果不存在，创建新会话并添加两个成员
     *
     * @param userId 当前用户ID
     * @param friendId 好友ID
     * @return 会话ID（新建的或已存在的）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPrivateChat(Long userId, Long friendId) {

        log.info("创建私聊会话: userId={}, friendId={}", userId, friendId);

        // 构建查询条件，查找所有未删除的私聊会话
        LambdaQueryWrapper<ChatConversation> convWrapper = new LambdaQueryWrapper<>();
        convWrapper.eq(ChatConversation::getType, 1)//聊天类型（私聊会话）
                .eq(ChatConversation::getIsDeleted, 0);//状态（未删除）

        // 找到所有私聊会话
        List<ChatConversation> conversations = conversationMapper.selectList(convWrapper);

        // 遍历所有私聊会话
        for (ChatConversation conv : conversations) {
            // 构建查询条件,查找当前会话中属于当前用户或好友的成员（都是私聊会话只要找两个人都在就行）
            LambdaQueryWrapper<ChatConversationMember> memberWrapper = new LambdaQueryWrapper<>();
            memberWrapper.eq(ChatConversationMember::getConversationId, conv.getId())//会话ID
                    .in(ChatConversationMember::getUserId, userId, friendId);//用户ID是其中之一即可in(,)

            // 统计符合条件的记录数
            long count = memberMapper.selectCount(memberWrapper);
            // 如果正好有2个成员，说明会话已存在
            if (count == 2) {
                log.info("找到已存在的私聊会话: conversationId={}", conv.getId());
                return conv.getId();//返回已存在的会话ID
            }
        }

        // 创建会话实体对象
        ChatConversation conv = new ChatConversation();
        conv.setType(1);//聊天类型（私聊会话）
        conv.setName("私聊");//会话名称
        conv.setOwnerId(userId);//会话创建者ID
        conv.setCreatedAt(LocalDateTime.now());//创建时间
        conv.setUpdatedAt(LocalDateTime.now());//更新时间
        conv.setIsDeleted(0);//状态（未删除）

        //插入数据库
        conversationMapper.insert(conv);

        //添加当前用户为会话成员
        addMember(conv.getId(), userId);
        //添加好友为会话成员
        addMember(conv.getId(), friendId);

        log.info("创建私聊会话成功: conversationId={}", conv.getId());
        return conv.getId();
    }

    /**
     * 创建群聊会话
     *
     * 1. 创建群聊会话记录
     * 2. 添加创建者为成员
     * 3. 批量添加其他成员（排除创建者自己）
     *
     * @param userId 当前用户ID（创建者）
     * @param request 创建请求参数（包含群名称和成员列表）
     * @return 新创建的群聊会话ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGroupChat(Long userId, CreateConversationRequest request) {

        log.info("创建群聊会话: userId={}, name={}, members={}", userId, request.getName(), request.getMemberIds());

        // 创建会话实体对象
        ChatConversation conv = new ChatConversation();
        conv.setType(2);//聊天类型（群聊会话）
        conv.setName(request.getName());//群名称
        conv.setOwnerId(userId);//会话创建者ID
        conv.setCreatedAt(LocalDateTime.now());//创建时间
        conv.setUpdatedAt(LocalDateTime.now());//更新时间
        conv.setIsDeleted(0);//状态（未删除）

        // 会话插入数据库（会话id回填）
        conversationMapper.insert(conv);

        // 添加创建者为成员
        addMember(conv.getId(), userId);
        // 遍历请求中的成员列表，逐个添加（排除创建者自己）
        for (Long memberId : request.getMemberIds()) {
            // 排除创建者自己
            if (!memberId.equals(userId)) {
                // 添加成员
                addMember(conv.getId(), memberId);
            }
        }

        // 返回新创建的群聊会话ID
        log.info("创建群聊会话成功: conversationId={}", conv.getId());
        return conv.getId();
    }

    /**
     * 获取用户的会话列表
     *
     * 1. 查询用户参与的所有会话
     * 2. 为每个会话补充额外信息：
     *    - 成员数量
     *    - 最后一条消息内容
     *    - 最后一条消息时间
     * 3. 按更新时间降序排列（最新的会话在前）
     *
     * - 前端展示会话列表（类似微信的聊天列表）
     * - 显示每条会话的最后一条消息预览
     *
     * @param userId 用户ID
     * @return 会话VO列表
     */
    @Override
    public List<ConversationVO> getUserConversations(Long userId) {

        log.info("获取用户会话列表: userId={}", userId);

        // 构建查询条件,查找用户参与的所有会话
        LambdaQueryWrapper<ChatConversationMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(ChatConversationMember::getUserId, userId);//用户ID

        // 执行查询,获取该用户参与的会话
        List<ChatConversationMember> members = memberMapper.selectList(memberWrapper);

        // 如果没有会话，则返回空列表
        if (members.isEmpty()) {
            return List.of();
        }
        // 提取会话ID列表
        List<Long> convIds = members.stream()
                .map(ChatConversationMember::getConversationId)//获取会话ID
                .toList();//转换为列表

        // 构建查询条件,查找这些会话
        LambdaQueryWrapper<ChatConversation> convWrapper = new LambdaQueryWrapper<>();
        convWrapper.in(ChatConversation::getId, convIds)//在会话ID列表其一
                .eq(ChatConversation::getIsDeleted, 0)//未删除状态
                .orderByDesc(ChatConversation::getUpdatedAt);//按更新时间降序排列（最新的会话排在前面）

        // 执行查询,获取这些会话
        List<ChatConversation> conversations = conversationMapper.selectList(convWrapper);

        return conversations.stream()
                .map(conv -> {
                    // 将 Entity 转换为 VO
                    ConversationVO vo = ConversationVO.fromEntity(conv);

                    // 构建查询条件,统计成员数量
                    LambdaQueryWrapper<ChatConversationMember> countWrapper = new LambdaQueryWrapper<>();
                    countWrapper.eq(ChatConversationMember::getConversationId, conv.getId());//会话ID

                    // 执行查询,获取成员数量
                    long memberCount = memberMapper.selectCount(countWrapper);
                    // 设置成员数量(要强转)
                    vo.setMemberCount((int) memberCount);

                    // 构建查询条件,获取最后一条消息
                    LambdaQueryWrapper<ChatMessageNew> msgWrapper = new LambdaQueryWrapper<>();
                    msgWrapper.eq(ChatMessageNew::getConversationId, conv.getId())//会话ID
                            .orderByDesc(ChatMessageNew::getCreatedAt)//按创建时间降序排列
                            .last("LIMIT 1");//只取第一条（最新的）
                    // 执行查询,获取最后一条消息
                    ChatMessageNew lastMsg = messageMapper.selectOne(msgWrapper);

                    if (lastMsg != null) {
                        // 设置最后消息内容
                        vo.setLastMessageContent(lastMsg.getContent());
                        // 设置最后消息时间
                        vo.setLastMessageTime(lastMsg.getCreatedAt());
                    }

                    // 返回VO
                    return vo;
                })
                .toList();
    }

    /**
     * 获取会话的消息列表（分页）
     *
     * 功能说明：
     * 1. 验证用户是否是会话成员（权限控制）
     * 2. 分页查询消息记录
     * 3. 按时间升序排列（旧消息在前，新消息在后）
     * 4. 转换为 VO 并填充发送者姓名
     *
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 分页的消息VO列表
     */
    @Override
    public Page<ChatMessageVO> getSessionMessages(Long userId, Long conversationId, Integer page, Integer size) {

        log.info("获取会话消息: userId={}, conversationId={}", userId, conversationId);

        // 调用方法检查用户是否是会话成员，如果不是抛出异常
        checkMember(conversationId, userId);

        // 创建分页对象
        Page<ChatMessageNew> messagePage = new Page<>(page, size);
        // 构建查询条件
        LambdaQueryWrapper<ChatMessageNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessageNew::getConversationId, conversationId)//会话ID
                .orderByAsc(ChatMessageNew::getCreatedAt);//按创建时间升序排列（旧消息在前，新消息在后）

        // 执行查询
        Page<ChatMessageNew> resultPage = messageMapper.selectPage(messagePage, wrapper);

        // 创建 VO 分页对象
        Page<ChatMessageVO> voPage = new Page<>(page, size, resultPage.getTotal());
        // 将 Entity 列表转换为 VO 列表
        List<ChatMessageVO> voList = resultPage.getRecords().stream()//创建流
                .map(msg -> {
                    // 获取发送者姓名
                    String senderName = getUserName(msg.getSenderId());
                    // 将 Entity 转换为 VO
                    return ChatMessageVO.fromEntity(msg, senderName);
                })
                .toList();

        // 设置 VO 分页列表的数据
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 清空会话消息
     *
     * 1. 验证用户是否是会话成员（权限控制）
     * 2. 删除该会话的所有消息记录
     * 3. 更新会话的更新时间
     * - 这是物理删除，消息无法恢复
     * - 只删除消息，不删除会话本身
     *
     * @param userId 用户ID
     * @param conversationId 会话ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearSessionMessages(Long userId, Long conversationId) {

        log.info("清空会话消息: userId={}, conversationId={}", userId, conversationId);

        // 检查用户是否是会话成员
        checkMember(conversationId, userId);

        // 构建删除条件：删除该会话的所有消息
        LambdaQueryWrapper<ChatMessageNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessageNew::getConversationId, conversationId);//会话ID

        // 执行删除
        messageMapper.delete(wrapper);

        // 调用方法更新会话的更新时间（标记为最近修改）
        updateConversationTime(conversationId);

        log.info("清空会话消息成功: conversationId={}", conversationId);
    }

    /**
     * 添加会话成员
     *
     * 创建会话时添加成员
     *
     * @param conversationId 会话ID
     * @param userId 用户ID
     */
    private void addMember(Long conversationId, Long userId) {
        //创建成员实体
        ChatConversationMember member = new ChatConversationMember();
        member.setConversationId(conversationId);//会话ID
        member.setUserId(userId);//用户ID
        member.setJoinedAt(LocalDateTime.now());//加入时间

        //插入数据库
        memberMapper.insert(member);
    }

    /**
     * 检查用户是否是会话成员
     *
     * - 权限控制，防止用户访问不属于他的会话
     * - 在查看消息、清空消息前调用
     *
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @throws BusinessException 如果用户不是会话成员
     */
    private void checkMember(Long conversationId, Long userId) {
        // 构建查询条件,查询成员表中该用户是否是该会话的成员
        LambdaQueryWrapper<ChatConversationMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatConversationMember::getConversationId, conversationId)//会话ID
                .eq(ChatConversationMember::getUserId, userId);//用户ID

        // 统计符合条件的记录数
        long count = memberMapper.selectCount(wrapper);
        // 如果记录数等于0，则抛出异常
        if (count == 0) {
            throw new BusinessException("无权访问该会话");
        }
    }

    /**
     * 更新会话的更新时间
     *
     * - 当会话有新消息或被清空时，更新 updateTime
     * - 用于会话列表排序（最新的会话在前）
     *
     * @param conversationId 会话ID
     */
    private void updateConversationTime(Long conversationId) {
        // 查询该会话
        ChatConversation conv = conversationMapper.selectById(conversationId);

        if (conv != null) {
            // 更新会话的更新时间
            conv.setUpdatedAt(LocalDateTime.now());
            // 更新数据库
            conversationMapper.updateById(conv);
        }
    }

    /**
     * 获取用户姓名
     *
     * - 在消息列表中显示发送者的姓名
     * - 如果用户不存在，返回"未知用户"
     *
     * @param userId 用户ID
     * @return 用户姓名
     */
    private String getUserName(Long userId) {
        // 查询用户
        User user = userMapper.selectById(userId);
        // 如果用户存在返回姓名，否则返回默认值
        return user != null ? user.getUsername() : "未知用户";
    }
}
