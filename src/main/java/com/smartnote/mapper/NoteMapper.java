package com.smartnote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartnote.entity.Note;
import com.smartnote.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface NoteMapper extends BaseMapper<Note> {

    @Select("SELECT t.* FROM tag t INNER JOIN note_tag nt ON t.id = nt.tag_id WHERE nt.note_id = #{noteId}")
    List<Tag> selectTagsByNoteId(Long noteId);

    @Select("SELECT n.* FROM note n INNER JOIN note_tag nt ON n.id = nt.note_id " +
            "WHERE n.user_id = #{userId} AND nt.tag_id = #{tagId} AND n.is_deleted = 0")
    List<Note> selectNotesByTagId(Long userId, Long tagId);

    @Select("SELECT * FROM note WHERE user_id = #{userId} AND is_deleted = 1 ORDER BY update_time DESC")
    Page<Note> selectDeletedNotes(Page<Note> page, @Param("userId") Long userId);

    @Select("SELECT * FROM note WHERE id = #{id}")
    Note selectByIdIgnoreLogic(@Param("id") Long id);

    @Update("UPDATE note SET is_deleted = 0, update_time = #{updateTime} WHERE id = #{id}")
    int restoreNoteById(@Param("id") Long id, @Param("updateTime") java.time.LocalDateTime updateTime);

}
