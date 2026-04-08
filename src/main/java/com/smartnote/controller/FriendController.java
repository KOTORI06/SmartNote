package com.smartnote.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartnote.dto.friend.*;
import com.smartnote.entity.FriendRelation;
import com.smartnote.service.FriendService;
import com.smartnote.util.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     */
    @GetMapping("/requests/received")
    public Result<Page<FriendRequestVO>> getReceivedFriendRequests(@RequestAttribute Long userId,
                                                                   @RequestParam(required = false) Integer status,
                                                                   @RequestParam(defaultValue = "1") Integer page,
                                                                   @RequestParam(defaultValue = "20") Integer size) {
        log.info("获取好友申请列表: userId={}, status={}, page={}, size={}",
                userId, status, page, size);
        Page<FriendRequestVO> requests = friendService.getReceivedFriendRequests(userId, status, page, size);
        return Result.success(requests);
    }

    /**
     * 获取我发送的好友申请列表
     * GET /api/friends/requests/sent
     * 获取当前用户发送的好友申请
     */
    @GetMapping("/requests/sent")
    public Result<Page<FriendRequestVO>> getSentFriendRequests(@RequestAttribute Long userId,
                                                               @RequestParam(defaultValue = "1") Integer page,
                                                               @RequestParam(defaultValue = "20") Integer size) {
        log.info("获取已发送好友申请: userId={}, page={}, size={}", userId, page, size);
        Page<FriendRequestVO> requests = friendService.getSentFriendRequests(userId, page, size);
        return Result.success(requests);
    }

    /**
     * 获取好友列表
     * GET /api/friends
     * 获取当前用户的所有好友，可按分组筛选
     */
    @GetMapping
    public Result<List<FriendVO>> getFriends(@RequestAttribute Long userId,
                                             @RequestParam(required = false) String groupName) {
        log.info("获取好友列表: userId={}, groupName={}", userId, groupName);
        List<FriendVO> friends = friendService.getFriends(userId, groupName);
        return Result.success(friends);
    }

    /**
     * 获取好友分组列表
     * GET /api/friends/groups
     * 获取当前用户的所有好友分组
     */
    @GetMapping("/groups")
    public Result<List<String>> getFriendGroups(@RequestAttribute Long userId) {
        log.info("获取好友分组: userId={}", userId);
        List<String> groups = friendService.getFriendGroups(userId);
        return Result.success(groups);
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
        log.info("更新好友分组: userId={}, friendId={}, groupName={}",
                userId, friendId, request.getGroupName());
        friendService.updateFriendGroup(userId, friendId, request.getGroupName());
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
     * 获取好友详情
     * GET /api/friends/{friendId}
     * 获取指定好友的详细信息
     */
    @GetMapping("/{friendId}")
    public Result<FriendVO> getFriendDetail(@RequestAttribute Long userId,
                                            @PathVariable Long friendId) {
        log.info("获取好友详情: userId={}, friendId={}", userId, friendId);
        FriendVO friend = friendService.getFriendDetail(userId, friendId);
        return Result.success(friend);
    }
}
