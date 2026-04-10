package com.smartnote.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartnote.dto.friend.*;
import com.smartnote.entity.FriendRelation;
import com.smartnote.entity.Group;
import com.smartnote.service.FriendService;
import com.smartnote.util.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/friends")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    /**
     * 发送好友申请
     * POST /api/friends/requests
     * 向指定用户发送好友申请
     */
    @PostMapping("/requests")
    public Result<FriendRelation> sendFriendRequest(@RequestAttribute Long userId,
                                                    @Valid @RequestBody FriendRequest request) {
        log.info("发送好友申请: userId={}, friendId={}, remark={}",
                userId, request.getFriendId(), request.getApplyRemark());
        FriendRelation relation = friendService.sendFriendRequest(userId, request);
        return Result.success("好友申请已发送", relation);
    }

    /**
     * 处理好友申请
     * PUT /api/friends/requests/{requestId}
     * 同意或拒绝好友申请
     *
     * @param userId 当前用户ID
     * @param requestId 好友申请ID
     */
    @PutMapping("/requests/{requestId}")
    public Result<String> handleFriendRequest(@RequestAttribute Long userId,
                                              @PathVariable Long requestId,
                                              @Valid @RequestBody HandleFriendRequest request) {
        log.info("处理好友申请: userId={}, requestId={}, status={}",
                userId, requestId, request.getStatus());
        friendService.handleFriendRequest(userId, requestId, request.getStatus());
        String message = request.getStatus() == 1 ? "好友申请已同意" : "好友申请已拒绝";
        return Result.success(message);
    }

    /**
     * 获取收到的好友申请列表
     * GET /api/friends/requests/received
     * 获取当前用户收到的好友申请
     * 支持按状态筛选，按时间排序
     */
    @GetMapping("/requests/received")
    public Result<Page<FriendRequestVO>> getReceivedFriendRequests(@RequestAttribute Long userId,
                                                                   @RequestParam(required = false) Integer status,
                                                                   @RequestParam(defaultValue = "desc") String sortOrder,
                                                                   @RequestParam(defaultValue = "1") Integer page,
                                                                   @RequestParam(defaultValue = "20") Integer size) {
        log.info("获取好友申请列表: userId={}, status={}, page={}, size={}",
                userId, status, page, size);
        Page<FriendRequestVO> requests = friendService.getReceivedFriendRequests(userId, status, sortOrder, page, size);
        return Result.success(requests);
    }

    /**
     * 获取好友列表
     * GET /api/friends
     * 获取当前用户的所有好友，支持分组筛选
     */
    @GetMapping
    public Result<Page<FriendVO>> getFriends(@RequestAttribute Long userId,
                                             @RequestParam(required = false) Long groupId,
                                             @RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "20") Integer size) {
        log.info("获取好友列表: userId={}, groupId={}, page={}, size={}",
                userId, groupId, page, size);
        Page<FriendVO> friends = friendService.getFriends(userId, groupId, page, size);
        return Result.success(friends);
    }

    /**
     * 更新好友分组
     * PATCH /api/friends/{friendId}/group
     * 修改指定好友的分组
     */
    @PatchMapping("/{friendId}/group")
    public Result<String> updateFriendGroup(@RequestAttribute Long userId,
                                            @PathVariable Long friendId,
                                            @Valid @RequestBody UpdateGroupRequest request) {
        log.info("更新好友分组: userId={}, friendId={}, groupId={}",
                userId, friendId, request.getGroupId());
        friendService.updateFriendGroup(userId, friendId, request.getGroupId());
        return Result.success("好友分组已更新");
    }

    /**
     * 删除好友
     * DELETE /api/friends/{friendId}
     * 删除好友关系（更新状态为已删除）
     */
    @DeleteMapping("/{friendId}")
    public Result<String> deleteFriend(@RequestAttribute Long userId,
                                       @PathVariable Long friendId) {
        log.info("删除好友: userId={}, friendId={}", userId, friendId);
        friendService.deleteFriend(userId, friendId);
        return Result.success("好友已删除");
    }

    /**
     * 创建好友分组
     * POST /api/friends/groups
     * 创建新的好友分组
     * 修改：返回Group实体
     */
    @PostMapping("/groups")
    public Result<Group> createFriendGroup(@RequestAttribute Long userId,
                                           @Valid @RequestBody CreateGroupRequest request) {
        log.info("创建好友分组: userId={}, groupName={}", userId, request.getGroupName());
        Group group = friendService.createFriendGroup(userId, request.getGroupName());
        return Result.success("分组创建成功", group);
    }

    /**
     * 获取好友分组列表
     * GET /api/friends/groups
     * 获取当前用户的所有好友分组
     */
    @GetMapping("/groups")
    public Result<Page<Group>> getFriendGroups(@RequestAttribute Long userId,
                                               @RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "20") Integer size) {
        log.info("获取好友分组列表: userId={}, keyword={}, page={}, size={}", userId, page, size);
        Page<Group> groups = friendService.getFriendGroups(userId, page, size);
        return Result.success(groups);
    }

    /**
     * 删除好友分组
     * DELETE /api/friends/groups/{groupId}
     * 删除好友分组（组内好友重新分组到默认分组）
     */
    @DeleteMapping("/groups/{groupId}")
    public Result<String> deleteFriendGroup(@RequestAttribute Long userId,
                                            @PathVariable Long groupId) {
        log.info("删除好友分组: userId={}, groupId={}",
                userId, groupId);
        friendService.deleteFriendGroup(userId, groupId);
        return Result.success("分组删除成功");
    }

    /**
     * 获取好友详情
     * GET /api/friends/{friendId}
     * 获取指定好友的详细信息
     *///111
    @GetMapping("/{friendId}")
    public Result<FriendVO> getFriendDetail(@RequestAttribute Long userId,
                                            @PathVariable Long friendId) {
        log.info("获取好友详情: userId={}, friendId={}", userId, friendId);
        FriendVO friend = friendService.getFriendDetail(userId, friendId);
        return Result.success(friend);
    }
}
