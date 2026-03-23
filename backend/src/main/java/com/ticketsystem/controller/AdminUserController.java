package com.ticketsystem.controller;

import com.ticketsystem.dto.UserProfileResponse;
import com.ticketsystem.entity.User;
import com.ticketsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        List<UserProfileResponse> users = userRepository.findAll().stream()
                .map(u -> UserProfileResponse.builder()
                        .username(u.getUsername())
                        .email(u.getEmail())
                        .fullName(u.getFullName())
                        .phone(u.getPhone())
                        .role(u.getRole().name())
                        .isEnabled(u.isEnabled())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{username}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable String username, Authentication authentication) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        // Preventions
        if (user.getUsername().equals(authentication.getName())) {
            throw new RuntimeException("Bạn không thể tự vô hiệu hoá tài khoản của chính mình!");
        }

        // Cannot disable the super admin seeded account "admin"
        if ("admin".equals(user.getUsername())) {
            throw new RuntimeException("Không thể vô hiệu hoá tài khoản Super Admin!");
        }

        user.setEnabled(!user.isEnabled());
        userRepository.save(user);

        return ResponseEntity.ok(java.util.Map.of("message", "Đã cập nhật trạng thái người dùng"));
    }
}
