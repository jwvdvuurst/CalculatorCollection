package com.example.CalCol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
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

	/**
	 * Download an image from a URL and save it to the upload directory
	 */
	public String downloadImageFromUrl(String imageUrl) throws IOException {
		try {
			URL url = validateImageUrl(imageUrl);

			// Determine file extension from URL or content type
			String extension = ".jpg"; // default
			String path = url.getPath().toLowerCase();
			if (path.endsWith(".png")) {
				extension = ".png";
			} else if (path.endsWith(".gif")) {
				extension = ".gif";
			} else if (path.endsWith(".webp")) {
				extension = ".webp";
			} else if (path.endsWith(".jpeg") || path.endsWith(".jpg")) {
				extension = ".jpg";
			}

			// Generate unique filename
			String uniqueFilename = UUID.randomUUID().toString() + extension;

			// Create upload directory if it doesn't exist
			Path uploadPath = Paths.get(uploadDir);
			if (!Files.exists(uploadPath)) {
				Files.createDirectories(uploadPath);
			}

			// Download and save file
			Path filePath = uploadPath.resolve(uniqueFilename);
			try (java.io.InputStream in = url.openStream()) {
				Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
			}

			log.info("Image downloaded and saved: {}", filePath);
			return uniqueFilename;

		} catch (Exception e) {
			log.error("Error downloading image from URL: {}", imageUrl, e);
			throw new IOException("Failed to download image from URL: " + imageUrl, e);
		}
	}

	/**
	 * Validate a user-provided image URL to mitigate SSRF.
	 * Allows only http/https schemes and disallows hosts that resolve to
	 * loopback, link-local, site-local (private) or multicast addresses.
	 */
	private URL validateImageUrl(String imageUrl) throws IOException {
		if (imageUrl == null || imageUrl.isBlank()) {
			throw new IllegalArgumentException("Image URL must not be empty");
		}

		try {
			URI uri = new URI(imageUrl.trim());
			String scheme = uri.getScheme();
			if (scheme == null) {
				throw new IllegalArgumentException("Image URL must include a scheme");
			}
			String lowerScheme = scheme.toLowerCase();
			if (!"http".equals(lowerScheme) && !"https".equals(lowerScheme)) {
				throw new IllegalArgumentException("Only HTTP and HTTPS URLs are allowed");
			}

			String host = uri.getHost();
			if (host == null || host.isBlank()) {
				throw new IllegalArgumentException("Image URL must include a host");
			}

			// Resolve host and ensure it does not point to internal or loopback addresses
			InetAddress[] addresses = InetAddress.getAllByName(host);
			for (InetAddress address : addresses) {
				if (address.isAnyLocalAddress()
						|| address.isLoopbackAddress()
						|| address.isLinkLocalAddress()
						|| address.isSiteLocalAddress()
						|| address.isMulticastAddress()) {
					throw new IllegalArgumentException("Image URL host is not allowed");
				}
			}

			return uri.toURL();
		} catch (IllegalArgumentException e) {
			// Re-throw to be handled uniformly by caller
			throw e;
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid image URL", e);
		}
	}
}

