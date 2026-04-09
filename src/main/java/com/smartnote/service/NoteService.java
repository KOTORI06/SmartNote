package com.smartnote.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartnote.dto.note.*;
import com.smartnote.entity.Note;
import jakarta.validation.Valid;

import java.util.List;

public interface NoteService {

    // 创建笔记
    Note createNote(Long userId, CreateNoteRequest request);

    // 分页查询笔记列表
    Page<NoteVO> getNotes(Long userId, String title, Long tagId, Long folderId, Integer page, Integer size, String order);

    // 获取笔记详情
    NoteDetailVO getNoteDetail(Long userId, Long id);

    // 更新笔记
    Note updateNote(Long userId, Long id, @Valid UpdateNoteRequest request);

    // 删除笔记
    void deleteNote(Long userId, Long id);

    // 获取笔记浏览历史
    Page<NoteViewHistoryVO> getViewHistory(Long userId, Integer page, Integer size);

    // 添加/删除笔记标签
    List<TagVO> manageNoteTags(Long userId, Long id, List<Long> tagIds);
}
