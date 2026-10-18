package com.example.CalCol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.util.Locale;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

	@Value("${app.upload.dir:uploads}")
	private String uploadDir;
	@Value("${app.upload.max-bytes:10485760}")
	private int maxBytes;
	@Value("${app.upload.max-pixels:20000000}")
	private long maxPixels;

	public String storeFile(MultipartFile file) throws IOException {
		if (file.isEmpty()) {
			throw new IllegalArgumentException("File is empty");
		}

		if (file.getSize() > maxBytes) {
			throw new IllegalArgumentException("Image exceeds the upload size limit");
		}
		try (InputStream input = file.getInputStream()) {
			return storeImage(input);
		}
	}

	private String storeImage(InputStream input) throws IOException {
		byte[] bytes = readImageBytes(input);
		String format;
		try (var imageInput = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
			var readers = ImageIO.getImageReaders(imageInput);
			if (!readers.hasNext()) {
				throw new IllegalArgumentException("Only valid JPEG, PNG, GIF and WebP images are accepted");
			}
			ImageReader reader = readers.next();
			try {
				format = reader.getFormatName().toLowerCase(Locale.ROOT);
				if (!Set.of("jpeg", "png", "gif", "webp").contains(format)) {
					throw new IllegalArgumentException("Unsupported image format");
				}
				reader.setInput(imageInput, true, true);
				int width = reader.getWidth(0);
				int height = reader.getHeight(0);
				if (width <= 0 || height <= 0 || (long) width * height > maxPixels) {
					throw new IllegalArgumentException("Image dimensions exceed the pixel limit");
				}
				if (reader.read(0) == null) {
					throw new IllegalArgumentException("Invalid image data");
				}
			} finally {
				reader.dispose();
			}
		} catch (IOException e) {
			throw new IllegalArgumentException("Invalid image data", e);
		}
		String filename = UUID.randomUUID() + "." + (format.equals("jpeg") ? "jpg" : format);
		Files.createDirectories(Paths.get(uploadDir));
		Files.write(loadFile(filename), bytes);
		return filename;
	}

	public byte[] readImageBytes(InputStream input) throws IOException {
		byte[] bytes = input.readNBytes(maxBytes + 1);
		if (bytes.length == 0 || bytes.length > maxBytes) {
			throw new IllegalArgumentException("Image is empty or exceeds the upload size limit");
		}
		return bytes;
	}

	public Path loadFile(String filename) {
		if (filename == null || !filename.matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9]+")) {
			throw new IllegalArgumentException("Invalid image filename");
		}
		return Paths.get(uploadDir).toAbsolutePath().normalize().resolve(filename);
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

			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setInstanceFollowRedirects(false);
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(10000);
			try {
				if (connection.getResponseCode() != 200) {
					throw new IOException("Image server did not return HTTP 200; redirects are not accepted");
				}
				try (InputStream input = connection.getInputStream()) {
					return storeImage(input);
				}
			} finally {
				connection.disconnect();
			}

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

