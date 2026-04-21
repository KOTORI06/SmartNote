package com.smartnote.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartnote.dto.share.*;
import com.smartnote.entity.NotePermission;
import jakarta.validation.Valid;

import java.util.List;

public interface ShareService {

    /**
     * 创建/更新分享权限
     *
     * @param userId       用户ID
     * @param request      创建/更新权限请求参数
     * @return             创建/更新后的权限信息
     */
    NotePermission createOrUpdatePermission(Long userId, @Valid CreatePermissionRequest request);

    /**
     * 获取笔记的分享权限列表
     *
     * @param userId       用户ID
     * @param noteId       笔记ID
     * @return             笔记的分享权限列表
     */
    List<PermissionVO> getNotePermissions(Long userId, Long noteId);

    /**
     * 获取分享给我的笔记
     *
     * @param userId       用户ID
     * @param permissionType  权限类型
     * @param page         页码
     * @param size         页大小
     * @return             分享给我的笔记列表
     */
    Page<SharedNoteVO> getSharedNotes(Long userId, Integer permissionType, Integer page, Integer size);

    /**
     * 删除分享权限
     *
     * @param userId       用户ID
     * @param permissionId 权限ID
     */
    void deletePermission(Long userId, Long permissionId);

    /**
     * 检查笔记访问权限
     *
     * @param userId       用户ID
     * @param noteId       笔记ID
     * @return             笔记访问权限信息
     */
    PermissionCheckVO checkPermission(Long userId, Long noteId);

    /**
     * 获取公开笔记
     *
     * @param noteId       笔记ID
     * @return             公开笔记信息
     */
    PublicNoteVO getPublicNote(Long noteId);
}
