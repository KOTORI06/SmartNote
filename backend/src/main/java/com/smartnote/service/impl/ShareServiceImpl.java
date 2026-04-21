package com.smartnote.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartnote.constant.ShareType;
import com.smartnote.dto.share.*;
import com.smartnote.entity.Note;
import com.smartnote.entity.NotePermission;
import com.smartnote.entity.User;
import com.smartnote.exception.BusinessException;
import com.smartnote.mapper.*;
import com.smartnote.service.ShareService;
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
public class ShareServiceImpl implements ShareService {

    private final ShareMapper shareMapper;
    private final NoteMapper noteMapper;
    private final UserMapper userMapper;

    /**
     * 创建/更新分享权限
     *
     * @param userId       用户ID
     * @param request      创建/更新权限请求参数
     * @return             创建/更新后的权限信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotePermission createOrUpdatePermission(Long userId, CreatePermissionRequest request) {
        //获取笔记
        Note note = noteMapper.selectById(request.getNoteId());

        //检查笔记是否存在
        if (note == null || note.getIsDeleted() == 1) {
            throw new BusinessException("笔记不存在");
        }

        //检查用户权限(实现用户隔离)
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException("只有笔记所有者才能设置分享权限");
        }

        //检查被授权者类型
        if (request.getGranteeType() == 1 && request.getGranteeId() == null) {
            throw new BusinessException("被授权者为用户时，必须指定用户ID");
        }

        //检查被授权者是否存在
        if (request.getGranteeType() == 1) {
            User granteeUser = userMapper.selectById(request.getGranteeId());
            if (granteeUser == null) {
                throw new BusinessException("被授权用户不存在");
            }
        }

        LambdaQueryWrapper<NotePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotePermission::getNoteId, request.getNoteId())//笔记ID
                .eq(NotePermission::getGranteeType, request.getGranteeType());//被授权者类型

        //如果是用户授权，则检查被授权者ID
        if (request.getGranteeType() == 1) {
            wrapper.eq(NotePermission::getGranteeId, request.getGranteeId());
        } else {
            //当 granteeType = 2（分享给所有人）时，granteeId 应该为空（NULL）
            wrapper.isNull(NotePermission::getGranteeId);
        }

        //检查已存在的权限
        NotePermission existingPermission = shareMapper.selectOne(wrapper);

        NotePermission permission;
        //如果已存在权限，则更新
        if (existingPermission != null) {
            existingPermission.setPermissionType(request.getPermissionType());
            existingPermission.setUpdateTime(LocalDateTime.now());
            shareMapper.updateById(existingPermission);
            permission = existingPermission;
            log.info("更新分享权限: permissionId={}, noteId={}", permission.getId(), request.getNoteId());
        } else {
            //如果不存在权限，则创建
            permission = new NotePermission();
            permission.setNoteId(request.getNoteId());
            permission.setOwnerId(userId);
            permission.setGranteeType(request.getGranteeType());
            permission.setGranteeId(request.getGranteeType() == 1 ? request.getGranteeId() : null);
            permission.setPermissionType(request.getPermissionType());
            permission.setCreateTime(LocalDateTime.now());
            permission.setUpdateTime(LocalDateTime.now());
            shareMapper.insert(permission);
            log.info("创建分享权限: permissionId={}, noteId={}", permission.getId(), request.getNoteId());
        }

        return permission;
    }

    /**
     * 获取笔记的分享权限列表
     *
     * @param userId   用户ID
     * @param noteId   笔记ID
     * @return         分享权限列表
     */
    @Override
    public List<PermissionVO> getNotePermissions(Long userId, Long noteId) {
        //根据笔记ID获取笔记
        Note note = noteMapper.selectById(noteId);

        //检查笔记是否存在
        if (note == null || note.getIsDeleted() == 1) {
            throw new BusinessException("笔记不存在");
        }

        //检查用户权限(实现用户隔离)
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException("只有笔记所有者才能查看权限列表");
        }

