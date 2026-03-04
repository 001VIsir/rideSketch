package org.example.ridesketch.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 *
 * <p>Spring Security过滤器链中的一环，负责处理JWT token的身份认证。
 * 该过滤器拦截每个HTTP请求，从请求头中提取JWT token，验证其有效性，
 * 并将用户信息设置到Spring Security上下文中。</p>
 *
 * <p>工作流程：</p>
 * <ol>
 *   <li>从HTTP请求头中提取Authorization header</li>
 *   <li>解析Bearer token，提取JWT字符串</li>
 *   <li>验证JWT token的有效性（签名、过期时间）</li>
 *   <li>从token中获取用户名，加载用户详情</li>
 *   <li>创建Authentication对象并设置到SecurityContext</li>
 *   <li>继续执行过滤器链</li>
 * </ol>
 *
 * <p>注意：此过滤器仅处理携带有效JWT的请求，对于无token或无效token的请求，
 * 不会设置认证信息，具体的访问控制由后续的过滤器（如ExceptionTranslationFilter）处理。</p>
 *
 * @author rideSketch
 * @version 1.0
 * @see OncePerRequestFilter
 * @see JwtUtils
 * @see UserDetailsService
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** JWT工具类，用于token的生成、解析和验证 */
    private final JwtUtils jwtUtils;

    /** 用户详情服务，用于根据用户名加载用户信息 */
    private final UserDetailsService userDetailsService;

    /**
     * 处理请求的核心过滤方法
     *
     * <p>该方法在每个HTTP请求时都会被调用，执行JWT认证流程。
     * 如果请求包含有效的JWT token，则将用户认证信息设置到SecurityContext中。</p>
     *
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param filterChain 过滤器链，用于将请求传递给下一个过滤器
     * @throws ServletException 如果servlet处理请求时发生错误
     * @throws IOException 如果发生I/O错误
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            // 从请求中提取JWT token
            String jwt = getJwtFromRequest(request);

            // 验证token有效性并提取用户信息
            if (StringUtils.hasText(jwt) && jwtUtils.validateToken(jwt)) {
                // 从token中获取用户名
                String username = jwtUtils.getUsernameFromToken(jwt);

                // 根据用户名加载用户详情
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // 创建认证令牌（密码为null，因为使用token认证）
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,      // 用户详情对象
                                null,             // 凭证（JWT认证不需要密码）
                                userDetails.getAuthorities() // 用户权限列表
                        );

                // 设置认证请求的详细信息（IP地址、Session ID等）
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 将认证对象设置到Spring Security上下文中
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            // 认证过程中发生任何异常，记录日志但不中断请求处理
            // 这样未认证的请求会被后续的AccessDeniedHandler或AuthenticationEntryPoint处理
            logger.error("Could not set user authentication in security context", ex);
        }

        // 继续执行过滤器链中的下一个过滤器
        filterChain.doFilter(request, response);
    }

    /**
     * 从HTTP请求中提取JWT Token
     *
     * <p>从请求头的Authorization字段中提取Bearer token。
     * 标准的Authorization头格式为：Bearer {token}</p>
     *
     * @param request HTTP请求对象
     * @return JWT token字符串，如果不存在或格式不正确则返回null
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        // 从请求头获取Authorization字段
        String bearerToken = request.getHeader("Authorization");

        // 检查token是否存在且以"Bearer "开头
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // 去掉"Bearer "前缀，返回实际的token
            return bearerToken.substring(7);
        }
        return null;
    }
}
