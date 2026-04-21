package com.smartnote.dto.friend;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新好友分组请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGroupRequest {

    @NotBlank(message = "分组ID不能为空")
    private String groupId;
}
