package org.example.ridesketch.dto;

import lombok.Data;

/**
 * 用户信息更新请求DTO
 * <p>
 * 用于处理用户个人信息更新请求，支持更新昵称、头像和邮箱。
 * 所有字段均为可选字段，只提交需要更新的内容。
 *
 * @author rideSketch
 * @since 1.0.0
 */
@Data
public class UpdateUserRequest {

    /**
     * 用户昵称
     * <p>
     * 可选的显示名称，用于社区展示。
     * 如果需要清除昵称，请提交空字符串。
     */
    private String nickname;

    /**
     * 头像URL
     * <p>
     * 用户头像的访问地址，支持HTTP/HTTPS URL。
     * 如果需要清除头像，请提交空字符串。
     */
    private String avatar;

    /**
     * 邮箱地址
     * <p>
     * 可选的邮箱地址，用于接收通知和找回密码。
     * 如果填写，必须为有效的邮箱格式且在系统中保持唯一性。
     */
    private String email;
}
