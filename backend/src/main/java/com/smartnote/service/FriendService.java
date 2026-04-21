package com.smartnote.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartnote.dto.friend.FriendRequest;
import com.smartnote.dto.friend.FriendRequestVO;
import com.smartnote.dto.friend.FriendVO;
import com.smartnote.entity.FriendRelation;
import com.smartnote.entity.Group;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public interface FriendService {
    /**
     * 发送好友申请
     *
     * @param userId       用户ID
     * @param request      好友申请信息
     * @return             发送成功后的好友关系信息
     */
    FriendRelation sendFriendRequest(Long userId, FriendRequest request);

    /**
     * 处理好友申请
     * PUT /api/friends/requests/{requestId}
     * 同意或拒绝好友申请
     *
     * @param userId 当前用户ID
     * @param requestId 好友申请行ID
     */
    void handleFriendRequest(Long userId, Long requestId, Integer status);

    /**
     * 获取收到的好友申请列表
     *
     * @param userId       用户ID
     * @param status       状态
     * @param sortOrder    排序顺序（asc: 升序 0: 降序）
     * @param page         页码
     * @param size         页大小
     * @return             收到的好友申请列表
     */
    Page<FriendRequestVO> getReceivedFriendRequests(Long userId, Integer status, String sortOrder, Integer page, Integer size);

    /**
     * 获取好友列表
     *
     * @param userId       用户ID
     * @param groupId      分组ID
     * @param page         页码
     * @param size         页大小
     * @return             好友列表
     */
    Page<FriendVO> getFriends(Long userId, Long groupId, Integer page, Integer size);

    /**
     * 更新指定好友的分组
     * 修改指定好友的分组
     *
     * @param userId       用户ID
     * @param friendId     好友ID
     * @param groupId      分组ID
     */
    void updateFriendGroup(Long userId, Long friendId, String groupId);

    /**
     * 删除好友
     *
     * @param userId       用户ID
     * @param friendId     好友ID
     */
    void deleteFriend(Long userId, Long friendId);

    /**创建新好友分组
     *
     * @param userId
     * @param groupName
     * @return
     */
    Group createFriendGroup(Long userId, String groupName);

    /**
     * 获取好友分组列表
     *
     * @param userId       用户ID
     * @param page         页码
     * @param size         页大小
     * @return             好友分组列表
     */
    Page<Group> getFriendGroups(Long userId, Integer page, Integer size);

    /**
     * 删除好友分组
     *
     * @param userId       用户ID
     * @param groupId      分组ID
     */
    void deleteFriendGroup(Long userId, Long groupId);

    /**
     * 获取好友详情
     *
     * @param userId       用户ID
     * @param friendId     好友ID
     * @return             好友详情
     */
    FriendVO getFriendDetail(Long userId, Long friendId);
}
