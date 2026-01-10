package com.example.CalCol.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collection;

@Controller
public class WelcomeController {

	@GetMapping("/")
	public String index() {
		return "redirect:/welcome";
	}

	@GetMapping("/welcome")
	public String welcome(Model model, Authentication authentication) {
		boolean isAuthenticated = authentication != null && authentication.isAuthenticated();
		boolean isAdmin = false;
		
		if (authentication != null && isAuthenticated) {
			Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
			if (authorities != null) {
				isAdmin = authorities.stream()
					.map(GrantedAuthority::getAuthority)
					.anyMatch(auth -> auth.equals("ROLE_ADMIN"));
			}
		}
		
		model.addAttribute("isAuthenticated", isAuthenticated);
		model.addAttribute("isAdmin", isAdmin);
		
		return "welcome";
	}
}

