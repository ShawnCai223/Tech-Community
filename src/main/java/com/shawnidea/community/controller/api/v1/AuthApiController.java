package com.shawnidea.community.controller.api.v1;

import com.google.code.kaptcha.Producer;
import com.shawnidea.community.dto.ApiResponse;
import com.shawnidea.community.entity.User;
import com.shawnidea.community.service.UserService;
import com.shawnidea.community.util.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthApiController implements AppConstants {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private Producer kaptchaProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/captcha")
    public ApiResponse<Map<String, String>> getCaptcha() throws IOException {
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        String captchaOwner = AppUtil.generateUUID();
        String redisKey = RedisKeyUtil.getKaptchaKey(captchaOwner);
        redisTemplate.opsForValue().set(redisKey, text, 60, TimeUnit.SECONDS);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        String base64Image = "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());

        Map<String, String> data = new HashMap<>();
        data.put("captchaOwner", captchaOwner);
        data.put("captchaImage", base64Image);
        return ApiResponse.ok(data);
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String email = body.get("email");

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);

        Map<String, Object> result = userService.register(user);
        if (result == null || result.isEmpty()) {
            return ApiResponse.ok();
        }

        String errorMsg = result.values().stream()
                .filter(v -> v != null)
                .map(Object::toString)
                .findFirst()
                .orElse("Registration failed.");
        return ApiResponse.error(400, errorMsg);
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String captchaCode = body.get("captchaCode");
        String captchaOwner = body.get("captchaOwner");
        boolean rememberMe = "true".equals(body.get("rememberMe"));

        // Validate captcha
        String kaptcha = null;
        if (StringUtils.isNotBlank(captchaOwner)) {
            String redisKey = RedisKeyUtil.getKaptchaKey(captchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }
        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(captchaCode) || !captchaCode.equalsIgnoreCase(kaptcha)) {
            return ApiResponse.error(400, "Incorrect verification code.");
        }

        // Validate credentials via existing UserService
        long expiredSeconds = rememberMe ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> loginResult = userService.login(username, password, expiredSeconds);

        if (!loginResult.containsKey("ticket")) {
            String errorMsg = loginResult.values().stream()
                    .filter(v -> v != null)
                    .map(Object::toString)
                    .findFirst()
                    .orElse("Login failed.");
            return ApiResponse.error(400, errorMsg);
        }

        // Login succeeded - generate JWT tokens
        User user = userService.findUserByName(username);
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", accessToken);
        data.put("refreshToken", refreshToken);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("headerUrl", user.getHeaderUrl());
        userInfo.put("type", user.getType());
        data.put("user", userInfo);

        return ApiResponse.ok(data);
    }

    @PostMapping("/refresh")
    public ApiResponse<Map<String, String>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (StringUtils.isBlank(refreshToken)) {
            return ApiResponse.error(400, "Refresh token is required.");
        }

        Integer userId = jwtUtil.validateRefreshToken(refreshToken);
        if (userId == null) {
            return ApiResponse.error(401, "Invalid or expired refresh token.");
        }

        User user = userService.findUserById(userId);
        if (user == null) {
            return ApiResponse.error(401, "User not found.");
        }

        String newAccessToken = jwtUtil.generateAccessToken(user);

        Map<String, String> data = new HashMap<>();
        data.put("accessToken", newAccessToken);
        return ApiResponse.ok(data);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (StringUtils.isNotBlank(refreshToken)) {
            jwtUtil.revokeRefreshToken(refreshToken);
        }
        return ApiResponse.ok();
    }

}
