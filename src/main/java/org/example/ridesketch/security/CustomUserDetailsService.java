package org.example.ridesketch.security;

import lombok.RequiredArgsConstructor;
import org.example.ridesketch.entity.User;
import org.example.ridesketch.mapper.UserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 自定义用户详情服务实现类
 *
 * <p>实现Spring Security的UserDetailsService接口，负责根据用户名（或邮箱）加载用户信息。
 * 该类是Spring Security认证流程中的关键组件，当需要进行用户身份验证时，
 * Spring Security会调用此服务来获取用户详情。</p>
 *
 * <p>功能说明：</p>
 * <ul>
 *   <li>支持通过用户名或邮箱进行用户查找</li>
 *   <li>将User实体转换为Spring Security所需的UserDetails对象</li>
 *   <li>用户不存在时抛出UsernameNotFoundException异常</li>
 * </ul>
 *
 * <p>使用场景：</p>
 * <ul>
 *   <li>用户登录时验证用户名和密码</li>
 *   <li>JWT认证过滤器中根据token解析出的用户名加载用户信息</li>
 *   <li>任何需要获取当前登录用户详情的场景</li>
 * </ul>
 *
 * @author rideSketch
 * @version 1.0
 * @see UserDetailsService
 * @see CustomUserDetails
 * @see org.example.ridesketch.entity.User
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    /** 用户Mapper，用于数据库查询 */
    private final UserMapper userMapper;

    /**
     * 根据用户名加载用户详情
     *
     * <p>实现UserDetailsService接口的方法，根据用户名（或邮箱）从数据库中查询用户信息。
     * 该方法支持两种登录方式：用户名或邮箱。</p>
     *
     * <p>查询逻辑：</p>
     * <ol>
     *   <li>首先尝试按username字段查询</li>
     *   <li>如果未找到，则按email字段查询</li>
     *   <li>如果仍未找到，抛出UsernameNotFoundException</li>
     * </ol>
     *
     * @param username 用户名或邮箱地址
     * @return UserDetails对象，包含用户信息和权限
     * @throws UsernameNotFoundException 当用户不存在时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 使用MyBatis-Plus构建查询条件：username = ? OR email = ?
        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                        .eq("username", username)  // 按用户名查询
                        .or()
                        .eq("email", username)     // 或按邮箱查询
        );

        // 用户不存在，抛出Spring Security标准的异常
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 将User实体包装为CustomUserDetails对象并返回
        return new CustomUserDetails(user);
    }
}
