package com.example.CalCol.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WelcomeController {

	@GetMapping("/")
	public String index() {
		return "redirect:/welcome";
	}

	@GetMapping("/welcome")
	public String welcome() {
		return "welcome";
	}
}

