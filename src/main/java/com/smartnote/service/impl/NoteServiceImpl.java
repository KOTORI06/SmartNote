package com.smartnote.service.impl;

import com.smartnote.dto.note.CreateNoteRequest;
import com.smartnote.entity.Note;
import com.smartnote.mapper.NoteMapper;
import com.smartnote.service.NoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor//构造器注入
public class NoteServiceImpl implements NoteService {

    private final NoteMapper noteMapper;

    /**
     * 创建新笔记
     *
     * @param userId 用户ID
     * @param request 创建笔记的请求参数
     * @return 创建成功的笔记对象
     */
    @Override
    public Note createNote(Long userId, CreateNoteRequest request) {
        return null;
    }
}
