package org.example.ridesketch.config;

import lombok.RequiredArgsConstructor;
import org.example.ridesketch.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 安全配置类
 *
 * 配置JWT认证、CORS跨域、请求授权等安全策略
 *
 * @author rideSketch
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** JWT认证过滤器 */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** 用户详情服务 */
    private final UserDetailsService userDetailsService;

    /**
     * 安全过滤器链配置
     *
     * 配置包括：
     * - 禁用CSRF（前后端分离使用JWT无需CSRF防护）
     * - 启用CORS（允许跨域请求）
     * - 无状态会话（JWT无需Session）
     * - 路径权限配置（公开接口vs需认证接口）
     *
     * @param http HttpSecurity对象
     * @return 安全过滤器链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 禁用CSRF（前后端分离使用JWT，无需CSRF防护）
                .csrf(AbstractHttpConfigurer::disable)
                // 2. 配置CORS（允许前端跨域访问）
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 3. 无状态会话（JWT是无状态的，不需要Session）
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 4. 请求授权配置
                .authorizeHttpRequests(auth -> auth
                        // 公开接口：认证、地图、路线查询、社区浏览、RAG问答
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/map/**",
                                "/api/route/**",
                                "/api/rag/**",
                                "/api/chroma/**",
                                "/api/agent/**",
                                "/api/community/routes",
                                "/api/community/route/{id}",
                                "/api/community/route/{id}/comments",
                                "/api/public/**",
                                "/error"
                        ).permitAll()
                        // 其他接口需要登录认证
                        .anyRequest().authenticated()
                )
                // 5. 认证提供者
                .authenticationProvider(authenticationProvider())
                // 6. JWT过滤器（在用户名密码认证之前执行）
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS跨域配置
     *
     * 允许前端应用跨域访问后端API
     *
     * @return CORS配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许的前端Origin（开发环境：5173端口，生产需配置实际域名）
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 允许所有请求头
        configuration.setAllowedHeaders(List.of("*"));
        // 允许携带凭证（Cookie、Authorization头等）
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 认证提供者配置
     *
     * 使用DaoAuthenticationProvider进行用户名密码认证
     *
     * @return 认证提供者
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * 认证管理器Bean
     *
     * 用于处理用户登录认证
     *
     * @param config 认证配置
     * @return 认证管理器
     * @throws Exception 获取异常
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 密码编码器Bean
     *
     * 使用BCrypt算法对密码进行加密和验证
     * BCrypt会自动生成随机盐，每次加密结果不同
     *
     * @return 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
