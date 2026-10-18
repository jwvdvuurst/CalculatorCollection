package com.example.CalCol.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Authorization uses the authenticated principal, never a client-supplied role. */
@Service
public class CurrentUserService {
	public boolean isAdmin(String username) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && authentication.isAuthenticated()
			&& authentication.getName().equals(username)
			&& authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
	}
}
