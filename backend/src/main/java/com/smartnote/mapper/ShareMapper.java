package com.smartnote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartnote.entity.NotePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ShareMapper extends BaseMapper<NotePermission> {

}
