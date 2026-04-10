package com.smartnote.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartnote.dto.note.*;
import com.smartnote.entity.Note;
import com.smartnote.service.NoteService;
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
@RequestMapping("/api/notes")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    /**
     * 创建新笔记
     * POST /api/notes
     * 创建一篇新笔记，可关联标签
     */
    @PostMapping
    public Result<Note> createNote(@RequestAttribute Long userId,
                                   @Valid @RequestBody CreateNoteRequest request) {
        log.info("创建笔记: userId={}, title={}", userId, request.getTitle());
        Note note = noteService.createNote(userId, request);
        return Result.success("笔记创建成功", note);
    }

    /**
     * 分页查询笔记列表
     * GET /api/notes
     * 支持按标题搜索、按标签筛选、分页查询和排序
     * 默认最新的笔记在前
     */
    @GetMapping
    public Result<Page<NoteVO>> getNotes(@RequestAttribute Long userId,
                                         @RequestParam(required = false) String title,
                                         @RequestParam(required = false) Long tagId,
                                         @RequestParam(required = false) Long folderId,
                                         @RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "20") Integer size,
                                         @RequestParam(defaultValue = "desc") String order) {
        log.info("查询笔记列表: userId={}, title={}, tagId={}, page={}, size={}, order={}",
                userId, title, tagId, page, size, order);
        Page<NoteVO> notes = noteService.getNotes(userId, title, tagId, folderId, page, size, order);
        return Result.success(notes);
    }

    /**
     * 获取笔记详情
     * GET /api/notes/{id}
     * 获取笔记的完整内容，包含标签和AI分析结果
     *
     * @param userId 用户ID
     * @param id 笔记ID
     */
    @GetMapping("/{id}")
    public Result<NoteDetailVO> getNoteDetail(@RequestAttribute Long userId,
                                              @PathVariable Long id) {
        log.info("获取笔记详情: userId={}, noteId={}", userId, id);
        NoteDetailVO noteDetail = noteService.getNoteDetail(userId, id);
        return Result.success(noteDetail);
    }

    /**
     * 更新笔记
     * PUT /api/notes/{id}
     * 更新笔记的标题和内容
     */
    @PutMapping("/{id}")
    public Result<Note> updateNote(@RequestAttribute Long userId,
                                   @PathVariable Long id,
                                   @Valid @RequestBody UpdateNoteRequest request) {
        log.info("更新笔记: userId={}, noteId={}, title={}", userId, id, request.getTitle());
        Note updatedNote = noteService.updateNote(userId, id, request);
        return Result.success("笔记更新成功", updatedNote);
    }

    /**
     * 删除笔记（逻辑删除）
     * DELETE /api/notes/{id}
     * 将笔记标记为已删除，而非物理删除
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteNote(@RequestAttribute Long userId,
                                     @PathVariable Long id) {
        log.info("删除笔记: userId={}, noteId={}", userId, id);
        noteService.deleteNote(userId, id);
        return Result.success("笔记删除成功");
    }

    /**
     * 获取笔记浏览历史
     * GET /api/notes/history
     * 获取用户最近浏览的笔记记录
     */
    @GetMapping("/history")
    public Result<Page<NoteViewHistoryVO>> getViewHistory(@RequestAttribute Long userId,
                                                          @RequestParam(defaultValue = "1") Integer page,
                                                          @RequestParam(defaultValue = "20") Integer size) {
        log.info("获取浏览历史: userId={}, page={}, size={}", userId, page, size);
        Page<NoteViewHistoryVO> history = noteService.getViewHistory(userId, page, size);
        return Result.success(history);
    }

    /**
     * 添加/删除笔记标签
     * POST /api/notes/{id}/tags
     * 为笔记添加或删除标签
     */
    @PostMapping("/{id}/tags")
    public Result<List<TagVO>> manageNoteTags(@RequestAttribute Long userId,
                                              @PathVariable Long id,
                                              @RequestBody TagManagementRequest request) {
        log.info("管理笔记标签: userId={}, noteId={}, tagIds={}", userId, id, request.getTagIds());
        List<TagVO> tags = noteService.manageNoteTags(userId, id, request.getTagIds());
        return Result.success("标签更新成功", tags);
    }

    /**
     * 编辑共享笔记(只能编辑标题，内容)
     * PATCH /api/notes/{id}
     * 编辑共享笔记的标题和内容
     *
     * @param userId 用户ID
     * @param id 笔记ID
     * @param request 更新请求
     */
    @PatchMapping("/{id}")
    public Result<Note> editSharedNote(@RequestAttribute Long userId,
                                       @PathVariable Long id,
                                       @Valid @RequestBody UpdateNoteRequest request) {
        log.info("编辑共享笔记: userId={}, noteId={}, title={}", userId, id, request.getTitle());
        Note editedNote = noteService.editSharedNote(userId, id, request);
        return Result.success("笔记编辑成功", editedNote);
    }


}
