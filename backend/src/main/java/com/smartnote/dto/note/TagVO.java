package com.smartnote.dto.note;

import com.smartnote.entity.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 标签视图对象 VO
 *
 * 用途：在笔记相关接口中展示标签信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagVO {

    private Long id;//标签ID

    private String name;//标签名称

    private LocalDateTime createTime;//标签创建时间


    /**
     * 从Tag实体对象构建TagVO
     *
     * @param tag 标签实体对象
     * @return 构建好的TagVO对象
     */
    public static TagVO fromEntity(Tag tag) {
        return TagVO.builder()
                .id(tag.getId())
                .name(tag.getName())
                .createTime(tag.getCreateTime())
                .build();
    }
}
