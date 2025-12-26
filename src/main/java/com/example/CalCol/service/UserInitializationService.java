package com.example.CalCol.service;

import com.example.CalCol.entity.AppUser;
import com.example.CalCol.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
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

	@Override
	@Transactional
	public void run(String... args) {
		if (userRepository.count() > 0) {
			log.info("Users already exist in database. Skipping user initialization.");
			return;
		}

		log.info("Initializing default users...");

		// Create admin user
		if (!userRepository.existsByUsername("admin")) {
			AppUser admin = new AppUser();
			admin.setUsername("admin");
			admin.setEmail("admin@calcol.example.com");
			admin.setPassword(passwordEncoder.encode("admin"));
			admin.setRole("ADMIN");
			admin.setEnabled(true);
			userRepository.save(admin);
			log.info("Created admin user");
		}

		// Create regular user
		if (!userRepository.existsByUsername("user")) {
			AppUser user = new AppUser();
			user.setUsername("user");
			user.setEmail("user@calcol.example.com");
			user.setPassword(passwordEncoder.encode("user"));
			user.setRole("USER");
			user.setEnabled(true);
			userRepository.save(user);
			log.info("Created user");
		}

		log.info("User initialization completed.");
	}
}

