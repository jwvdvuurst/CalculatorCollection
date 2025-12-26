package com.example.CalCol.controller;

import com.example.CalCol.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping
	public String viewProfile(Model model, Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return "redirect:/login";
		}

		String username = authentication.getName();
		userService.getUserByUsername(username).ifPresent(user -> {
			model.addAttribute("user", user);
		});

		return "user/profile";
	}

	@PostMapping("/update")
	public String updateProfile(
			@RequestParam String email,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return "redirect:/login";
		}

		String username = authentication.getName();
		if (userService.updateProfile(username, email)) {
			redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", 
				"Failed to update profile. Email may already be in use.");
		}

		return "redirect:/profile";
	}

	@PostMapping("/change-password")
	public String changePassword(
			@RequestParam String oldPassword,
			@RequestParam String newPassword,
			@RequestParam String confirmPassword,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {

		if (authentication == null || !authentication.isAuthenticated()) {
			return "redirect:/login";
		}

		if (!newPassword.equals(confirmPassword)) {
			redirectAttributes.addFlashAttribute("errorMessage", "New passwords do not match.");
			return "redirect:/profile";
		}

		if (newPassword.length() < 4) {
			redirectAttributes.addFlashAttribute("errorMessage", "Password must be at least 4 characters long.");
			return "redirect:/profile";
		}

		String username = authentication.getName();
		if (userService.changePassword(username, oldPassword, newPassword)) {
			redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully!");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", 
				"Failed to change password. Please check your current password.");
		}

		return "redirect:/profile";
	}
}

