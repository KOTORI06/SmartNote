package com.smartnote.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class JwtUtil {

    /**
     JWT 密钥（用于签名和验证）
     生产环境应该从配置文件读取，并且使用更复杂的密钥
     密钥长度至少 256 位（32 字节），这里使用 HS256 算法
     */
    private static final String SECRET_KEY = "SmartNote_JWT_Secret_Key_2026_Very_Long_String_For_HS256";
    /**
     Token 有效期：7 天（单位：毫秒）
     计算方式：7 * 24 * 60 * 60 * 1000 = 604800000 毫秒
     */
    private static final long EXPIRATION_TIME = 7 * 24 * 60 * 60 * 1000;
    /**
     Token 前缀
     HTTP 请求头中格式：Authorization: Bearer xxxxx.yyyyy.zzzzz
     */
    private static final String TOKEN_PREFIX = "Bearer ";
    /**
     HTTP 请求头名称
     */
    public static final String HEADER_STRING = "Authorization";
    /**
     JWT 中的主题字段名（存储用户ID）
     */
    private static final String CLAIM_USER_ID = "userId";
    /**
     JWT 中的用户名载荷字段名
     */
    private static final String CLAIM_USERNAME = "username";

    /**
     * 获取签名密钥
     * 将字符串密钥转换为符合 HS256 算法要求的 SecretKey 对象
     * @return SecretKey 签名密钥对象
     */
    private static SecretKey getSigningKey() {
        // Keys.hmacShaKeyFor() 方法会将字符串转换为安全的密钥
        // 要求密钥长度至少 256 位（32 字节）
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    /**
     生成 JWT Token
     工作流程：
     1. 创建载荷（Claims），添加用户信息
     2. 设置签发时间、过期时间
     3. 使用密钥进行签名
     4. 生成最终的 Token 字符串

     @param userId 用户ID
     @param username 用户名
     @return 生成的 JWT Token 字符串
     */
    public static String generateToken(Long userId, String username) {
        // 创建载荷 Map，用于存储自定义信息
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);      // 添加用户ID到载荷
        claims.put(CLAIM_USERNAME, username);   // 添加用户名到载荷

        // 获取当前时间
        Date now = new Date();
        // 计算过期时间 = 当前时间 + 有效期
        Date expiration = new Date(now.getTime() + EXPIRATION_TIME);

        // 构建并生成 JWT Token
        String token = Jwts.builder()
                .setClaims(claims)              // 设置自定义载荷（用户信息）
                .setSubject(username)           // 设置主题（通常是用户名）
                .setIssuedAt(now)               // 设置签发时间（iat - issued at）
                .setExpiration(expiration)      // 设置过期时间（exp - expiration）
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)  // 使用 HS256 算法和密钥签名
                .compact();                     // 压缩生成最终的 Token 字符串

        log.info("为用户 [{}] 生成 JWT Token，过期时间：{}", username, expiration);
        return token;
    }

    /**
     从 Token 中解析出 Claims（载荷）

     Claims 是 JWT 的 payload 部分，包含：
     - 标准声明：sub（主题）、iat（签发时间）、exp（过期时间）等
     - 自定义声明：userId、username 等我们添加的信息

     @param token JWT Token 字符串
     @return Claims 对象，包含所有载荷信息
     @throws Exception 如果 Token 无效或已过期，会抛出异常
     */
    public static Claims parseToken(String token) {
        try {
            // 移除可能存在的 "Bearer " 前缀
            if (token != null && token.startsWith(TOKEN_PREFIX)) {
                token = token.substring(TOKEN_PREFIX.length());
            }

            // 解析 Token 并验证签名
            // parseClaimsJws() 会自动验证：
            // 1. 签名是否正确（防止篡改）
            // 2. Token 是否过期
            // 3. Token 格式是否正确
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())  // 设置签名密钥用于验证
                    .build()                          // 构建解析器
                    .parseClaimsJws(token)            // 解析并验证 Token
                    .getBody();                       // 获取载荷部分

            return claims;
        } catch (Exception e) {
            log.error("JWT Token 解析失败：{}", e.getMessage());
            throw new RuntimeException("无效的 Token", e);
        }
    }

    /**
     从 Token 中获取用户ID

     @param token JWT Token 字符串
     @return 用户ID
     */
    public static Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        // 从载荷中获取 userId，转换为 Long 类型
        return claims.get(CLAIM_USER_ID, Long.class);
    }

    /**
     从 Token 中获取用户名

     @param token JWT Token 字符串
     @return 用户名
     */
    public static String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        // getSubject() 获取标准声明中的 sub 字段
        return claims.getSubject();
    }

    /**
     验证 Token 是否有效

     验证流程：
     1. Token 不为空
     2. Token 格式正确
     3. 签名验证通过
     4. Token 未过期

     @param token JWT Token 字符串
     @return true-有效，false-无效
     */
    public static boolean validateToken(String token) {
        try {
            parseToken(token);  // 尝试解析，如果成功说明 Token 有效
            return true;
        } catch (Exception e) {
            log.warn("JWT Token 验证失败：{}", e.getMessage());
            return false;
        }
    }

    /**
     检查 Token 是否即将过期

     @param token JWT Token 字符串
     @param thresholdMinutes 阈值（分钟），默认 30 分钟
     @return true-即将过期，false-未即将过期
     */
    public static boolean isTokenExpiringSoon(String token, int thresholdMinutes) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();  // 获取过期时间
            Date now = new Date();

            // 计算剩余时间（毫秒）
            long remainingTime = expiration.getTime() - now.getTime();
            // 阈值时间（毫秒）
            long thresholdTime = thresholdMinutes * 60 * 1000L;

            // 如果剩余时间小于阈值，说明即将过期
            return remainingTime > 0 && remainingTime < thresholdTime;
        } catch (Exception e) {
            return true;  // 解析失败也认为需要刷新
        }
    }

    /**
     刷新 Token（生成新的 Token）

     使用场景：
     - Token 即将过期时，生成新的 Token 延长有效期
     - 避免用户频繁重新登录

     @param oldToken 旧的 Token
     @return 新的 Token
     */
    public static String refreshToken(String oldToken) {
        try {
            // 从旧 Token 中解析出用户信息
            Claims claims = parseToken(oldToken);
            Long userId = claims.get(CLAIM_USER_ID, Long.class);
            String username = claims.getSubject();

            // 使用相同的信息生成新 Token
            String newToken = generateToken(userId, username);
            log.info("为用户 [{}] 刷新 Token", username);
            return newToken;
        } catch (Exception e) {
            log.error("刷新 Token 失败：{}", e.getMessage());
            throw new RuntimeException("无法刷新 Token，请重新登录", e);
        }
    }

    /**
     从 HTTP 请求头中提取 Token

     前端发送请求时的格式：
     Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxxxx.yyyyy

     @param authHeader HTTP 请求头中的 Authorization 字段值
     @return Token 字符串（不包含 "Bearer " 前缀），如果不存在返回 null
     */
    public static String extractTokenFromHeader(String authHeader) {
        // 检查请求头是否存在且以 "Bearer " 开头
        if (authHeader != null && authHeader.startsWith(TOKEN_PREFIX)) {
            // 截取 "Bearer " 后面的部分，即真正的 Token
            return authHeader.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
}
