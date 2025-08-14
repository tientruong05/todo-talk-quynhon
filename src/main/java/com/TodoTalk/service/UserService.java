package com.TodoTalk.service;

import com.TodoTalk.dto.request.LoginRequest;
import com.TodoTalk.dto.request.RegisterRequest;
import com.TodoTalk.dto.response.LoginResponse;
import com.TodoTalk.dto.response.UserResponse;
import com.TodoTalk.entity.User;
import com.TodoTalk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public UserResponse register(RegisterRequest request) {
        // Check if username exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists!");
        }
        
        // Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists!");
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .avatarUrl(request.getAvatarUrl())
                .build();

        User savedUser = userRepository.save(user);
        return convertToUserResponse(savedUser);
    }

    public LoginResponse login(LoginRequest request) {
        // Authenticate user by email only
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(), // This should be email
                        request.getPassword()
                )
        );

        // Get user details by email only
        String email = request.getUsernameOrEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // Generate JWT token using email
        String token = jwtService.generateToken(user.getEmail());

        UserResponse userResponse = convertToUserResponse(user);
        return new LoginResponse(token, userResponse);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(String searchTerm) {
        List<User> users = userRepository.searchUsers(searchTerm);
        return users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsersNotInChat(Long chatId) {
        List<User> users = userRepository.findUsersNotInChat(chatId);
        return users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserById(Long userId) {
        return userRepository.findById(userId)
                .map(this::convertToUserResponse);
    }

    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::convertToUserResponse);
    }

    @Transactional
    public UserResponse updateUser(Long userId, String email, String fullName, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Update user fields
        if (email != null && !email.trim().isEmpty()) {
            user.setEmail(email.trim());
        }
        if (fullName != null && !fullName.trim().isEmpty()) {
            user.setFullName(fullName.trim());
        }
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }

        // updatedAt will be automatically set by @PreUpdate
        User savedUser = userRepository.save(user);
        return convertToUserResponse(savedUser);
    }

    private UserResponse convertToUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
