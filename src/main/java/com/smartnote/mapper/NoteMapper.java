package com.smartnote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartnote.entity.Note;
import com.smartnote.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NoteMapper extends BaseMapper<Note> {

    @Select("SELECT t.* FROM tag t INNER JOIN note_tag nt ON t.id = nt.tag_id WHERE nt.note_id = #{noteId}")
    List<Tag> selectTagsByNoteId(Long noteId);

    @Select("SELECT n.* FROM note n INNER JOIN note_tag nt ON n.id = nt.note_id " +
            "WHERE n.user_id = #{userId} AND nt.tag_id = #{tagId} AND n.is_deleted = 0")
    List<Note> selectNotesByTagId(Long userId, Long tagId);
}
