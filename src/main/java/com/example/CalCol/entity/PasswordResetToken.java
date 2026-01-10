package com.example.CalCol.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String token;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "user_id", nullable = false)
	private AppUser user;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "used", nullable = false)
	private Boolean used = false;

	@PrePersist
	protected void onCreate() {
		if (expiresAt == null) {
			// Default: 24 hours from now
			expiresAt = LocalDateTime.now().plusHours(24);
		}
	}

	public boolean isExpired() {
		return LocalDateTime.now().isAfter(expiresAt);
	}

	public boolean isValid() {
		return !used && !isExpired();
	}
}

