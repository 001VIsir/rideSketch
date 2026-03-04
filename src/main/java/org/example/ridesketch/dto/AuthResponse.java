package org.example.ridesketch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证响应DTO
 * <p>
 * 用户登录或注册成功后返回的认证信息，包含JWT令牌和用户基本信息。
 * 客户端需要将token保存在本地，用于后续接口的身份验证。
 *
 * @author rideSketch
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /**
     * JWT认证令牌
     * <p>
     * 用于后续接口请求的身份验证，格式为Bearer Token。
     * 令牌包含过期时间，请在过期前刷新或重新登录。
     */
    private String token;

    /**
     * 用户ID
     * <p>
     * 用户在系统中的唯一标识符，用于关联用户数据。
     */
    private Long userId;

    /**
     * 用户名
     * <p>
     * 用户登录时使用的唯一标识。
     */
    private String username;

    /**
     * 用户昵称
     * <p>
     * 用户在社区中显示的名称，可为空。
     */
    private String nickname;

    /**
     * 头像URL
     * <p>
     * 用户头像的访问地址，可为空表示使用默认头像。
     */
    private String avatar;
}
