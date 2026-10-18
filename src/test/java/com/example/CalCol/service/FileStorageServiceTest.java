package com.example.CalCol.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

	@Test
	void storeFile_rejectsNonImageContentRegardlessOfFilenameExtension() {
		FileStorageService service = new FileStorageService();
		MockMultipartFile file = new MockMultipartFile(
			"file", "payload.jpg", "image/jpeg", "<script>alert('xss')</script>".getBytes());

		assertThatThrownBy(() -> service.storeFile(file))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("File is not a supported image");
	}
}