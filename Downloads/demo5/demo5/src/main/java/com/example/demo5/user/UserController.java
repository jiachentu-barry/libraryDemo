package com.example.demo5.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {

    private final AppUserRepository appUserRepository;

    public UserController(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @PostMapping("/users/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        String username = trim(request.username());
        String password = trim(request.password());

        String validation = validate(username, password);
        if (validation != null) {
            return ResponseEntity.badRequest().body(new ApiMessage(validation));
        }

        if (appUserRepository.existsByUsernameIgnoreCase(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiMessage("用户名已存在"));
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(sha256(password));
        appUserRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiMessage("注册成功"));
    }

    @PostMapping("/users/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String username = trim(request.username());
        String password = trim(request.password());

        if (username.isEmpty() || password.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiMessage("用户名和密码不能为空"));
        }

        AppUser user = appUserRepository.findByUsernameIgnoreCase(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiMessage("用户名或密码错误"));
        }

        String inputHash = sha256(password);
        if (!inputHash.equals(user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiMessage("用户名或密码错误"));
        }

        return ResponseEntity.ok(new ApiMessage("登录成功"));
    }

    private String validate(String username, String password) {
        if (username.isEmpty()) {
            return "用户名不能为空";
        }
        if (!username.matches("^[a-zA-Z0-9_]{3,20}$")) {
            return "用户名需为 3-20 位字母、数字或下划线";
        }
        if (password.length() < 6 || password.length() > 50) {
            return "密码长度需为 6-50 位";
        }
        return null;
    }

    private String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not supported", ex);
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    public record RegisterRequest(String username, String password) {
    }

    public record LoginRequest(String username, String password) {
    }

    public record ApiMessage(String message) {
    }
}
