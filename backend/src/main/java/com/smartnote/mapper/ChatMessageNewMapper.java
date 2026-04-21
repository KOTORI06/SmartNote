package com.smartnote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartnote.entity.ChatMessageNew;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ChatMessageNewMapper extends BaseMapper<ChatMessageNew> {

    @Select("SELECT m.* FROM chat_message_new m " +
            "INNER JOIN (" +
            "  SELECT id FROM chat_message_new " +
            "  WHERE conversation_id = #{conversationId} " +
            "  ORDER BY created_at DESC " +
            "  LIMIT #{offset}, #{size}" +
            ") t ON m.id = t.id " +
            "ORDER BY m.created_at DESC")
    Page<ChatMessageNew> selectMessagesByConversation(Page<ChatMessageNew> page,
                                                      @Param("conversationId") Long conversationId,
                                                      @Param("offset") long offset,
                                                      @Param("size") long size);


}
