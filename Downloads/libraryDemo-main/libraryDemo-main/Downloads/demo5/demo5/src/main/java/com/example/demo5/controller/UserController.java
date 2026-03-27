package com.example.demo5.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

import com.example.demo5.entity.AppUser;
import com.example.demo5.enums.UserRole;
import com.example.demo5.repository.AppUserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
        AppUser saved = appUserRepository.save(user);
        if (saved.getId() != null && saved.getId() == 1L && saved.getRole() != UserRole.ADMIN) {
            saved.setRole(UserRole.ADMIN);
            appUserRepository.save(saved);
        }

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

        if (user.getId() != null && user.getId() == 1L && user.getRole() != UserRole.ADMIN) {
            user.setRole(UserRole.ADMIN);
            user = appUserRepository.save(user);
        }

        UserRole actualRole = user.getRole() == null ? UserRole.USER : user.getRole();
        UserRole requestedRole = parseRole(request.loginRole());
        if (requestedRole == UserRole.ADMIN && actualRole != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiMessage("该账号不是管理员，无法使用管理员登录"));
        }

        return ResponseEntity.ok(new LoginResponse("登录成功", new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                actualRole
        )));
    }

    @GetMapping("/users/{username}/profile")
    public ResponseEntity<?> profile(@PathVariable String username) {
        AppUser user = appUserRepository.findByUsernameIgnoreCase(trim(username)).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiMessage("用户不存在"));
        }
        if (user.getId() != null && user.getId() == 1L && user.getRole() != UserRole.ADMIN) {
            user.setRole(UserRole.ADMIN);
            user = appUserRepository.save(user);
        }
        return ResponseEntity.ok(new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getRole() == null ? UserRole.USER : user.getRole()
        ));
    }

    @GetMapping("/admin/users")
    public ResponseEntity<?> listUsersForAdmin(@RequestHeader(value = "X-Auth-Username", required = false) String authUsername) {
        ResponseEntity<ApiMessage> adminCheck = requireAdmin(authUsername);
        if (adminCheck != null) {
            return adminCheck;
        }

        List<UserAdminResponse> users = appUserRepository.findAll().stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .map(user -> new UserAdminResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getRole() == null ? UserRole.USER : user.getRole(),
                        user.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/admin/users/{id}/role")
    public ResponseEntity<?> updateUserRole(@RequestHeader(value = "X-Auth-Username", required = false) String authUsername,
                                            @PathVariable Long id,
                                            @RequestBody UpdateUserRoleRequest request) {
        AppUser actor = requireAdminUser(authUsername);
        if (actor == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiMessage("仅管理员可访问后台管理"));
        }

        AppUser target = appUserRepository.findById(id).orElse(null);
        if (target == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiMessage("用户不存在"));
        }

        UserRole targetRole = parseRole(request.role());
        if (targetRole == null) {
            return ResponseEntity.badRequest().body(new ApiMessage("角色不合法"));
        }

        if (target.getId() != null && target.getId() == 1L && targetRole != UserRole.ADMIN) {
            return ResponseEntity.badRequest().body(new ApiMessage("id=1 必须保持管理员身份"));
        }

        target.setRole(targetRole);
        AppUser saved = appUserRepository.save(target);
        return ResponseEntity.ok(new UserAdminResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getRole() == null ? UserRole.USER : saved.getRole(),
                saved.getCreatedAt()
        ));
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

    private UserRole parseRole(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UserRole.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private ResponseEntity<ApiMessage> requireAdmin(String authUsername) {
        AppUser user = requireAdminUser(authUsername);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiMessage("仅管理员可访问后台管理"));
        }
        return null;
    }

    private AppUser requireAdminUser(String authUsername) {
        String username = trim(authUsername);
        if (username.isEmpty()) {
            return null;
        }
        AppUser user = appUserRepository.findByUsernameIgnoreCase(username).orElse(null);
        if (user == null) {
            return null;
        }
        UserRole role = user.getRole() == null ? UserRole.USER : user.getRole();
        return role == UserRole.ADMIN ? user : null;
    }

    public record RegisterRequest(String username, String password) {
    }

    public record LoginRequest(String username, String password, String loginRole) {
    }

    public record ApiMessage(String message) {
    }

    public record UpdateUserRoleRequest(String role) {
    }

    public record UserProfileResponse(Long id, String username, UserRole role) {
    }

    public record UserAdminResponse(Long id, String username, UserRole role, java.time.LocalDateTime createdAt) {
    }

    public record LoginResponse(String message, UserProfileResponse user) {
    }
}
