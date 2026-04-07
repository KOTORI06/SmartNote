package com.smartnote.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 密码加密工具类

 采用 BCrypt 强哈希算法，提供安全的密码加密和验证功能
 BCrypt 算法特点：
 1. 自动加盐：每次加密同一密码都会生成不同的密文，防止彩虹表攻击
 2. 可调强度：通过 strength 参数控制计算复杂度，抵御暴力破解
 3. 单向加密：不可逆，无法从密文还原明文密码
 4. 内置验证：提供 matches 方法直接对比明文和密文

 使用场景：
 - 用户注册时加密密码
 - 用户登录时验证密码
 - 修改密码时加密新密码
 */
@Component
public class PasswordUtil {

    /**
     BCrypt 密码编码器实例

     strength 参数说明：
     - 取值范围：4-31
     - 默认值：10
     - 计算次数：2^strength 次迭代
     - 示例：strength=10 表示 2^10=1024 次迭代
     - 建议值：10-12（平衡安全性和性能）

     性能参考（strength=10）：
     - 加密一次耗时：约 50-100ms
     - 强度每增加 1，耗时翻倍
     */
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(10);
    /**
     * 私有构造函数，防止实例化
     *
     * 由于所有方法都是静态的，且使用单例编码器，
     * 不需要创建工具类实例
     */
    private PasswordUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     对明文密码进行加密

     工作流程：
     1. 自动生成随机盐值（16 字节）
     2. 将盐值和密码组合后进行 BCrypt 哈希计算
     3. 返回包含盐值和哈希值的完整密文

     密文格式示例：
     $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
     │   │  │                                │
     │   │  │                                └─ 31 字节哈希值
     │   │  └─ 22 字符盐值（Base64 编码）
     │   └─ strength 参数（10）
       └─ 算法版本标识（2a）

     @param plainPassword 明文密码，不能为 null 或空字符串
     @return 加密后的密文，格式为 BCrypt 标准格式
     @throws IllegalArgumentException 如果密码为空或 null
     */
    public static String encrypt(String plainPassword) {
        // 参数校验：密码不能为空
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        // 调用 BCrypt 编码器进行加密
        // 内部自动完成：生成盐值 → 组合密码 → 哈希计算 → 返回密文
        return PASSWORD_ENCODER.encode(plainPassword);
    }

    /**
     验证明文密码是否与加密后的密文匹配

     工作原理：
     1. 从密文中提取盐值和 strength 参数
     2. 使用相同的盐值对明文密码进行加密
     3. 比较新生成的密文与原始密文是否一致

     时序安全：
     - 使用恒定时间比较算法，防止时序攻击
     - 即使密码错误，也会执行完整的比较流程
     - 攻击者无法通过响应时间推断密码正确性

     @param plainPassword 用户输入的明文密码
     @param encodedPassword 数据库中存储的加密密文
     @return true-密码匹配，false-密码不匹配
     @throws IllegalArgumentException 如果任一参数为空或 null

     使用示例：
     <pre>
     boolean isValid = PasswordUtil.matches("123456", "$2a$10$N9qo8uLO...");
     // 结果：true 或 false
     </pre>
     */
    public static boolean matches(String plainPassword, String encodedPassword) {
        // 参数校验：明文密码和密文都不能为空
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("明文密码不能为空");
        }
        if (encodedPassword == null || encodedPassword.isEmpty()) {
            throw new IllegalArgumentException("加密密文不能为空");
        }

