package org.example.ridesketch.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求DTO
 * <p>
 * 用于处理新用户注册请求，包含用户名、邮箱、密码和昵称信息。
 * 所有字段将在后端进行验证，确保数据合法性和安全性。
 *
 * @author rideSketch
 * @since 1.0.0
 */
@Data
public class RegisterRequest {

    /**
     * 用户名
     * <p>
     * 用于登录的唯一标识，长度要求3-50个字符之间。
     * 用户名在系统中必须保持唯一性。
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    private String username;

    /**
     * 邮箱地址
     * <p>
     * 用于接收通知和找回密码，必须为有效的邮箱格式。
     * 邮箱在系统中必须保持唯一性。
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 登录密码
     * <p>
     * 用户登录密码，长度要求6-100个字符之间。
     * 建议使用包含大小写字母、数字和特殊字符的强密码。
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
    private String password;

    /**
     * 用户昵称
     * <p>
     * 可选的显示名称，用于社区展示。
     * 如果未提供，系统将自动生成默认昵称。
     */
    private String nickname;
}
