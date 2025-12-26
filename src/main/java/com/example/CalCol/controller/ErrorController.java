package com.example.CalCol.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

	@RequestMapping("/error")
	public String handleError(HttpServletRequest request, Model model) {
		Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
		Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

		if (status != null) {
			Integer statusCode = Integer.valueOf(status.toString());
			model.addAttribute("statusCode", statusCode);
			model.addAttribute("statusText", HttpStatus.valueOf(statusCode).getReasonPhrase());

			if (statusCode == HttpStatus.NOT_FOUND.value()) {
				model.addAttribute("errorMessage", "The page you are looking for does not exist.");
			} else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
				model.addAttribute("errorMessage", "An internal server error occurred.");
			} else if (statusCode == HttpStatus.FORBIDDEN.value()) {
				model.addAttribute("errorMessage", "You do not have permission to access this resource.");
			} else {
				model.addAttribute("errorMessage", message != null ? message.toString() : "An error occurred.");
			}
		} else {
			model.addAttribute("statusCode", 500);
			model.addAttribute("statusText", "Error");
			model.addAttribute("errorMessage", "An unexpected error occurred.");
		}

		if (exception != null) {
			model.addAttribute("exception", exception.toString());
		}

		return "error";
	}
}

