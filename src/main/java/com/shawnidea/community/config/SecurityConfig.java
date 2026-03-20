package com.shawnidea.community.config;

import com.shawnidea.community.util.AppConstants;
import com.shawnidea.community.util.AppUtil;
import com.shawnidea.community.service.UserService;
import com.shawnidea.community.util.HostHolder;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;

@Configuration
public class SecurityConfig implements AppConstants {

    private final LoginTicketFilter loginTicketFilter;

    public SecurityConfig(UserService userService, HostHolder hostHolder) {
        this.loginTicketFilter = new LoginTicketFilter(userService, hostHolder);
    }

    @Bean
    public LoginTicketFilter loginTicketFilter() {
        return loginTicketFilter;
    }

    @Bean
    public FilterRegistrationBean<LoginTicketFilter> loginTicketFilterRegistration() {
        FilterRegistrationBean<LoginTicketFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(loginTicketFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 授权
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                .requestMatchers(
                        "/user/setting",
                        "/user/upload",
                        "/discuss/add",
                        "/comment/add/**",
                        "/letter/**",
                        "/notice/**",
                        "/like",
                        "/follow",
                        "/unfollow"
                )
                .hasAnyAuthority(
                        AUTHORITY_USER,
                        AUTHORITY_ADMIN,
                        AUTHORITY_MODERATOR
                )
                .requestMatchers(
                        "/discuss/top",
                        "/discuss/wonderful"
                )
                .hasAnyAuthority(
                        AUTHORITY_MODERATOR
                )
                .requestMatchers(
                        "/discuss/delete",
                        "/data/**",
                        "/admin/elasticsearch/**"
                )
                .hasAnyAuthority(
                        AUTHORITY_ADMIN
                )
                .anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .addFilterBefore(loginTicketFilter, AuthorizationFilter.class);

        // 权限不够时的处理
        http.exceptionHandling(exception -> exception
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    // 没有登录
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
                        if (shouldReturnJson(request)) {
                            response.setContentType("application/json;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(AppUtil.getJSONString(403, "You are not signed in."));
                        } else {
                            response.sendRedirect(request.getContextPath() + "/login");
                        }
                    }
                })
                .accessDeniedHandler(new AccessDeniedHandler() {
                    // 权限不足
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {
                        if (shouldReturnJson(request)) {
                            response.setContentType("application/json;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(AppUtil.getJSONString(403, "You do not have permission to access this endpoint."));
                        } else {
                            response.sendRedirect(request.getContextPath() + "/denied");
                        }
                    }
                }));

        // Security底层默认会拦截/logout请求,进行退出处理.
        // 覆盖它默认的逻辑,才能执行我们自己的退出代码.
        http.logout(logout -> logout.logoutUrl("/securitylogout"));

        return http.build();
    }

    private boolean shouldReturnJson(HttpServletRequest request) {
        String xRequestedWith = request.getHeader("x-requested-with");
        if ("XMLHttpRequest".equals(xRequestedWith)) {
            return true;
        }

        String accept = request.getHeader("accept");
        if (accept != null && accept.contains("application/json")) {
            return true;
        }

        String requestUri = request.getRequestURI();
        return requestUri != null && requestUri.startsWith(
                Objects.toString(request.getContextPath(), "") + "/admin/elasticsearch/");
    }

}
