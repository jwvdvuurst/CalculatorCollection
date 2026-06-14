package com.example.CalCol.controller.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthRestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void registerLoginAndAccessProfileWithJwt() throws Exception {
		String registerBody = """
			{
			  "username": "mobileuser",
			  "email": "mobile@example.com",
			  "password": "secret",
			  "confirmPassword": "secret"
			}
			""";

		MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(registerBody))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andReturn();

		JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
		String token = registerJson.path("data").path("accessToken").asText();
		assertThat(token).isNotBlank();

		mockMvc.perform(get("/api/user/profile")
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.username").value("mobileuser"));

		String loginBody = """
			{
			  "username": "mobileuser",
			  "password": "secret"
			}
			""";

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.accessToken").isNotEmpty());
	}

	@Test
	void profileStillWorksWithHttpBasicAuth() throws Exception {
		mockMvc.perform(get("/api/user/profile")
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("admin", "admin")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.username").value("admin"));
	}
}
