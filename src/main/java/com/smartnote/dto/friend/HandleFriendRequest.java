package com.smartnote.dto.friend;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 处理好友申请请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HandleFriendRequest {

    @NotBlank(message = "处理状态不能为空")
    @Min(value = 1, message = "状态值无效")
    @Max(value = 2, message = "状态值无效")
    private Integer status;
}
