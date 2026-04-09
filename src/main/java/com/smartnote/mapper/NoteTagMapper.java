package com.smartnote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartnote.entity.NoteTag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface NoteTagMapper extends BaseMapper<NoteTag> {

    @Delete("DELETE FROM note_tag WHERE note_id = #{noteId}")
    int deleteByNoteId(Long noteId);
}
