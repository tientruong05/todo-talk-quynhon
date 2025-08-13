package com.TodoTalk.controller;

import com.TodoTalk.dto.request.LoginRequest;
import com.TodoTalk.dto.request.RegisterRequest;
import com.TodoTalk.dto.response.UserResponse;
import com.TodoTalk.service.UserService;
import jakarta.servlet.http.HttpSession;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;


@Controller
public class WebController {

    private final UserService userService;

    public WebController(UserService userService) {
        this.userService = userService;
    }

    @PostConstruct
    public void init() {
        // Ensure avatars directory exists on startup
        try {
            String projectDir = System.getProperty("user.dir");
            String uploadDir = projectDir + "/src/main/resources/static/images/avatars/";
            File directory = new File(uploadDir);

            if (!directory.exists()) {
                directory.mkdirs();
            }
        } catch (Exception e) {
            // Directory creation failed, but continue
        }
    }

    @GetMapping("/")
    public String home(HttpSession session) {
        // Check if user is logged in
        UserResponse user = (UserResponse) session.getAttribute("user");
        if (user != null) {
            return "redirect:/message";
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(Model model, HttpSession session) {
        // Redirect to message if already logged in
        UserResponse user = (UserResponse) session.getAttribute("user");
        if (user != null) {
            return "redirect:/message";
        }

        model.addAttribute("title", "Sign In - TodoTalk");
        if (!model.containsAttribute("loginRequest")) {
            model.addAttribute("loginRequest", new LoginRequest());
        }
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "auth";
    }

    @GetMapping("/register")
    public String register(Model model, HttpSession session) {
        // Redirect to message if already logged in
        UserResponse user = (UserResponse) session.getAttribute("user");
        if (user != null) {
            return "redirect:/message";
        }

        // Redirect to auth page with register tab
        return "redirect:/login?tab=register";
    }

    @GetMapping("/message")
    public String message(Model model, HttpSession session) {
        // Check if user is logged in
        UserResponse user = (UserResponse) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("title", "Messages - TodoTalk");
        model.addAttribute("user", user);
        return "message";
    }

    // Keep dashboard for backward compatibility
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session) {
        return "redirect:/message";
    }

    @GetMapping("/profile")
    public String profile(HttpSession session) {
        // Redirect to message page since profile is now a modal
        return "redirect:/message";
    }



    @PostMapping("/profile")
    @ResponseBody
    public ResponseEntity<String> updateProfile(
            @RequestParam("email") String email,
            @RequestParam("fullName") String fullName,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
            HttpSession session) {

        try {
            // Check if user is logged in
            UserResponse user = (UserResponse) session.getAttribute("user");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
            }

            // Validate input
            if (fullName == null || fullName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Họ và tên không được để trống");
            }

            // Handle avatar upload
            String avatarUrl = user.getAvatarUrl();
            if (avatarFile != null && !avatarFile.isEmpty()) {
                // Validate file type
                String contentType = avatarFile.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return ResponseEntity.badRequest().body("Chỉ chấp nhận file ảnh (jpg, png, gif)");
                }

                // Validate file size (max 5MB)
                if (avatarFile.getSize() > 5 * 1024 * 1024) {
                    return ResponseEntity.badRequest().body("Kích thước file không được vượt quá 5MB");
                }

                // Validate file name
                String originalFilename = avatarFile.getOriginalFilename();
                if (originalFilename == null || originalFilename.trim().isEmpty()) {
                    return ResponseEntity.badRequest().body("Tên file không hợp lệ");
                }

                avatarUrl = saveAvatarFile(avatarFile, user.getUsername());
            }

            // Update user in database using UserService
            UserResponse updatedUser = userService.updateUser(
                user.getUserId(),
                email,
                fullName.trim(),
                avatarUrl
            );

            // Update session with the updated user from database
            session.setAttribute("user", updatedUser);

            return ResponseEntity.ok("Success");

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lưu file ảnh: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Có lỗi xảy ra: " + e.getMessage());
        }
    }



    private String saveAvatarFile(MultipartFile file, String username) throws IOException {
        try {
            // Get the correct path to the project's static/images/avatars directory
            String projectDir = System.getProperty("user.dir");
            String uploadDir = projectDir + "/src/main/resources/static/images/avatars/";
            File directory = new File(uploadDir);

            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                if (!created) {
                    throw new IOException("Không thể tạo thư mục lưu ảnh");
                }
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = ".jpg"; // Default extension
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
                // Validate extension
                if (!extension.matches("\\.(jpg|jpeg|png|gif)")) {
                    throw new IOException("Định dạng file không được hỗ trợ: " + extension);
                }
            }

            // Sanitize username for filename
            String sanitizedUsername = username.replaceAll("[^a-zA-Z0-9]", "_");
            String filename = sanitizedUsername + "_" + System.currentTimeMillis() + extension;

            // Save file
            String filePath = uploadDir + filename;
            File targetFile = new File(filePath);

            // Check if parent directory is writable
            if (!targetFile.getParentFile().canWrite()) {
                throw new IOException("Không có quyền ghi vào thư mục lưu ảnh");
            }

            file.transferTo(targetFile);

            // Verify file was saved successfully
            if (!targetFile.exists() || targetFile.length() == 0) {
                throw new IOException("File không được lưu thành công");
            }

            // Return web-accessible path
            return "/images/avatars/" + filename;

        } catch (IOException e) {
            throw new IOException("Lỗi khi lưu file ảnh: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IOException("Lỗi không xác định khi lưu file: " + e.getMessage(), e);
        }
    }
}
