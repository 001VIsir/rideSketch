package org.example.ridesketch.service;

import org.example.ridesketch.dto.RegisterRequest;
import org.example.ridesketch.dto.LoginRequest;
import org.example.ridesketch.dto.AuthResponse;
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
}