        //获取分享权限列表
        LambdaQueryWrapper<NotePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotePermission::getNoteId, noteId)//笔记ID
                .orderByDesc(NotePermission::getCreateTime);//按创建时间倒序

        //查询该笔记的全部分享权限
        List<NotePermission> permissions = shareMapper.selectList(wrapper);

        //转换为VO
        return permissions.stream()
                .map(permission -> PermissionVO.fromEntity(permission, note.getTitle()))
                .toList();
    }

    /**
     * 获取分享给我的笔记列表
     *
     * @param userId       用户ID
     * @param permissionType  权限类型
     * @param page         页码
     * @param size         页大小
     * @return             分享的笔记列表
     */
    @Override
    public Page<SharedNoteVO> getSharedNotes(Long userId, Integer permissionType, Integer page, Integer size) {
        //创建分页对象
        Page<NotePermission> permissionPage = new Page<>(page, size);

        LambdaQueryWrapper<NotePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(NotePermission::getGranteeType, ShareType.GRANTEE_TYPE_ALL)//所有人分享
                        .or()
                        .eq(NotePermission::getGranteeId, userId))//分享给我的
                .orderByDesc(NotePermission::getCreateTime);

        //如果有权限类型，则检查权限类型
        if (permissionType != null) {
            wrapper.eq(NotePermission::getPermissionType, permissionType);
        }

        //执行查询
        Page<NotePermission> resultPage = shareMapper.selectPage(permissionPage, wrapper);

        //转换为VO
        Page<SharedNoteVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());

        List<SharedNoteVO> voList = resultPage.getRecords().stream()
                .map(permission -> {
                    Note note = noteMapper.selectById(permission.getNoteId());//获取笔记完整信息
                    //过滤掉已删除的笔记
                    if (note == null || note.getIsDeleted() == 1) {
                        return null;
                    }

                    //获取笔记所有者信息
                    User owner = userMapper.selectById(note.getUserId());
                    //获取笔记所有者名称
                    String ownerName = owner != null ? owner.getUsername() : "未知用户";

                    return SharedNoteVO.fromEntity(note, ownerName, permission);
                })
                .filter(vo -> vo != null)//过滤掉null的元素(已删除的笔记)
                .toList();

        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * 删除分享权限
     *
     * @param userId       用户ID
     * @param permissionId 权限ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermission(Long userId, Long permissionId) {
        //获取权限记录
        NotePermission permission = shareMapper.selectById(permissionId);

        if (permission == null) {
            throw new BusinessException("权限记录不存在");
        }

        if (!permission.getOwnerId().equals(userId)) {
            throw new BusinessException("只有权限所有者才能删除该权限");
        }

        shareMapper.deleteById(permissionId);
        log.info("删除分享权限: permissionId={}, userId={}", permissionId, userId);
    }

    /**
     * 检查用户对笔记的权限
     *
     * @param userId   用户ID
     * @param noteId   笔记ID
     * @return         权限检查结果
     */
    @Override
    public PermissionCheckVO checkPermission(Long userId, Long noteId) {
        //根据笔记ID获取笔记
        Note note = noteMapper.selectById(noteId);

        //检查笔记是否存在
        if (note == null || note.getIsDeleted() == 1) {
            return PermissionCheckVO.denied("笔记不存在");
        }

        //检查用户权限(实现用户隔离)
        if (note.getUserId().equals(userId)) {
            return PermissionCheckVO.granted(2, "笔记所有者，拥有完全权限");
        }

        //编辑查询条件
        LambdaQueryWrapper<NotePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotePermission::getNoteId, noteId)//笔记ID
                .and(w -> w.eq(NotePermission::getGranteeType, ShareType.GRANTEE_TYPE_ALL)//所有人分享
                        .or()
                        .eq(NotePermission::getGranteeId, userId))//分享给我的
                .orderByDesc(NotePermission::getPermissionType);

        //执行查询
        NotePermission permission = shareMapper.selectOne(wrapper);

        //检查权限
        if (permission != null) {
            String message = permission.getPermissionType() == 1 ? "拥有查看权限" : "拥有编辑权限";
            return PermissionCheckVO.granted(permission.getPermissionType(), message);
        }

        return PermissionCheckVO.denied("没有访问权限");
    }

    /**
     * 获取公开分享的笔记信息
     *
     * @param noteId   笔记ID
     * @return         公开分享的笔记信息
     */
    @Override
    public PublicNoteVO getPublicNote(Long noteId) {
        //通过笔记ID获取笔记
        Note note = noteMapper.selectById(noteId);

        //检查笔记是否存在
        if (note == null || note.getIsDeleted() == 1) {
            throw new BusinessException("笔记不存在");
        }

        //检查笔记是否公开分享
        LambdaQueryWrapper<NotePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotePermission::getNoteId, noteId)//笔记ID
                .eq(NotePermission::getGranteeType, ShareType.GRANTEE_TYPE_ALL);//所有人分享

        //执行查询
        NotePermission permission = shareMapper.selectOne(wrapper);

        //检查权限
        if (permission == null) {
            throw new BusinessException("该笔记未公开分享");
        }

        return PublicNoteVO.fromEntity(note);
    }
}
