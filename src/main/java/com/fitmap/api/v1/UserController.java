package com.fitmap.api.v1;

import com.fitmap.common.ApiResponse;
import com.fitmap.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(userService.getMe(userId)));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, String>>> withdraw(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        userService.withdraw(userId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "withdrawn")));
    }
}
