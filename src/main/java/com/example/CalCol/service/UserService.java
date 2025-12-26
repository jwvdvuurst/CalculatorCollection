package com.example.CalCol.service;

import com.example.CalCol.entity.AppUser;
import com.example.CalCol.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository, @Lazy PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public Page<AppUser> getAllUsers(Pageable pageable) {
		return userRepository.findAll(pageable);
	}

	public Optional<AppUser> getUserByUsername(String username) {
		return userRepository.findByUsername(username);
	}

	public Optional<AppUser> getUserById(Long id) {
		return userRepository.findById(id);
	}

	public boolean usernameExists(String username) {
		return userRepository.existsByUsername(username);
	}

	public boolean emailExists(String email) {
		return userRepository.existsByEmail(email);
	}

	@Transactional
	public AppUser createUser(String username, String email, String password, String role) {
		if (userRepository.existsByUsername(username)) {
			throw new IllegalArgumentException("Username already exists");
		}
		if (userRepository.existsByEmail(email)) {
			throw new IllegalArgumentException("Email already exists");
		}

		AppUser user = new AppUser();
		user.setUsername(username);
		user.setEmail(email);
		user.setPassword(passwordEncoder.encode(password));
		user.setRole(role != null ? role : "USER");
		user.setEnabled(true);

		return userRepository.save(user);
	}

	@Transactional
	public AppUser updateUser(Long id, String email, String role, Boolean enabled) {
		Optional<AppUser> userOpt = userRepository.findById(id);
		if (userOpt.isEmpty()) {
			throw new IllegalArgumentException("User not found");
		}

		AppUser user = userOpt.get();
		if (email != null && !email.equals(user.getEmail())) {
			if (userRepository.existsByEmail(email)) {
				throw new IllegalArgumentException("Email already exists");
			}
			user.setEmail(email);
		}
		if (role != null) {
			user.setRole(role);
		}
		if (enabled != null) {
			user.setEnabled(enabled);
		}

		return userRepository.save(user);
	}

	@Transactional
	public boolean changePassword(String username, String oldPassword, String newPassword) {
		Optional<AppUser> userOpt = userRepository.findByUsername(username);
		if (userOpt.isEmpty()) {
			return false;
		}

		AppUser user = userOpt.get();
		// Verify old password
		if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
			return false;
		}

		user.setPassword(passwordEncoder.encode(newPassword));
		userRepository.save(user);
		return true;
	}

	@Transactional
	public boolean updateProfile(String username, String email) {
		Optional<AppUser> userOpt = userRepository.findByUsername(username);
		if (userOpt.isEmpty()) {
			return false;
		}

		AppUser user = userOpt.get();
		if (email != null && !email.equals(user.getEmail())) {
			if (userRepository.existsByEmail(email)) {
				return false; // Email already exists
			}
			user.setEmail(email);
			userRepository.save(user);
		}
		return true;
	}

	@Transactional
	public boolean deleteUser(Long id) {
		Optional<AppUser> userOpt = userRepository.findById(id);
		if (userOpt.isEmpty()) {
			return false;
		}
		userRepository.delete(userOpt.get());
		return true;
	}

	@Transactional
	public void updateLastLogin(String username) {
		userRepository.findByUsername(username).ifPresent(user -> {
			user.setLastLogin(java.time.LocalDateTime.now());
			userRepository.save(user);
		});
	}
}

