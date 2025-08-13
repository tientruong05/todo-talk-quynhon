package com.TodoTalk.controller;

import com.TodoTalk.dto.request.LoginRequest;
import com.TodoTalk.dto.request.RegisterRequest;
import com.TodoTalk.dto.response.LoginResponse;
import com.TodoTalk.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute LoginRequest loginRequest,
                       BindingResult bindingResult,
                       Model model,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Please fill in all required fields");
            model.addAttribute("registerRequest", new RegisterRequest());
            return "auth";
        }

        try {
            LoginResponse response = userService.login(loginRequest);

            // Store user info in session
            session.setAttribute("user", response.getUser());
            session.setAttribute("token", response.getToken());

            redirectAttributes.addFlashAttribute("success", "Login successful!");
            return "redirect:/message";

        } catch (RuntimeException e) {
            model.addAttribute("error", "Invalid email or password");
            model.addAttribute("loginRequest", loginRequest);
            model.addAttribute("registerRequest", new RegisterRequest());
            return "auth";
        }
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterRequest registerRequest,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("registerRequest", registerRequest);
            model.addAttribute("loginRequest", new LoginRequest());
            return "auth";
        }

        try {
            userService.register(registerRequest);
            redirectAttributes.addFlashAttribute("success",
                "Registration successful! Please login with your credentials.");
            return "redirect:/login";

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("registerRequest", registerRequest);
            model.addAttribute("loginRequest", new LoginRequest());
            return "auth";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("success", "You have been logged out successfully");
        return "redirect:/login";
    }
}