        // 调用 BCrypt 编码器进行验证
        // 内部自动完成：提取盐值 → 重新加密 → 恒定时间比较
        return PASSWORD_ENCODER.matches(plainPassword, encodedPassword);
    }

    /**
     * 检查密文是否需要升级
     *
     * 应用场景：
     * - 当系统调整了 strength 参数后
     * - 检测旧密码是否使用了较弱的加密强度
     * - 在用户下次登录时自动重新加密
     *
     * @param encodedPassword 数据库中的加密密文
     * @return true-需要升级（强度不足），false-无需升级
     */
    public static boolean needsUpgrade(String encodedPassword) {
        // 参数校验
        if (encodedPassword == null || encodedPassword.isEmpty()) {
            return true; // 空密文视为需要升级
        }

        // 检查密文是否符合 BCrypt 格式
        // BCrypt 密文应以 $2a$、$2b$ 或 $2y$ 开头
        if (!encodedPassword.startsWith("$2a$") &&
            !encodedPassword.startsWith("$2b$") &&
            !encodedPassword.startsWith("$2y$")) {
            return true; // 非 BCrypt 格式，需要升级
        }

        // 提取当前的 strength 值
        try {
            // 密文格式：$2a$10$...
            //           ↑↑↑↑
            //         第 4-5 位是 strength
            String strengthStr = encodedPassword.substring(4, 6);
            int currentStrength = Integer.parseInt(strengthStr);

            // 如果当前强度低于推荐值（10），则需要升级
            return currentStrength < 10;
        } catch (Exception e) {
            // 解析失败，视为需要升级
            return true;
        }
    }

    /**
     生成符合安全要求的随机密码

     密码规则：
     - 长度：12 位（可调整）
     - 包含：大写字母、小写字母、数字、特殊字符
     - 用途：忘记密码时的临时密码、初始密码

     @param length 密码长度，建议 8-16 位
     @return 随机生成的密码字符串
     @throws IllegalArgumentException 如果长度不在合理范围内
     */
    public static String generateRandomPassword(int length) {
        // 参数校验：密码长度必须在 8-32 之间
        if (length < 8 || length > 32) {
            throw new IllegalArgumentException("密码长度必须在 8-32 位之间");
        }

        // 定义字符集
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";  // 大写字母
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";  // 小写字母
        String digits = "0123456789";                      // 数字
        String specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?"; // 特殊字符
        String allChars = upperCase + lowerCase + digits + specialChars;

        // 使用 SecureRandom 生成密码（比 Random 更安全）
        java.security.SecureRandom secureRandom = new java.security.SecureRandom();

        StringBuilder password = new StringBuilder(length);

        // 确保每种字符至少出现一次
        password.append(upperCase.charAt(secureRandom.nextInt(upperCase.length())));
        password.append(lowerCase.charAt(secureRandom.nextInt(lowerCase.length())));
        password.append(digits.charAt(secureRandom.nextInt(digits.length())));
        password.append(specialChars.charAt(secureRandom.nextInt(specialChars.length())));

        // 剩余位置从所有字符中随机选择
        for (int i = 4; i < length; i++) {
            password.append(allChars.charAt(secureRandom.nextInt(allChars.length())));
        }

        // 打乱字符顺序（避免前 4 位固定类型）
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            // 交换字符
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }

        return new String(passwordArray);
    }

    /**
     * 检查明文密码强度
     *
     * 评分规则（满分 100 分）：
     * - 长度 >= 8：+20 分
     * - 长度 >= 12：+10 分
     * - 包含大写字母：+15 分
     * - 包含小写字母：+15 分
     * - 包含数字：+15 分
     * - 包含特殊字符：+25 分
     *
     * @param password 待检查的密码
     * @return 密码强度分数（0-100）
     */
    public static int checkPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        int score = 0;

        // 长度评分
        if (password.length() >= 8) {
            score += 20;
        }
        if (password.length() >= 12) {
            score += 10;
        }

        // 字符类型评分
        boolean hasUpperCase = false;  // 是否包含大写字母
        boolean hasLowerCase = false;  // 是否包含小写字母
        boolean hasDigit = false;      // 是否包含数字
        boolean hasSpecialChar = false; // 是否包含特殊字符

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpperCase = true;
            } else if (Character.isLowerCase(c)) {
                hasLowerCase = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else {
                hasSpecialChar = true;
            }
        }

        if (hasUpperCase) {
            score += 15;
        }
        if (hasLowerCase) {
            score += 15;
        }
        if (hasDigit) {
            score += 15;
        }
        if (hasSpecialChar) {
            score += 25;
        }

        return score;
    }

    /**
     获取密码强度等级描述

     @param password 待检查的密码
     @return 强度等级描述：弱/中等/强/非常强
     */
    public static String getPasswordStrengthLevel(String password) {
        int score = checkPasswordStrength(password);

        if (score < 40) {
            return "弱";
        } else if (score < 60) {
            return "中等";
        } else if (score < 80) {
            return "强";
        } else {
            return "非常强";
        }
    }
}
