package com.smartnote.service;

import com.smartnote.dto.note.CreateNoteRequest;
import com.smartnote.entity.Note;
import jakarta.validation.Valid;

public interface NoteService {

    // 创建笔记
    Note createNote(Long userId, CreateNoteRequest request);
}
