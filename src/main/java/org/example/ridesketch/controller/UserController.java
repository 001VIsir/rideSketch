package org.example.ridesketch.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户注册
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
     * 用户登录
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
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
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
     * 更新当前用户信息
     */
    @PutMapping("/me")
    public ResponseEntity<Map<String, Object>> updateCurrentUser(
            Authentication authentication,
            @RequestBody UpdateUserRequest updateUserRequest) {
        try {
            org.example.ridesketch.security.CustomUserDetails userDetails =
                    (org.example.ridesketch.security.CustomUserDetails) authentication.getPrincipal();

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
