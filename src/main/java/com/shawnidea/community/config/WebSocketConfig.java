package com.shawnidea.community.config;

import com.shawnidea.community.websocket.JwtHandshakeInterceptor;
import com.shawnidea.community.websocket.MessageWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private MessageWebSocketHandler messageWebSocketHandler;

    @Autowired
    private JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Value("${community.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(messageWebSocketHandler, "/ws/messages")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .toArray(String[]::new));
    }
}
