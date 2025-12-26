package com.example.CalCol.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "shared_collections")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedCollection {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "share_token", nullable = false, unique = true, length = 100)
	private String shareToken;

	@Column(name = "shared_by", nullable = false, length = 100)
	private String sharedBy;

	@Column(name = "shared_at", nullable = false)
	private LocalDateTime sharedAt;

	@Column(name = "expires_at")
	private LocalDateTime expiresAt;

	@Column(name = "is_public", nullable = false)
	private Boolean isPublic = false;

	@Column(length = 500)
	private String title;

	@Column(length = 2000)
	private String description;

	@PrePersist
	protected void onCreate() {
		if (sharedAt == null) {
			sharedAt = LocalDateTime.now();
		}
		if (shareToken == null) {
			shareToken = java.util.UUID.randomUUID().toString();
		}
	}
}

