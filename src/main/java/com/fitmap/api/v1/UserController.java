package com.fitmap.api.v1;

import com.fitmap.domain.user.User;
import com.fitmap.repository.CreditRepository;
import com.fitmap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;
    private final CreditRepository creditRepository;

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        long remainingCredits = creditRepository.countByUser_IdAndUsedAtIsNull(userId);

        return ResponseEntity.ok(Map.of(
            "id", user.getId(),
            "email", user.getEmail() != null ? user.getEmail() : "",
            "name", user.getName() != null ? user.getName() : "",
            "profileImageUrl", user.getProfileImageUrl() != null ? user.getProfileImageUrl() : "",
            "provider", user.getProvider(),
            "role", user.getRole(),
            "remainingCredits", remainingCredits,
            "createdAt", user.getCreatedAt()
        ));
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> withdraw(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setDeletedAt(java.time.LocalDateTime.now());
        user.setEmail("withdrawn_" + userId + "@deleted");
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "withdrawn"));
    }
}
