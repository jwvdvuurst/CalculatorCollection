package com.example.CalCol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

	@Value("${app.upload.dir:uploads}")
	private String uploadDir;

	public String storeFile(MultipartFile file) throws IOException {
		if (file.isEmpty()) {
			throw new IllegalArgumentException("File is empty");
		}

		// Validate file type
		String originalFilename = file.getOriginalFilename();
		if (originalFilename == null) {
			throw new IllegalArgumentException("Filename is null");
		}

		String extension = "";
		int lastDotIndex = originalFilename.lastIndexOf('.');
		if (lastDotIndex > 0) {
			extension = originalFilename.substring(lastDotIndex);
		}

		// Generate unique filename
		String uniqueFilename = UUID.randomUUID().toString() + extension;

		// Create upload directory if it doesn't exist
		Path uploadPath = Paths.get(uploadDir);
		if (!Files.exists(uploadPath)) {
			Files.createDirectories(uploadPath);
		}

		// Save file
		Path filePath = uploadPath.resolve(uniqueFilename);
		Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

		log.info("File saved: {}", filePath);
		return uniqueFilename;
	}

	public Path loadFile(String filename) {
		return Paths.get(uploadDir).resolve(filename);
	}

	public boolean deleteFile(String filename) {
		try {
			Path filePath = loadFile(filename);
			return Files.deleteIfExists(filePath);
		} catch (IOException e) {
			log.error("Error deleting file: {}", filename, e);
			return false;
		}
	}
}

