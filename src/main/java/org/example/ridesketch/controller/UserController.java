package org.example.ridesketch.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ridesketch.dto.AuthResponse;
import org.example.ridesketch.dto.LoginRequest;
import org.example.ridesketch.dto.RegisterRequest;
import org.example.ridesketch.dto.UpdateUserRequest;
import org.example.ridesketch.entity.User;
import org.example.ridesketch.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户认证控制器
 * <p>
 * 处理用户注册、登录、个人信息获取和更新等认证相关功能。
 * 所有接口前缀为 /api/auth
 *
 * @author rideSketch
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户注册接口
     * <p>
     * 注册新用户账号，包含用户名、邮箱、密码等信息的校验与存储。
     *
     * @param registerRequest 注册请求体，包含username、email、password、nickname(可选)字段
     *                        - username: 用户名（必填，3-20字符）
     *                        - email: 邮箱（必填，有效邮箱格式）
     *                        - password: 密码（必填，6-20字符）
     *                        - nickname: 昵称（可选，默认与用户名相同）
     * @return ResponseEntity 包含success、message、data字段
     *         - success: 注册是否成功
     *         - message: 操作消息
     *         - data: 注册成功的用户信息（id、username、email、nickname）
     * @throws Exception 注册失败时抛出异常（用户名已存在、邮箱已注册等）
     * @see RegisterRequest
     * @see org.example.ridesketch.entity.User
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            User user = userService.register(registerRequest);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "注册成功");
            response.put("data", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "nickname", user.getNickname()
            ));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 用户登录接口
     * <p>
     * 使用用户名或邮箱登录系统，返回JWT令牌用于后续接口的身份验证。
     * 登录成功后会在响应中返回accessToken令牌。
     *
     * @param loginRequest 登录请求体，包含usernameOrEmail和password字段
     *                     - usernameOrEmail: 用户名或邮箱（必填）
     *                     - password: 密码（必填）
     * @return ResponseEntity 包含success、message、data字段
     *         - success: 登录是否成功
     *         - message: 操作消息
     *         - data: 登录成功后的用户信息和令牌
     *           - id: 用户ID
     *           - username: 用户名
     *           - nickname: 昵称
     *           - email: 邮箱
     *           - accessToken: JWT访问令牌（用于后续请求的Authorization头）
     * @throws Exception 登录失败时抛出异常（用户名/密码错误、账号被禁用等）
     * @see LoginRequest
     * @see org.example.ridesketch.dto.AuthResponse
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            AuthResponse authResponse = userService.login(loginRequest);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "登录成功");
            response.put("data", authResponse);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取当前登录用户信息接口
     * <p>
     * 获取当前已登录用户的详细信息。此接口需要用户登录后才能访问，
     * 通过JWT令牌进行身份验证。
     *
     * @param authentication Spring Security提供的认证对象，包含当前用户的安全信息
     * @return ResponseEntity 包含success和data字段
     *         - success: 请求是否成功
     *         - data: 当前用户详细信息
     *           - id: 用户ID
     *           - username: 用户名
     *           - nickname: 昵称（可能为空）
     *           - email: 邮箱（可能为空）
     *           - avatar: 头像URL（可能为空）
     * @throws AuthenticationException 用户未登录或令牌无效时抛出
     * @see org.example.ridesketch.entity.User
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        // 从Authentication对象中获取用户详情，提取用户ID
        org.example.ridesketch.security.CustomUserDetails userDetails =
                (org.example.ridesketch.security.CustomUserDetails) authentication.getPrincipal();

        // 通过 UserService 重新获取用户信息，确保数据最新
        User user = userService.findById(userDetails.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname() != null ? user.getNickname() : "");
        data.put("email", user.getEmail() != null ? user.getEmail() : "");
        data.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    /**
     * 更新当前登录用户信息接口
     * <p>
     * 更新当前已登录用户的个人信息，包括昵称、邮箱、头像等。
     * 此接口需要用户登录后才能访问。
     *
     * @param authentication Spring Security提供的认证对象
     * @param updateUserRequest 更新请求体，包含nickname、email、avatar等可更新字段
     *                          - nickname: 新昵称（可选）
     *                          - email: 新邮箱（可选，有效邮箱格式）
     *                          - avatar: 头像URL（可选）
     * @return ResponseEntity 包含success、message、data字段
     *         - success: 更新是否成功
     *         - message: 操作消息
     *         - data: 更新后的用户信息（id、username、nickname、email、avatar）
     * @throws Exception 更新失败时抛出异常（邮箱已被使用等）
     * @see UpdateUserRequest
     * @see org.example.ridesketch.entity.User
     */
    @PutMapping("/me")
    public ResponseEntity<Map<String, Object>> updateCurrentUser(
            Authentication authentication,
            @RequestBody UpdateUserRequest updateUserRequest) {
        try {
            // 获取当前登录用户的安全详情
            org.example.ridesketch.security.CustomUserDetails userDetails =
                    (org.example.ridesketch.security.CustomUserDetails) authentication.getPrincipal();

            // 调用服务层更新用户信息
            User updatedUser = userService.updateUser(userDetails.getId(), updateUserRequest);

            Map<String, Object> data = new HashMap<>();
            data.put("id", updatedUser.getId());
            data.put("username", updatedUser.getUsername());
            data.put("nickname", updatedUser.getNickname() != null ? updatedUser.getNickname() : "");
            data.put("email", updatedUser.getEmail() != null ? updatedUser.getEmail() : "");
            data.put("avatar", updatedUser.getAvatar() != null ? updatedUser.getAvatar() : "");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "更新成功");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
