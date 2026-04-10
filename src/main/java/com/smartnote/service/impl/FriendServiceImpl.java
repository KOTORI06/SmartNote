package com.smartnote.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartnote.dto.friend.FriendRequest;
import com.smartnote.dto.friend.FriendRequestVO;
import com.smartnote.dto.friend.FriendVO;
import com.smartnote.entity.FriendRelation;
import com.smartnote.entity.Group;
import com.smartnote.entity.User;
import com.smartnote.exception.BusinessException;
import com.smartnote.mapper.FriendMapper;
import com.smartnote.mapper.GroupMapper;
import com.smartnote.mapper.UserMapper;
import com.smartnote.service.FriendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor//构造器注入
public class FriendServiceImpl implements FriendService {

    private final FriendMapper friendMapper;
    private final GroupMapper groupMapper;
    private final UserMapper userMapper;

    /**
     * 发送好友申请
     *
     * @param userId
     * @param request
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FriendRelation sendFriendRequest(Long userId, FriendRequest request) {
        log.info("发送好友申请: userId={}, friendId={}", userId, request.getFriendId());

        // 不能添加自己为好友
        if (userId.equals(request.getFriendId())) {
            throw new BusinessException("不能添加自己为好友");
        }

        // 目标用户是否存在
        User targetUser = userMapper.selectById(request.getFriendId());
        if (targetUser == null) {
            throw new BusinessException("目标用户不存在");
        }

        //编写查询条件（不能重复添加好友）
        LambdaQueryWrapper<FriendRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(FriendRelation::getUserId, userId)//当前用户
                        .eq(FriendRelation::getFriendId, request.getFriendId())//目标用户
                        .or()
                        .eq(FriendRelation::getUserId, request.getFriendId())//目标用户
                        .eq(FriendRelation::getFriendId, userId))//当前用户

                .ne(FriendRelation::getStatus, 3);//状态不等于已删除（保留好友历史）

        //执行查询
        FriendRelation existingRelation = friendMapper.selectOne(wrapper);
        //存在（看状态）
        if (existingRelation != null) {
            //已是好友
            if (existingRelation.getStatus() == 1) {
                throw new BusinessException("你们已经是好友了");
            } else if (existingRelation.getStatus() == 0) {
                //已发送过好友申请
                throw new BusinessException("好友申请已发送，请等待对方处理");
            } else if (existingRelation.getStatus() == 2) {
                //拒绝过
                existingRelation.setStatus(0);
                existingRelation.setApplyRemark(request.getApplyRemark());
                existingRelation.setUpdateTime(LocalDateTime.now());
                friendMapper.updateById(existingRelation);
                return existingRelation;
            }
        }

        //以前没加过，创建新纪录（申请没处理就一条记录，添加好友再创建双向记录）
        FriendRelation relation = new FriendRelation();
        relation.setUserId(userId);
        relation.setFriendId(request.getFriendId());
        relation.setGroupId("1");
        relation.setStatus(0);
        relation.setApplyRemark(request.getApplyRemark());
        relation.setCreateTime(LocalDateTime.now());
        relation.setUpdateTime(LocalDateTime.now());

        friendMapper.insert(relation);

        return relation;
    }

    /**
     * 处理好友申请
     *
     * @param userId
     * @param requestId
     * @param status
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleFriendRequest(Long userId, Long requestId, Integer status) {
        log.info("处理好友申请: userId={}, requestId={}, status={}", userId, requestId, status);

        //查询目标申请条
        FriendRelation relation = friendMapper.selectById(requestId);
        //不存在
        if (relation == null) {
            throw new BusinessException("好友申请不存在");
        }

        //我不是目标好友用户
        if (!relation.getFriendId().equals(userId)) {
            throw new BusinessException("无权处理此好友申请");
        }

        //申请已处理
        if (relation.getStatus() != 0) {
            throw new BusinessException("该申请已处理");
        }

        //处理
        if (status == 1) {
            //同意（添加双向记录）
            relation.setStatus(1);
            relation.setUpdateTime(LocalDateTime.now());
            friendMapper.updateById(relation);

            //创建双向记录
            FriendRelation reverseRelation = new FriendRelation();
            reverseRelation.setUserId(userId);//当前用户
            reverseRelation.setFriendId(relation.getUserId());//对方用户
            reverseRelation.setGroupId("1");//默认分组
            reverseRelation.setStatus(1);//状态已同意
            reverseRelation.setApplyRemark(relation.getApplyRemark());//备注
            reverseRelation.setCreateTime(LocalDateTime.now());//创建时间
            reverseRelation.setUpdateTime(LocalDateTime.now());//更新时间
            friendMapper.insert(reverseRelation);

            log.info("同意好友申请并创建双向记录: requestId={}", requestId);
        } else if (status == 2) {
            //拒绝(保持单条记录)
            relation.setStatus(2);//改状态
            relation.setUpdateTime(LocalDateTime.now());//更新时间
            friendMapper.updateById(relation);//更新数据库
            log.info("拒绝好友申请: requestId={}", requestId);
        } else {
            //无效状态值
            throw new BusinessException("无效的处理状态");
        }
    }

    /**
     * 获取收到的好友申请列表
     *
     * @param userId
     * @param status
     * @param sortOrder
     * @param page
     * @param size
     * @return
     */
    @Override
    public Page<FriendRequestVO> getReceivedFriendRequests(Long userId, Integer status, String sortOrder, Integer page, Integer size) {
        log.info("获取收到的好友申请列表: userId={}, status={}", userId, status);

        //创建分页对象
        Page<FriendRelation> relationPage = new Page<>(page, size);

        //编写查询条件
        LambdaQueryWrapper<FriendRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRelation::getFriendId, userId);//获取当前用户的所有申请(对方的好友ID是当前用户ID)

