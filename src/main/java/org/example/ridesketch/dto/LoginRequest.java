package org.example.ridesketch.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

/**
 * 登录请求DTO
 * <p>
 * 用于处理用户登录请求，包含用户名或邮箱以及密码信息。
 * 用户可以使用用户名或邮箱任一方式进行登录验证。
 *
 * @author rideSketch
 * @since 1.0.0
 */
@Data
@Validated
public class LoginRequest {

    /**
     * 用户名或邮箱
     * <p>
     * 支持使用用户名或邮箱进行登录，长度不能为空。
     * 系统会根据输入内容自动识别是用户名还是邮箱。
     */
    @NotBlank(message = "用户名或邮箱不能为空")
    private String usernameOrEmail;

    /**
     * 用户密码
     * <p>
     * 用户登录密码，长度不能为空。
     * 密码在传输过程中应使用HTTPS加密保护。
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
