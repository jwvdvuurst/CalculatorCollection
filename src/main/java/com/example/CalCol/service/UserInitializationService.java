package com.example.CalCol.service;

import com.example.CalCol.entity.AppUser;
import com.example.CalCol.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Order(0)
public class UserInitializationService implements CommandLineRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	@Value("${app.bootstrap.admin.username:admin}")
	private String adminUsername;
	@Value("${app.bootstrap.admin.email:}")
	private String adminEmail;
	@Value("${app.bootstrap.admin.password:}")
	private String adminPassword;

	@Override
	@Transactional
	public void run(String... args) {
		if (userRepository.count() > 0) {
			log.info("Users already exist in database. Skipping user initialization.");
			return;
		}

		if (adminPassword.isBlank()) {
			log.info("No bootstrap administrator configured. No default accounts will be created.");
			return;
		}
		if (adminUsername.isBlank() || !adminEmail.contains("@") || adminPassword.length() < 12
				|| adminPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
			throw new IllegalStateException("Bootstrap administrator requires a username, email and password of 12 or more characters (at most 72 UTF-8 bytes)");
		}
		AppUser admin = new AppUser();
		admin.setUsername(adminUsername);
		admin.setEmail(adminEmail);
		admin.setPassword(passwordEncoder.encode(adminPassword));
		admin.setRole("ADMIN");
		admin.setEnabled(true);
		userRepository.save(admin);
		log.info("Created configured bootstrap administrator");
	}
}

