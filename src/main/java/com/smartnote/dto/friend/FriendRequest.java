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

    private String groupName;//分组名称（默认为"默认"）

    //前端不能不传字段，没有时传null
    public void setGroupName(String groupName) {
        this.groupName = (groupName == null || groupName.isEmpty()) ? "默认" : groupName;
    }
}
