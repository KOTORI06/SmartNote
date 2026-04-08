package com.smartnote.dto.share;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量设置分享权限请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchPermissionRequest {

    @NotNull(message = "笔记ID不能为空")
    private Long noteId;

    @NotNull(message = "被授权者类型不能为空")
    private Integer granteeType;

    @NotNull(message = "被授权者ID列表不能为空")
    private List<Long> granteeIds; // 多个用户ID或分组ID

    @NotNull(message = "权限类型不能为空")
    private Integer permissionType;
}
