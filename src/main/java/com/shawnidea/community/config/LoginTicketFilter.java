package com.shawnidea.community.config;

import com.shawnidea.community.entity.LoginTicket;
import com.shawnidea.community.entity.User;
import com.shawnidea.community.service.UserService;
import com.shawnidea.community.util.CookieUtil;
import com.shawnidea.community.util.HostHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;

public class LoginTicketFilter extends OncePerRequestFilter {

    private final UserService userService;

    private final HostHolder hostHolder;

    public LoginTicketFilter(UserService userService, HostHolder hostHolder) {
        this.userService = userService;
        this.hostHolder = hostHolder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String ticket = CookieUtil.getValue(request, "ticket");
            if (ticket != null) {
                LoginTicket loginTicket = userService.findLoginTicket(ticket);
                if (loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())) {
                    User user = userService.findUserById(loginTicket.getUserId());
                    if (user != null) {
                        hostHolder.setUser(user);
                        Authentication authentication = new UsernamePasswordAuthenticationToken(
                                user, user.getPassword(), userService.getAuthorities(user.getId()));
                        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
                    }
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            hostHolder.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
