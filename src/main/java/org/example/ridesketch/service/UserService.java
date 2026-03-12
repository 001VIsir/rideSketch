package org.example.ridesketch.service;

import org.example.ridesketch.dto.RegisterRequest;
import org.example.ridesketch.dto.LoginRequest;
import org.example.ridesketch.dto.AuthResponse;
import org.example.ridesketch.dto.UpdateUserRequest;
import org.example.ridesketch.entity.User;

public interface UserService {

    /**
     * 用户注册
     * @param registerRequest 注册请求
     * @return 注册后的用户信息
     */
    User register(RegisterRequest registerRequest);

    /**
     * 用户登录
     * @param loginRequest 登录请求
     * @return 认证响应（包含token）
     */
    AuthResponse login(LoginRequest loginRequest);

    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户信息
     */
    User findByUsername(String username);

    /**
     * 根据邮箱查找用户
     * @param email 邮箱
     * @return 用户信息
     */
    User findByEmail(String email);

    /**
     * 根据ID查找用户
     * @param id 用户ID
     * @return 用户信息
     */
    User findById(Long id);

    /**
     * 更新用户信息
     * @param userId 用户ID
     * @param updateUserRequest 更新请求
     * @return 更新后的用户信息
     */
    User updateUser(Long userId, UpdateUserRequest updateUserRequest);
}
