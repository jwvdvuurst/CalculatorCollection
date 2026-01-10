package com.example.CalCol.controller;

import com.example.CalCol.entity.PasswordResetToken;
import com.example.CalCol.service.EmailService;
import com.example.CalCol.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

	private final UserService userService;
	
	@Autowired(required = false)
	private EmailService emailService;

	@Value("${app.base-url:http://localhost:8080}")
	private String baseUrl;

	@GetMapping("/register")
	public String showRegistrationForm(Model model) {
		return "auth/register";
	}

	@PostMapping("/register")
	public String register(
			@RequestParam String username,
			@RequestParam String email,
			@RequestParam String password,
			@RequestParam String confirmPassword,
			RedirectAttributes redirectAttributes) {

		// Validation
		if (username == null || username.trim().isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage", "Username is required.");
			return "redirect:/register";
		}

		if (email == null || email.trim().isEmpty() || !email.contains("@")) {
			redirectAttributes.addFlashAttribute("errorMessage", "Valid email is required.");
			return "redirect:/register";
		}

		if (password == null || password.length() < 4) {
			redirectAttributes.addFlashAttribute("errorMessage", "Password must be at least 4 characters long.");
			return "redirect:/register";
		}

		if (!password.equals(confirmPassword)) {
			redirectAttributes.addFlashAttribute("errorMessage", "Passwords do not match.");
			return "redirect:/register";
		}

		// Check if username or email already exists
		if (userService.usernameExists(username)) {
			redirectAttributes.addFlashAttribute("errorMessage", "Username already exists. Please choose another.");
			return "redirect:/register";
		}

		if (userService.emailExists(email)) {
			redirectAttributes.addFlashAttribute("errorMessage", "Email already registered. Please use a different email or try logging in.");
			return "redirect:/register";
		}

		try {
			userService.createUser(username, email, password, "USER");
			redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please log in.");
			return "redirect:/welcome";
		} catch (Exception e) {
			log.error("Registration error: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("errorMessage", "Registration failed: " + e.getMessage());
			return "redirect:/register";
		}
	}

	@GetMapping("/forgot-password")
	public String showForgotPasswordForm() {
		return "auth/forgot-password";
	}

	@PostMapping("/forgot-password")
	public String requestPasswordReset(
			@RequestParam String username,
			@RequestParam String email,
			RedirectAttributes redirectAttributes) {

		Optional<PasswordResetToken> tokenOpt = userService.createPasswordResetToken(username, email);

		// Always show success message (security: don't reveal if user exists)
		redirectAttributes.addFlashAttribute("successMessage",
			"If the username and email match, a password reset link has been sent to your email address.");

		if (tokenOpt.isPresent()) {
			PasswordResetToken token = tokenOpt.get();
			String resetUrl = baseUrl + "/reset-password?token=" + token.getToken();

			if (emailService != null) {
				try {
					Map<String, Object> variables = Map.of(
						"username", username,
						"resetUrl", resetUrl
					);
					emailService.sendHtmlEmail(email, "Password Reset Request", "email/password-reset", variables);
					log.info("Password reset email sent successfully to: {}", email);
				} catch (RuntimeException e) {
					log.error("Failed to send password reset email to {}: {}", email, e.getMessage(), e);
					// Don't reveal error to user, but log it
				} catch (Exception e) {
					log.error("Unexpected error sending password reset email to {}: {}", email, e.getMessage(), e);
				}
			} else {
				log.warn("Email service not configured. Password reset token generated but email not sent. Token: {}", token.getToken());
				log.warn("To enable email functionality, configure spring.mail.host in application.properties");
			}
		} else {
			log.debug("Password reset token not created - username/email mismatch or user not found");
		}

		return "redirect:/forgot-password";
	}

	@GetMapping("/reset-password")
	public String showResetPasswordForm(@RequestParam(required = false) String token, Model model) {
		if (token == null || token.trim().isEmpty()) {
			model.addAttribute("errorMessage", "Invalid or missing reset token.");
			return "auth/reset-password";
		}

		Optional<PasswordResetToken> tokenOpt = userService.getPasswordResetToken(token);
		if (tokenOpt.isEmpty() || !tokenOpt.get().isValid()) {
			model.addAttribute("errorMessage", "Invalid or expired reset token. Please request a new one.");
			return "auth/reset-password";
		}

		model.addAttribute("token", token);
		return "auth/reset-password";
	}

	@PostMapping("/reset-password")
	public String resetPassword(
			@RequestParam String token,
			@RequestParam String newPassword,
			@RequestParam String confirmPassword,
			RedirectAttributes redirectAttributes) {

		if (token == null || token.trim().isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage", "Invalid reset token.");
			return "redirect:/reset-password?token=" + token;
		}

		if (newPassword == null || newPassword.length() < 4) {
			redirectAttributes.addFlashAttribute("errorMessage", "Password must be at least 4 characters long.");
			return "redirect:/reset-password?token=" + token;
		}

		if (!newPassword.equals(confirmPassword)) {
			redirectAttributes.addFlashAttribute("errorMessage", "Passwords do not match.");
			return "redirect:/reset-password?token=" + token;
		}

		if (userService.resetPassword(token, newPassword)) {
			redirectAttributes.addFlashAttribute("successMessage", "Password reset successful! Please log in with your new password.");
			return "redirect:/welcome";
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "Invalid or expired reset token. Please request a new one.");
			return "redirect:/forgot-password";
		}
	}
}

