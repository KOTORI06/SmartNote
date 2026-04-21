package com.smartnote.dto.share;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 权限检查结果视图对象 VO
 *
 * 用途：返回用户对笔记的访问权限检查结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionCheckVO {

    private Boolean hasPermission;//是否有权限

    private Integer permissionType;//权限类型：null-无权限，1-可查看，2-可编辑

    private String message;//提示信息

    /**
     * 构建有权限的响应
     *
     * @param permissionType 权限类型
     * @return 构建好的PermissionCheckVO对象
     */
    public static PermissionCheckVO granted(Integer permissionType, String message) {
        return PermissionCheckVO.builder()
                .hasPermission(true)
                .permissionType(permissionType)
                .message(message)
                .build();
    }

    /**
     * 构建无权限的响应
     *
     * @param message 提示信息
     * @return 构建好的PermissionCheckVO对象
     */
    public static PermissionCheckVO denied(String message) {
        return PermissionCheckVO.builder()
                .hasPermission(false)
                .permissionType(null)
                .message(message)
                .build();
    }
}
