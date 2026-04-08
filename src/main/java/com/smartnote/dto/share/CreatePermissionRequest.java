package com.smartnote.dto.share;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建分享权限请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePermissionRequest {

    @NotNull(message = "笔记ID不能为空")
    private Long noteId;

    @NotNull(message = "被授权者类型不能为空")
    @Min(value = 1, message = "无效的被授权者类型")
    @Max(value = 3, message = "无效的被授权者类型")
    private Integer granteeType; // 1-用户，2-好友分组，3-所有人

    private Long granteeId; // 当granteeType=1或2时，指定用户ID或分组ID

    @NotNull(message = "权限类型不能为空")
    @Min(value = 1, message = "无效的权限类型")
    @Max(value = 2, message = "无效的权限类型")
    private Integer permissionType; // 1-可查看，2-可编辑
}