        //添加状态条件
        if (status != null) {
            wrapper.eq(FriendRelation::getStatus, status);
        }

        //添加排序条件
        if ("asc".equalsIgnoreCase(sortOrder)) {
            wrapper.orderByAsc(FriendRelation::getCreateTime);
        } else {
            wrapper.orderByDesc(FriendRelation::getCreateTime);
        }

        //执行查询
        Page<FriendRelation> pageResult = friendMapper.selectPage(relationPage, wrapper);

        //转换为VO
        List<FriendRequestVO> voList = pageResult.getRecords().stream()
                .map(relation -> {
                    User applicant = userMapper.selectById(relation.getUserId());//查询申请人信息
                    String username = applicant != null ? applicant.getUsername() : "未知用户";//申请人名称
                    String avatarUrl = applicant != null ? applicant.getAvatarUrl() : null;//申请人头像
                    return FriendRequestVO.fromEntity(relation, username, avatarUrl);
                })
                .toList();

        //创建分页对象
        Page<FriendRequestVO> voPage = new Page<>(page, size);
        //设置记录
        voPage.setRecords(voList);
        //设置总数
        voPage.setTotal(pageResult.getTotal());

        return voPage;
    }

    /**
     * 获取好友列表
     *
     * @param userId
     * @param groupId
     * @param page
     * @param size
     * @return
     */
    @Override
    public Page<FriendVO> getFriends(Long userId, Long groupId, Integer page, Integer size) {
        log.info("获取好友列表: userId={}, groupId={}", userId, groupId);

        //创建分页对象
        Page<FriendRelation> relationPage = new Page<>(page, size);

        //编写查询条件
        LambdaQueryWrapper<FriendRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRelation::getUserId, userId)//获取当前用户的所有好友(我的ID是当前用户ID)
                .eq(FriendRelation::getStatus, 1);//状态已同意

        //添加分组条件
        if (groupId != null) {
            wrapper.eq(FriendRelation::getGroupId, String.valueOf(groupId));
        }

        //添加排序条件(默认最新更新时间排序)
        wrapper.orderByDesc(FriendRelation::getUpdateTime);

        //执行查询
        Page<FriendRelation> pageResult = friendMapper.selectPage(relationPage, wrapper);

        //转换为VO
        List<FriendVO> voList = pageResult.getRecords().stream()
                .map(relation -> {
                    User friend = userMapper.selectById(relation.getFriendId());//查询好友信息
                    //好友不存在
                    if (friend == null) {
                        return null;
                    }

                    //获取分组名称(没有添加就默认)
                    String groupName = "默认分组";
                    if (relation.getGroupId() != null && !relation.getGroupId().equals("1")) {
                        //查询组信息
                        Group group = groupMapper.selectById(Long.parseLong(relation.getGroupId()));//转换数据类型为Long
                        //分组存在
                        if (group != null) {
                            //获取分组名称
                            groupName = group.getGroupName();
                        }
                    }

                    //转换为VO返回
                    return FriendVO.fromEntity(friend, groupName, relation.getUpdateTime());
                })
                .filter(vo -> vo != null)//过滤掉null的VO
                .toList();

        //创建分页对象
        Page<FriendVO> voPage = new Page<>(page, size);
        //设置记录
        voPage.setRecords(voList);
        //设置总数
        voPage.setTotal(pageResult.getTotal());

        return voPage;
    }

    /**
     * 更新指定好友的分组
     *
     * @param userId
     * @param friendId
     * @param groupId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFriendGroup(Long userId, Long friendId, String groupId) {
        log.info("更新好友分组: userId={}, friendId={}, groupId={}", userId, friendId, groupId);

        //编写查询条件
        LambdaQueryWrapper<FriendRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRelation::getUserId, userId)//关系主动方是当前用户ID
                .eq(FriendRelation::getFriendId, friendId)//对方ID是好友用户ID
                .eq(FriendRelation::getStatus, 1);//状态已同意

        //查询我是主动方的单向好友关系
        FriendRelation relation = friendMapper.selectOne(wrapper);
        //好友关系不存在
        if (relation == null) {
            throw new BusinessException("好友关系不存在");
        }

        //判断新分组ID(如果不是默认分组)
        if (!"1".equals(groupId)) {
            //查询分组
            Group group = groupMapper.selectById(Long.parseLong(groupId));
            if (group == null || !group.getUserId().equals(userId)) {
                throw new BusinessException("分组不存在");
            }
        }

        //更新
        relation.setGroupId(groupId);
        relation.setUpdateTime(LocalDateTime.now());
        //更新数据库信息
        friendMapper.updateById(relation);

        log.info("好友分组更新成功: friendId={}, groupId={}", friendId, groupId);
    }

    /**
     * 删除好友
     *
     * @param userId 用户ID
     * @param friendId 好友ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFriend(Long userId, Long friendId) {
        log.info("删除好友: userId={}, friendId={}", userId, friendId);

        //编写查询条件
        LambdaQueryWrapper<FriendRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(FriendRelation::getUserId, userId)
                        .eq(FriendRelation::getFriendId, friendId)
                        .or()
                        .eq(FriendRelation::getUserId, friendId)
                        .eq(FriendRelation::getFriendId, userId))
                .eq(FriendRelation::getStatus, 1);

        //查询双向的好友关系
        List<FriendRelation> relations = friendMapper.selectList(wrapper);
        if (relations.isEmpty()) {
            throw new BusinessException("好友关系不存在");
        }

        //增强for循环改列表里的元素数据
        for (FriendRelation relation : relations) {
            relation.setStatus(3);//删除
            relation.setUpdateTime(LocalDateTime.now());
            friendMapper.updateById(relation);
        }

        log.info("好友删除成功: friendId={}", friendId);

    }

    /**
     * 创建好友分组
     *
     * @param userId 用户ID
     * @param groupName 分组名称
     * @return 分组信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Group createFriendGroup(Long userId, String groupName) {
        log.info("创建好友分组: userId={}, groupName={}", userId, groupName);

        //编写查询条件
        LambdaQueryWrapper<Group> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Group::getUserId, userId)//分组创建者ID是当前用户ID
                .eq(Group::getGroupName, groupName);//分组名称是传入的分组名称

        //分组名称已存在
        Group existingGroup = groupMapper.selectOne(wrapper);
        if (existingGroup != null) {
            throw new BusinessException("分组名称已存在");
        }

        //创建分组
        Group group = new Group();
        group.setUserId(userId);
        group.setGroupName(groupName);
        group.setCreateTime(LocalDateTime.now());

        groupMapper.insert(group);

        log.info("好友分组创建成功: groupId={}, groupName={}", group.getId(), groupName);
        return group;
    }

    /**
     * 获取好友分组列表
     *
     * @param userId 用户ID
     * @param page 页码
     * @param size 页大小
     * @return 好友分组列表
     * 前端加上默认分组
     */
    @Override
    public Page<Group> getFriendGroups(Long userId, Integer page, Integer size) {
        log.info("获取好友分组列表: userId={}", userId);

        //创建分页对象
        Page<Group> groupPage = new Page<>(page, size);

        //编写查询条件
        LambdaQueryWrapper<Group> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Group::getUserId, userId)//分组创建者ID是当前用户ID
                .orderByDesc(Group::getCreateTime);//按创建时间降序

        //执行查询
        return groupMapper.selectPage(groupPage, wrapper);
    }

    /**
     * 删除好友分组
     *
     * @param userId 用户ID
     * @param groupId 分组ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFriendGroup(Long userId, Long groupId) {
        log.info("删除好友分组: userId={}, groupId={}", userId, groupId);

        //默认分组不能删除
        if (groupId == 1) {
            throw new BusinessException("默认分组不能删除");
        }

        //查询分组信息
        Group group = groupMapper.selectById(groupId);
        if (group == null || !group.getUserId().equals(userId)) {
            throw new BusinessException("分组不存在");
        }

        //编写查询条件
        LambdaQueryWrapper<FriendRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRelation::getUserId, userId)//关系主动方是当前用户ID
                .eq(FriendRelation::getGroupId, String.valueOf(groupId));//分组ID

        //查询该分组下的所有好友关系
        List<FriendRelation> relations = friendMapper.selectList(wrapper);
        //增强for循环改列表里的元素数据
        for (FriendRelation relation : relations) {
            relation.setGroupId("1");//默认分组
            relation.setUpdateTime(LocalDateTime.now());
            friendMapper.updateById(relation);
        }

        //删除分组
        groupMapper.deleteById(groupId);

        log.info("好友分组删除成功: groupId={}", groupId);
    }

    /**
     * 获取好友详情
     *
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 好友详情
     */
    @Override
    public FriendVO getFriendDetail(Long userId, Long friendId) {
        log.info("获取好友详情: userId={}, friendId={}", userId, friendId);

        //编写查询条件
        LambdaQueryWrapper<FriendRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRelation::getUserId, userId)//关系主动方是当前用户ID
                .eq(FriendRelation::getFriendId, friendId)//关系被动方是传入的好友ID
                .eq(FriendRelation::getStatus, 1);//状态是已通过

        //查询好友关系
        FriendRelation relation = friendMapper.selectOne(wrapper);
        if (relation == null) {
            throw new BusinessException("好友关系不存在");
        }

        //查询好友信息
        User friend = userMapper.selectById(friendId);
        if (friend == null) {
            throw new BusinessException("好友不存在");
        }

        //获取好友分组名称
        String groupName = "默认分组";
        if (relation.getGroupId() != null && !relation.getGroupId().equals("1")) {
            Group group = groupMapper.selectById(Long.parseLong(relation.getGroupId()));
            if (group != null) {
                groupName = group.getGroupName();
            }
        }

        //构建好友详情VO
        return FriendVO.fromEntity(friend, groupName, relation.getUpdateTime());
    }

}
