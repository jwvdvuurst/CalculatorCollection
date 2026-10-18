package com.example.CalCol.controller;

import com.example.CalCol.repository.CalculatorImageRepository;
import com.example.CalCol.service.CurrentUserService;
import com.example.CalCol.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.util.Locale;

/** Delivers approved images publicly and private images only to their owner or an administrator. */
@RestController
@RequiredArgsConstructor
public class UploadController {
	private final CalculatorImageRepository images;
	private final FileStorageService storage;
	private final CurrentUserService currentUser;

	@GetMapping("/uploads/{filename:.+}")
	public ResponseEntity<Resource> image(@PathVariable String filename, Authentication authentication) {
		var image = images.findByImagePath(filename);
		if (image.isEmpty()) return ResponseEntity.notFound().build();
		boolean ownerOrAdmin = authentication != null && authentication.isAuthenticated()
			&& (image.get().getUploadedBy().equals(authentication.getName()) || currentUser.isAdmin(authentication.getName()));
		if (!Boolean.TRUE.equals(image.get().getIsApproved()) && !ownerOrAdmin) {
			return ResponseEntity.notFound().build();
		}
		String type = switch (filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)) {
			case "jpg", "jpeg" -> "image/jpeg";
			case "png" -> "image/png";
			case "gif" -> "image/gif";
			case "webp" -> "image/webp";
			default -> null;
		};
		if (type == null) return ResponseEntity.notFound().build();
		try {
			var path = storage.loadFile(filename);
			if (!Files.isRegularFile(path)) return ResponseEntity.notFound().build();
			return ResponseEntity.ok().contentType(MediaType.parseMediaType(type))
				.header("Cache-Control", "private, no-store")
				.header("X-Content-Type-Options", "nosniff")
				.body(new FileSystemResource(path));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.notFound().build();
		}
	}
}
