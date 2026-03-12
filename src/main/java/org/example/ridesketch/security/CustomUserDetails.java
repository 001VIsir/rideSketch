package org.example.ridesketch.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.ridesketch.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * 自定义用户详情实现类
 *
 * <p>实现Spring Security的UserDetails接口，将项目中的User实体包装为Spring Security
 * 能够识别的用户详情对象。该类包含了用户的基本信息以及Spring Security所需的各种账户状态标志。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>提供用户名和密码用于身份验证</li>
 *   <li>提供用户权限信息（角色）</li>
 *   <li>提供账户状态信息（是否启用、是否锁定、是否过期等）</li>
 * </ul>
 *
 * <p>账户状态说明：</p>
 * <ul>
 *   <li>isEnabled - 用户是否启用，通过User实体的status字段判断（1为启用）</li>
 *   <li>isAccountNonLocked - 账户是否未锁定（本项目暂未实现锁定功能，恒返回true）</li>
 *   <li>isAccountNonExpired - 账户是否未过期（本项目暂未实现过期功能，恒返回true）</li>
 *   <li>isCredentialsNonExpired - 凭证是否未过期（本项目暂未实现过期功能，恒返回true）</li>
 * </ul>
 *
 * @author rideSketch
 * @version 1.0
 * @see UserDetails
 * @see org.example.ridesketch.entity.User
 */
@Getter
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    /** 持有的用户实体对象，包含完整的用户信息 */
    private final User user;

    /**
     * 获取用户的权限集合
     *
     * <p>实现UserDetails接口的方法，返回用户具有的权限。
     * 本项目中所有用户默认具有ROLE_USER角色。</p>
     *
     * @return 包含用户权限的集合
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 返回一个包含ROLE_USER角色的集合
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    /**
     * 获取用户密码
     *
     * <p>用于Spring Security进行密码验证。</p>
     *
     * @return 用户的加密密码
     */
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /**
     * 获取用户名
     *
     * <p>用于Spring Security进行用户名验证。</p>
     *
     * @return 用户名
     */
    @Override
    public String getUsername() {
        return user.getUsername();
    }

    /**
     * 检查账户是否未过期
     *
     * <p>本项目暂未实现账户过期功能，恒返回true。</p>
     *
     * @return 始终返回true（账户未过期）
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 检查账户是否未锁定
     *
     * <p>本项目暂未实现账户锁定功能，恒返回true。</p>
     *
     * @return 始终返回true（账户未锁定）
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 检查凭证（密码）是否未过期
     *
     * <p>本项目暂未实现密码过期功能，恒返回true。</p>
     *
     * @return 始终返回true（凭证未过期）
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 检查账户是否启用
     *
     * <p>根据User实体的status字段判断账户是否启用。
     * status为1时表示启用，其他值表示未启用。</p>
     *
     * @return 账户启用状态（true:启用, false:未启用）
     */
    @Override
    public boolean isEnabled() {
        return user.getStatus() == 1;
    }

    /**
     * 获取用户ID
     *
     * <p>扩展方法，方便在其他业务代码中直接获取用户ID。</p>
     *
     * @return 用户ID
     */
    public Long getId() {
        return user.getId();
    }
}
