package com.smartnote.dto.friend;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateGroupRequest {

    @NotBlank(message = "分组名称不能为空")
    @Size(min = 1, max = 20, message = "分组名称长度必须在1-20个字符之间")
    private String groupName;
}
