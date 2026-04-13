package com.smartnote.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartnote.dto.ai.AiAnalysisVO;
import com.smartnote.dto.note.*;
import com.smartnote.entity.*;
import com.smartnote.exception.BusinessException;
import com.smartnote.mapper.*;
import com.smartnote.service.NoteService;
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
public class NoteServiceImpl implements NoteService {

    private final NoteMapper noteMapper;
    private final NoteTagMapper noteTagMapper;
    private final NoteViewHistoryMapper viewHistoryMapper;
    private final AiMapper aiMapper;
    private final ShareMapper shareMapper;
    private final TagMapper tagMapper;

    /**
     * 创建新笔记
     *
     * @param userId 用户ID
     * @param request 创建笔记的请求参数
     * @return 创建成功的笔记对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Note createNote(Long userId, CreateNoteRequest request) {
        // 创建笔记对象
        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setFolderId(request.getFolderId());
        note.setIsDeleted(0);
        note.setCreateTime(LocalDateTime.now());
        note.setUpdateTime(LocalDateTime.now());

        noteMapper.insert(note);

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            saveNoteTags(note.getId(), request.getTagIds());
        }

        log.info("笔记创建成功: noteId={}, userId={}", note.getId(), userId);
        return note;
    }

    /**
     * 获取笔记列表
     *
     * @param userId 用户ID
     * @param title 笔记标题
     * @param tagId 标签ID
     * @param folderId 文件夹ID
     * @param page 页码
     * @param size 页大小
     * @param order 排序方式
     * @return 笔记列表
     */
    @Override
    public Page<NoteVO> getNotes(Long userId,
                                 String title,
                                 Long tagId,
                                 Long folderId,
                                 Integer page,
                                 Integer size,
                                 String order) {
        // 如果tagId不为空，则根据标签ID查询
        if (tagId != null) {
            return getNotesByTag(userId, tagId, page, size, order);
        }

        //创建分页对象
        Page<Note> notePage = new Page<>(page, size);

        //编写查询条件
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getUserId, userId)
                .eq(Note::getIsDeleted, 0);

        //标题模糊查询
        if (title != null && !title.trim().isEmpty()) {
            wrapper.like(Note::getTitle, title);
        }

        //文件夹ID查询
        if (folderId != null) {
            wrapper.eq(Note::getFolderId, folderId);
        }

        //排序方式(默认降序)(忽略大小写)
        if ("asc".equalsIgnoreCase(order)) {
            wrapper.orderByAsc(Note::getCreateTime);
        } else {
            wrapper.orderByDesc(Note::getCreateTime);
        }

        //按条件执行查询，返回结果
        Page<Note> resultPage = noteMapper.selectPage(notePage, wrapper);

