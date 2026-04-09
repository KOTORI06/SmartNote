package com.smartnote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartnote.entity.NotePermission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShareMapper extends BaseMapper<NotePermission> {

}
