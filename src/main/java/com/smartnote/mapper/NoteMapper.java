package com.smartnote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartnote.entity.Note;
import com.smartnote.entity.NoteTag;
import com.smartnote.entity.NoteViewHistory;
import com.smartnote.entity.Tag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NoteMapper extends BaseMapper<Note> {

    @Select("SELECT t.* FROM tag t INNER JOIN note_tag nt ON t.id = nt.tag_id WHERE nt.note_id = #{noteId}")
    List<Tag> selectTagsByNoteId(Long noteId);

    @Insert("INSERT INTO note_tag(note_id, tag_id, create_time) VALUES(#{noteId}, #{tagId}, #{createTime})")
    int insertNoteTag(NoteTag noteTag);

    @Delete("DELETE FROM note_tag WHERE note_id = #{noteId}")
    int deleteNoteTagsByNoteId(Long noteId);

    @Insert("INSERT INTO note_view_history(user_id, note_id, view_time) VALUES(#{userId}, #{noteId}, #{viewTime})")
    int insertViewHistory(NoteViewHistory history);
}
