package com.smartnote.dto.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 更新笔记请求 DTO
 *
 * 用途：前端更新笔记时提交的请求数据
 * 验证规则：标题不能为空
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNoteRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;//笔记标题（可选）

    @Size(max = 10000, message = "内容长度不能超过10000个字符")
    private String content;//笔记正文内容（可选）

    private Long folderId;//所属文件夹ID（可选）

    private List<Long> tagIds;//关联的标签ID列表（可选）
}
