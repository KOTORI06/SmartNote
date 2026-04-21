package com.smartnote.constant;

public class FriendStatus {

    public static final int PENDING = 0;//已发送申请

    public static final int ACCEPTED = 1;//已接受

    public static final int REJECTED = 2;//已拒绝

    public static final int DELETED = 3;//已删除

    //私有构造函数，防止实例化
    private FriendStatus() {
    }
}
