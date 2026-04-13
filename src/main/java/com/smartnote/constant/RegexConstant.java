package com.smartnote.constant;

//正则表达式验证常量
public class RegexConstant {

    //手机号：11位数字，以1开头，第二位3-9
    public static final String PHONE = "^1[3-9]\\d{9}$";
    public static final String PHONE_MESSAGE = "手机号格式不正确";

    //邮箱：标准邮箱格式
    public static final String EMAIL = "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$";
    public static final String EMAIL_MESSAGE = "邮箱格式不正确";

    //用户名：4-20位，支持字母、数字、下划线、中文
    public static final String USERNAME = "^[a-zA-Z0-9_\\u4e00-\\u9fa5]{4,20}$";//\u4e00-\u9fa5（Unicode 范围，匹配所有常用中文字符）
    public static final String USERNAME_MESSAGE = "用户名格式不正确（4-20位字母、数字、下划线或中文）";

    //密码：8-20位，必须包含字母和数字，可选特殊字符
    public static final String PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@#$%^&+=]{8,20}$";
    public static final String PASSWORD_MESSAGE = "密码格式不正确（8-20位，需包含字母和数字）";

    //邮箱或手机号：标准邮箱格式或11位手机号
    public static final String EMAIL_OR_PHONE = "(^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$)|(^1[3-9]\\d{9}$)";
    public static final String EMAIL_OR_PHONE_MESSAGE = "请输入正确的邮箱或手机号";

    //私有构造函数，防止实例化
    private RegexConstant() {
    }
}
