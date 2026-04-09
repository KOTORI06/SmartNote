package com.smartnote.dto.friend;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 好友申请请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequest {

    @NotNull(message = "好友ID不能为空")
    private Long friendId;//好友ID

    @Size(max = 100, message = "申请备注不能超过100个字符")
    private String applyRemark;//申请备注
}
