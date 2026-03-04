package org.example.ridesketch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ridesketch.dto.AuthResponse;
import org.example.ridesketch.dto.LoginRequest;
import org.example.ridesketch.dto.RegisterRequest;
import org.example.ridesketch.dto.UpdateUserRequest;
import org.example.ridesketch.entity.User;
import org.example.ridesketch.mapper.UserMapper;
import org.example.ridesketch.security.JwtUtils;
import org.example.ridesketch.service.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务实现类
 * 负责处理用户注册、登录、信息更新等业务逻辑
 * 使用MyBatis-Plus与数据库交互，通过Spring Security进行身份认证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtils jwtUtils;

    private final AuthenticationManager authenticationManager;

    /**
     * 用户注册
     * 校验用户名和邮箱唯一性，对密码进行加密存储
     *
     * @param registerRequest 注册请求参数（用户名、密码、邮箱、昵称）
     * @return 注册成功的用户对象（不含密码）
     * @throws RuntimeException 用户名或邮箱已存在时抛出
     */
    @Override
    public User register(RegisterRequest registerRequest) {
        // 检查用户名是否已存在
        QueryWrapper<User> usernameQuery = new QueryWrapper<>();
        usernameQuery.eq("username", registerRequest.getUsername());
        if (userMapper.selectOne(usernameQuery) != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (registerRequest.getEmail() != null && !registerRequest.getEmail().isEmpty()) {
            QueryWrapper<User> emailQuery = new QueryWrapper<>();
            emailQuery.eq("email", registerRequest.getEmail());
            if (userMapper.selectOne(emailQuery) != null) {
                throw new RuntimeException("邮箱已被注册");
            }
        }

        // 创建新用户
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setNickname(registerRequest.getNickname() != null ? registerRequest.getNickname() : registerRequest.getUsername());
        user.setStatus(1); // 正常状态

        userMapper.insert(user);
        log.info("用户注册成功: {}", user.getUsername());
        return user;
    }

    /**
     * 用户登录认证
     * 使用Spring Security进行身份验证，成功后生成JWT令牌
     *
     * @param loginRequest 登录请求参数（用户名或邮箱、密码）
     * @return 认证响应对象（包含JWT令牌和用户信息）
     * @throws BadCredentialsException 认证失败时抛出
     * @throws RuntimeException 账号已被禁用时抛出
     */
    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        // 尝试认证
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsernameOrEmail(),
                            loginRequest.getPassword()
                    )
            );
        } catch (Exception e) {
            throw new BadCredentialsException("用户名或密码错误");
        }

        // 查找用户
        User user = findByUsername(loginRequest.getUsernameOrEmail());
        if (user == null) {
            user = findByEmail(loginRequest.getUsernameOrEmail());
        }

        if (user == null) {
            throw new BadCredentialsException("用户不存在");
        }

        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }

        // 生成Token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .build();
    }

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户对象，不存在则返回null
     */
    @Override
    public User findByUsername(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        return userMapper.selectOne(queryWrapper);
    }

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱地址
     * @return 用户对象，不存在则返回null
     */
    @Override
    public User findByEmail(String email) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        return userMapper.selectOne(queryWrapper);
    }

    /**
     * 根据用户ID查询用户
     *
     * @param id 用户ID
     * @return 用户对象，不存在则返回null
     */
    @Override
    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 更新用户信息
     * 支持更新昵称、头像、邮箱，邮箱更新时校验唯一性
     *
     * @param userId 用户ID
     * @param updateUserRequest 更新请求参数
     * @return 更新后的用户对象
     * @throws RuntimeException 用户不存在或邮箱已被占用时抛出
     */
    @Override
    @Transactional
    public User updateUser(Long userId, UpdateUserRequest updateUserRequest) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 更新昵称
        if (updateUserRequest.getNickname() != null) {
            user.setNickname(updateUserRequest.getNickname());
        }

        // 更新头像
        if (updateUserRequest.getAvatar() != null) {
            user.setAvatar(updateUserRequest.getAvatar());
        }

        // 更新邮箱（需要检查唯一性）
        if (updateUserRequest.getEmail() != null && !updateUserRequest.getEmail().equals(user.getEmail())) {
            QueryWrapper<User> emailQuery = new QueryWrapper<>();
            emailQuery.eq("email", updateUserRequest.getEmail());
            emailQuery.ne("id", userId);
            if (userMapper.selectOne(emailQuery) != null) {
                throw new RuntimeException("邮箱已被其他用户使用");
            }
            user.setEmail(updateUserRequest.getEmail());
        }

        userMapper.updateById(user);
        return user;
    }
}
