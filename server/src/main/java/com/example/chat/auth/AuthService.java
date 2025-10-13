package com.example.chat.auth;

import com.example.chat.model.User;
import com.example.chat.repo.UserRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    public String authenticate(String username, String password) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // For POC, we are just checking the password directly.
            // In a real application, you MUST use a password encoder (e.g., BCrypt).
            if (password.equals(user.getPassword())) {
                return jwtUtil.generateToken(username);
            }
        }
        return null;
    }
}