        return convertToNoteVOPage(resultPage);
    }

    /**
     * 获取笔记详情
     *
     * @param userId 用户ID
     * @param id 笔记ID
     * @return 笔记详情
     */
    @Override
    public NoteDetailVO getNoteDetail(Long userId, Long id) {
        //用笔记ID查询笔记
        Note note = noteMapper.selectById(id);

        if (note == null || note.getIsDeleted() == 1) {
            throw new BusinessException("笔记不存在");
        }

        //检查用户权限(实现用户隔离)
        if (!note.getUserId().equals(userId)) {
            //编写查询条件
            LambdaQueryWrapper<NotePermission> permWrapper = new LambdaQueryWrapper<>();
            permWrapper.eq(NotePermission::getNoteId, id)//笔记ID
                    .and(w -> w.eq(NotePermission::getGranteeType, 2)//所有人分享
                            .or()
                            .eq(NotePermission::getGranteeId, userId));//当前用户是被分享用户

            //执行查询
            NotePermission permission = shareMapper.selectOne(permWrapper);
            //无权限
            if (permission == null) {
                throw new BusinessException("无权访问该笔记");
            }
        }

        //保存笔记访问记录
        saveViewHistory(userId, id);

        //获取标签列表
        List<TagVO> tags = getTagsByNoteId(id);
        //获取AI分析
        AiAnalysisVO aiAnalysis = getLatestAiAnalysis(id);

        return NoteDetailVO.fromEntity(note, tags, aiAnalysis);

    }

    /**
     * 更新笔记
     *
     * @param userId 用户ID
     * @param id 笔记ID
     * @param request 更新笔记的请求参数
     * @return 更新后的笔记对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Note updateNote(Long userId, Long id, UpdateNoteRequest request) {
        //用笔记ID查询笔记
        Note note = noteMapper.selectById(id);

        //检查笔记是否存在
        if (note == null || note.getIsDeleted() == 1) {
            throw new BusinessException("笔记不存在");
        }

        //检查用户权限(实现用户隔离)
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException("无权修改该笔记");
        }

        //更新笔记信息
        if (request.getTitle() != null) {
            note.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            note.setContent(request.getContent());
        }
        if (request.getFolderId() != null) {
            note.setFolderId(request.getFolderId());
        }
        note.setUpdateTime(LocalDateTime.now());

        //调用mapper,更新数据库
        noteMapper.updateById(note);

        if (request.getTagIds() != null) {
            updateNoteTags(id, request.getTagIds());
        }

        log.info("笔记更新成功: noteId={}, userId={}", id, userId);
        return note;
    }

    /**
     * 删除笔记
     *
     * @param userId 用户ID
     * @param id 笔记ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNote(Long userId, Long id) {
        //用笔记ID查询笔记
        Note note = noteMapper.selectById(id);

        //检查笔记是否存在
        if (note == null || note.getIsDeleted() == 1) {
            throw new BusinessException("笔记不存在");
        }

        //检查用户权限(实现用户隔离)
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException("无权删除该笔记");
        }

        noteMapper.deleteById(id);

        log.info("笔记删除成功: noteId={}, userId={}", id, userId);
    }

    /**
     * 获取笔记访问记录
     *
     * @param userId 用户ID
     * @param page 页码
     * @param size 页大小
     * @return 笔记访问记录列表
     */
    @Override
    public Page<NoteViewHistoryVO> getViewHistory(Long userId, Integer page, Integer size) {
        Page<NoteViewHistory> historyPage = new Page<>(page, size);

        //编写查询条件
        LambdaQueryWrapper<NoteViewHistory> wrapper = new LambdaQueryWrapper<>();
        //降序
        wrapper.eq(NoteViewHistory::getUserId, userId)
                .orderByDesc(NoteViewHistory::getViewTime);

        //执行查询
        Page<NoteViewHistory> resultPage = viewHistoryMapper.selectPage(historyPage, wrapper);

        List<NoteViewHistoryVO> voList = resultPage.getRecords().stream()
                .map(history -> {
                    Note note = noteMapper.selectById(history.getNoteId());
                    if (note != null && note.getIsDeleted() == 0) {
                        return NoteViewHistoryVO.fromEntity(history, note);
                    }
                    return null;
                })
                .filter(vo -> vo != null)//过滤掉null的元素(看过但是删掉了)
                .toList();

        Page<NoteViewHistoryVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * 管理笔记标签
     *
     * @param userId 用户ID
     * @param id 笔记ID
     * @param tagIds 标签ID列表
     * @return 标签列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TagVO> manageNoteTags(Long userId, Long id, List<Long> tagIds) {
        Note note = noteMapper.selectById(id);

        if (note == null || note.getIsDeleted() == 1) {
            throw new BusinessException("笔记不存在");
        }

        if (!note.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该笔记");
        }

        updateNoteTags(id, tagIds);

        return getTagsByNoteId(id);
    }

    /**
     * 编辑共享笔记
     *
     * @param userId 用户ID
     * @param id 笔记ID
     * @param request 更新笔记的请求参数
     * @return 更新后的笔记对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Note editSharedNote(Long userId, Long id, UpdateNoteRequest request) {
        //用笔记ID查询笔记
        Note note = noteMapper.selectById(id);

        //检查笔记是否存在
        if (note == null || note.getIsDeleted() == 1) {
            throw new BusinessException("笔记不存在");
        }

        //检查用户权限(实现用户隔离)(不是创建者)
        if (!note.getUserId().equals(userId)) {
            //编写查询条件（检查权限）
            LambdaQueryWrapper<NotePermission> permWrapper = new LambdaQueryWrapper<>();
            permWrapper.eq(NotePermission::getNoteId, id)//笔记ID
                    .eq(NotePermission::getPermissionType, 2)//权限类型为编辑
                    .eq(NotePermission::getGranteeType, 1)//权限类型为用户
                    .eq(NotePermission::getGranteeId, userId);//被分享者是我

            NotePermission permission = shareMapper.selectOne(permWrapper);
            //没权限
            if (permission == null) {
                throw new BusinessException("无权编辑该笔记");
            }
        } else {
            //创建者
            throw new BusinessException("所有者请使用完整更新接口");
        }

        //更新笔记标题
        if (request.getTitle() != null) {
            note.setTitle(request.getTitle());
        }
        //更新笔记内容
        if (request.getContent() != null) {
            note.setContent(request.getContent());
        }
        note.setUpdateTime(LocalDateTime.now());

        noteMapper.updateById(note);

        log.info("共享笔记编辑成功: noteId={}, userId={}", id, userId);
        return note;
    }

    /**
     * 创建新标签
     *
     * @param userId 用户ID
     * @param request 创建标签的请求参数
     * @return 创建成功的标签VO对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TagVO createTag(Long userId, CreateTagRequest request) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tag::getName, request.getName());

        Tag existingTag = tagMapper.selectOne(wrapper);
        if (existingTag != null) {
            throw new BusinessException("标签名称已存在");
        }

        Tag tag = new Tag();
        tag.setName(request.getName());
        tag.setCreateTime(LocalDateTime.now());

        tagMapper.insert(tag);

        log.info("标签创建成功: tagId={}, tagName={}", tag.getId(), request.getName());
        return TagVO.fromEntity(tag);
    }

    /**
     * 删除标签
     *
     * @param userId 用户ID
     * @param id 标签ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long userId, Long id) {
        Tag tag = tagMapper.selectById(id);

        if (tag == null) {
            throw new BusinessException("标签不存在");
        }

        noteTagMapper.deleteByTagId(id);

        tagMapper.deleteById(id);

        log.info("标签删除成功: tagId={}, tagName={}", id, tag.getName());
    }

    /**
     * 查询标签列表
     *
     * @param userId 用户ID
     * @param page 页码
     * @param size 页大小
     * @return 标签分页列表（按创建时间降序）
     */
    @Override
    public Page<TagVO> getTags(Long userId, Integer page, Integer size) {
        Page<Tag> tagPage = new Page<>(page, size);

        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Tag::getCreateTime);

        Page<Tag> resultPage = tagMapper.selectPage(tagPage, wrapper);

        List<TagVO> voList = resultPage.getRecords().stream()
                .map(TagVO::fromEntity)
                .toList();

        Page<TagVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        voPage.setRecords(voList);

        return voPage;
    }


    //保存笔记标签(只多添加)
    private void saveNoteTags(Long noteId, List<Long> tagIds) {
        //增强for循环
        for (Long tagId : tagIds) {
            NoteTag noteTag = new NoteTag();
            noteTag.setNoteId(noteId);
            noteTag.setTagId(tagId);
            noteTag.setCreateTime(LocalDateTime.now());
            noteTagMapper.insert(noteTag);
        }
    }

    //更新笔记标签(全部删了重新保存)
    private void updateNoteTags(Long noteId, List<Long> tagIds) {
        //删除所有
        noteTagMapper.deleteByNoteId(noteId);

        //逐一保存
        if (tagIds != null && !tagIds.isEmpty()) {
            saveNoteTags(noteId, tagIds);
        }
    }

    //获取笔记标签
    private List<TagVO> getTagsByNoteId(Long noteId) {
        //获取该笔记的标签列表
        List<Tag> tags = noteMapper.selectTagsByNoteId(noteId);
        return tags.stream()
                .map(TagVO::fromEntity)
                .toList();
    }

    //获取笔记ai分析摘要(预览)
    private String getAiSummaryByNoteId(Long noteId) {
        //获取该笔记的最新ai分析
        AiAnalysisVO analysis = getLatestAiAnalysis(noteId);
        //返回分析内容
        return analysis != null ? analysis.getAnalysisContent() : null;
    }

    //获取笔记最新一次的ai分析(详情)
    private AiAnalysisVO getLatestAiAnalysis(Long noteId) {
        LambdaQueryWrapper<AiAnalysis> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiAnalysis::getNoteId, noteId)
                .orderByDesc(AiAnalysis::getCreateTime)//降序(最新在前)
                .last("LIMIT 1");//获取最新的一条(直接在末尾加片段，只返回查询结果第一条)

        //获取最新一条
        AiAnalysis analysis = aiMapper.selectOne(wrapper);
        return analysis != null ? AiAnalysisVO.fromEntity(analysis) : null;
    }

    //保存笔记访问记录
    private void saveViewHistory(Long userId, Long noteId) {
        //先查询是否已存在
        LambdaQueryWrapper<NoteViewHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteViewHistory::getUserId, userId)
                .eq(NoteViewHistory::getNoteId, noteId);

        NoteViewHistory existing = viewHistoryMapper.selectOne(wrapper);

        if (existing != null) {
            //更新访问时间
            existing.setViewTime(LocalDateTime.now());
            viewHistoryMapper.updateById(existing);
        } else {
            //新增记录
            NoteViewHistory history = new NoteViewHistory();
            history.setUserId(userId);
            history.setNoteId(noteId);
            history.setViewTime(LocalDateTime.now());
            viewHistoryMapper.insert(history);
        }
    }

    //根据标签ID查询笔记
    private Page<NoteVO> getNotesByTag(Long userId, Long tagId, Integer page, Integer size, String order) {
        //获取笔记列表
        List<Note> notes = noteMapper.selectNotesByTagId(userId, tagId);

        if ("asc".equalsIgnoreCase(order)) {
            //升序
            notes.sort((n1, n2) -> n1.getCreateTime().compareTo(n2.getCreateTime()));
        } else {
            //降序
            notes.sort((n1, n2) -> n2.getCreateTime().compareTo(n1.getCreateTime()));
        }

        int total = notes.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        // 计算当前页的数据范围：如果起始索引小于总数，截取子列表；否则返回空列表（防止越界）
        List<Note> pageNotes = fromIndex < total ? notes.subList(fromIndex, toIndex) : List.of();

        //创建分页对象
        Page<Note> notePage = new Page<>(page, size, total);
        notePage.setRecords(pageNotes);

        return convertToNoteVOPage(notePage);
    }

    //转换笔记为VO
    private Page<NoteVO> convertToNoteVOPage(Page<Note> notePage) {
        List<NoteVO> voList = notePage.getRecords().stream()
                .map(note -> {
                    List<TagVO> tags = getTagsByNoteId(note.getId());
                    String aiSummary = getAiSummaryByNoteId(note.getId());
                    return NoteVO.fromEntity(note, tags, aiSummary);
                })
                .toList();

        Page<NoteVO> voPage = new Page<>(notePage.getCurrent(), notePage.getSize(), notePage.getTotal());
        voPage.setRecords(voList);

        return voPage;
    }
}
