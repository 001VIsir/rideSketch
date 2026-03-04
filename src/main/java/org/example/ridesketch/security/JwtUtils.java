package org.example.ridesketch.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 *
 * <p>提供JWT令牌的生成、解析和验证功能。
 * 该类使用HS256算法进行签名，确保令牌的安全性。
 * 令牌中包含用户ID和用户名信息，用于身份认证。</p>
 *
 * <p>配置项：</p>
 * <ul>
 *   <li>jwt.secret - JWT签名密钥</li>
 *   <li>jwt.expiration - 令牌有效期（毫秒）</li>
 * </ul>
 *
 * @author rideSketch
 * @version 1.0
 */
@Component
public class JwtUtils {

    /** JWT签名密钥，从配置文件中读取 */
    @Value("${jwt.secret}")
    private String secret;

    /** JWT令牌有效期（毫秒），从配置文件中读取 */
    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * 获取签名密钥
     *
     * <p>使用HMAC-SHA算法创建签名密钥，确保密钥与配置的secret匹配。</p>
     *
     * @return 用于JWT签名和验证的SecretKey对象
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成JWT Token
     *
     * <p>创建一个包含用户信息的JWT令牌，令牌有效期从创建时刻开始计算。</p>
     *
     * @param userId 用户ID，用于在令牌中标识用户
     * @param username 用户名，作为令牌的主题
     * @return JWT Token字符串
     */
    public String generateToken(Long userId, String username) {
        // 创建声明Map，存储用户ID和用户名
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        return createToken(claims, username);
    }

    /**
     * 创建Token
     *
     * <p>构建完整的JWT令牌，包含声明、主题、发行时间和过期时间。</p>
     *
     * @param claims 声明Map，包含自定义Claims（如userId, username）
     * @param subject 主题，通常为用户名
     * @return JWT Token字符串
     */
    private String createToken(Map<String, Object> claims, String subject) {
        // 当前时间
        Date now = new Date();
        // 计算过期时间 = 当前时间 + 有效期
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)           // 设置自定义声明
                .subject(subject)        // 设置主题（用户名）
                .issuedAt(now)            // 设置签发时间
                .expiration(expiryDate)   // 设置过期时间
                .signWith(getSigningKey()) // 使用密钥签名
                .compact();               // 生成紧凑的URL-safe字符串
    }

    /**
     * 解析Token获取用户名
     *
     * <p>从JWT令牌中提取主题（username）信息。</p>
     *
     * @param token JWT Token字符串
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * 解析Token获取用户ID
     *
     * <p>从JWT令牌中提取用户ID信息。</p>
     *
     * @param token JWT Token字符串
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 解析Token获取声明
     *
     * <p>解析JWT令牌并返回所有Claims信息。此方法是私有方法，
     * 供其他解析方法内部调用。</p>
     *
     * @param token JWT Token字符串
     * @return Claims对象，包含所有声明信息
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // 使用密钥验证签名
                .build()
                .parseSignedClaims(token)    // 解析并验证签名
                .getPayload();               // 获取Payload内容
    }

    /**
     * 验证Token是否有效
     *
     * <p>验证JWT令牌的签名是否有效且未过期。
     * 如果令牌格式错误、签名不匹配或已过期，返回false。</p>
     *
     * @param token JWT Token字符串
     * @return 是否有效（true:有效, false:无效或已过期）
     */
    public boolean validateToken(String token) {
        try {
            // 尝试解析令牌，如果成功则检查是否过期
            Claims claims = getClaimsFromToken(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            // 解析失败（签名错误、格式错误等），返回false
            return false;
        }
    }

    /**
     * 检查Token是否过期
     *
     * <p>比较令牌的过期时间与当前时间，判断令牌是否已过期。</p>
     *
     * @param token JWT Token字符串
     * @return 是否过期（true:已过期, false:未过期）
     */
    private boolean isTokenExpired(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration().before(new Date());
    }
}
