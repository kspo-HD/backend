package com.fitmap.service;

import com.fitmap.domain.user.User;
import com.fitmap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    public Optional<User> findOrNull(String provider, String providerId) {
        return userRepository.findByProviderAndProviderId(provider, providerId);
    }

    public User updateLastLogin(User user) {
        return userRepository.save(user);
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
