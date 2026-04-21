package com.smartnote.dto.note;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 标签管理请求 DTO
 *
 * 用途：为笔记添加或删除标签
 * 说明：tagIds 表示要为笔记设置的标签ID集合（全量替换）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagManagementRequest {

    private List<Long> tagIds;//要关联的标签ID列表（空列表表示清除所有标签）
}
