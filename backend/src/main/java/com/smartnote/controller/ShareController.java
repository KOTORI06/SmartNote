package com.smartnote.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartnote.dto.share.*;
import com.smartnote.entity.NotePermission;
import com.smartnote.service.ShareService;
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
@RequestMapping("/api/shares")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    /**
     * 创建/更新分享权限
     * POST /api/shares
     * 为笔记设置或更新分享权限
     * 支持设置给单个用户或所有人
     */
    @PostMapping
    public Result<NotePermission> createOrUpdatePermission(@RequestAttribute Long userId,
                                                           @Valid @RequestBody CreatePermissionRequest request) {
        log.info("创建分享权限: userId={}, noteId={}, granteeType={}, permissionType={}",
                userId, request.getNoteId(), request.getGranteeType(), request.getPermissionType());
        NotePermission permission = shareService.createOrUpdatePermission(userId, request);
        return Result.success("权限设置成功", permission);
    }

    /**
     * 获取笔记的分享权限列表
     * GET /api/shares/notes/{noteId}
     * 获取某条笔记的所有分享权限设置
     */
    @GetMapping("/notes/{noteId}")
    public Result<List<PermissionVO>> getNotePermissions(@RequestAttribute Long userId,
                                                         @PathVariable Long noteId) {
        log.info("获取笔记权限列表: userId={}, noteId={}", userId, noteId);
        List<PermissionVO> permissions = shareService.getNotePermissions(userId, noteId);
        return Result.success(permissions);
    }

    /**
     * 获取分享给我的笔记
     * GET /api/shares/shared-to-me
     * 获取其他用户分享给当前用户的所有笔记
     */
    @GetMapping("/shared-to-me")
    public Result<Page<SharedNoteVO>> getSharedNotes(@RequestAttribute Long userId,
                                                     @RequestParam(required = false) Integer permissionType,
                                                     @RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "20") Integer size) {
        log.info("获取分享给我的笔记: userId={}, permissionType={}, page={}, size={}",
                userId, permissionType, page, size);
        Page<SharedNoteVO> sharedNotes = shareService.getSharedNotes(userId, permissionType, page, size);
        return Result.success(sharedNotes);
    }


    /**
     * 删除分享权限
     * DELETE /api/shares/{permissionId}
     * 删除（收回）特定的分享权限
     */
    @DeleteMapping("/{permissionId}")
    public Result<String> deletePermission(@RequestAttribute Long userId,
                                           @PathVariable Long permissionId) {
        log.info("删除分享权限: userId={}, permissionId={}", userId, permissionId);
        shareService.deletePermission(userId, permissionId);
        return Result.success("权限已删除");
    }

    /**
     * 检查当前用户对笔记的访问权限
     * GET /api/shares/check-permission/{noteId}
     * 检查当前用户对指定笔记的访问权限
     */
    @GetMapping("/check-permission/{noteId}")
    public Result<PermissionCheckVO> checkPermission(@RequestAttribute Long userId,
                                                     @PathVariable Long noteId) {
        log.info("检查笔记访问权限: userId={}, noteId={}", userId, noteId);
        PermissionCheckVO result = shareService.checkPermission(userId, noteId);
        return Result.success(result);
    }

    /**
     * 获取公开笔记
     * GET /api/shares/public/{noteId}
     * 获取公开分享的笔记（无需登录）
     */
    @GetMapping("/public/{noteId}")
    public Result<PublicNoteVO> getPublicNote(@PathVariable Long noteId) {
        log.info("获取公开笔记: noteId={}", noteId);
        PublicNoteVO note = shareService.getPublicNote(noteId);
        return Result.success(note);
    }
}
