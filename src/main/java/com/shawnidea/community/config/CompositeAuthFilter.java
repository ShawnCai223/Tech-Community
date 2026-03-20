package com.shawnidea.community.config;

import com.shawnidea.community.entity.LoginTicket;
import com.shawnidea.community.entity.User;
import com.shawnidea.community.service.UserService;
import com.shawnidea.community.util.CookieUtil;
import com.shawnidea.community.util.HostHolder;
import com.shawnidea.community.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;

/**
 * Composite authentication filter that supports both JWT (for API clients)
 * and cookie-based LoginTicket (for legacy Thymeleaf pages).
 *
 * Priority: JWT Bearer token > Cookie ticket
 */
public class CompositeAuthFilter extends OncePerRequestFilter {

    private final UserService userService;
    private final HostHolder hostHolder;
    private final JwtUtil jwtUtil;

    public CompositeAuthFilter(UserService userService, HostHolder hostHolder, JwtUtil jwtUtil) {
        this.userService = userService;
        this.hostHolder = hostHolder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            User user = authenticateByJwt(request);
            if (user == null) {
                user = authenticateByCookie(request);
            }

            if (user != null) {
                hostHolder.setUser(user);
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        user, user.getPassword(), userService.getAuthorities(user.getId()));
                SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            }

            filterChain.doFilter(request, response);
        } finally {
            hostHolder.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private User authenticateByJwt(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authHeader.substring(7);
        Claims claims = jwtUtil.validateAccessToken(token);
        if (claims == null) {
            return null;
        }

        int userId = Integer.parseInt(claims.getSubject());
        return userService.findUserById(userId);
    }

    private User authenticateByCookie(HttpServletRequest request) {
        String ticket = CookieUtil.getValue(request, "ticket");
        if (ticket == null) {
            return null;
        }

        LoginTicket loginTicket = userService.findLoginTicket(ticket);
        if (loginTicket == null || loginTicket.getStatus() != 0 || !loginTicket.getExpired().after(new Date())) {
            return null;
        }

        return userService.findUserById(loginTicket.getUserId());
    }

}
