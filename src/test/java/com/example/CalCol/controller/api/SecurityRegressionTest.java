package com.example.CalCol.controller.api;

import com.example.CalCol.entity.*;
import com.example.CalCol.repository.*;
import com.example.CalCol.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityRegressionTest {
	@TempDir static Path uploads;
	@DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
		registry.add("app.upload.dir", () -> uploads.toString());
		registry.add("app.upload.max-bytes", () -> 4096);
		registry.add("app.upload.max-pixels", () -> 10000);
	}
	@Autowired MockMvc mvc;
	@Autowired ObjectMapper json;
	@Autowired UserService users;
	@Autowired UserDetailsService details;
	@Autowired JwtService jwt;
	@Autowired ManufacturerRepository manufacturers;
	@Autowired CalculatorRepository calculators;
	@Autowired CalculatorImageRepository images;
	@Autowired CalculatorLinkRepository links;
	@Autowired UserCalculatorCollectionRepository collections;
	@Autowired CalculatorService calculatorService;
	Calculator calculator;
	Manufacturer manufacturer;
	String ownerToken, otherToken, adminToken;
	byte[] png;

	@BeforeEach void fixtures() throws Exception {
		users.createUser("owner", "owner@example.invalid", "OwnerPassword123!", "USER");
		users.createUser("other", "other@example.invalid", "OtherPassword123!", "USER");
		ownerToken = token("owner"); otherToken = token("other"); adminToken = token("admin");
		manufacturer = new Manufacturer(); manufacturer.setName("Regression fixture");
		manufacturer = manufacturers.saveAndFlush(manufacturer);
		calculator = new Calculator(); calculator.setModel("Fixture"); calculator.setManufacturer(manufacturer);
		calculator = calculators.saveAndFlush(calculator);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", output);
		png = output.toByteArray();
	}
	String token(String name) { return "Bearer " + jwt.generateToken(details.loadUserByUsername(name)); }
	String imageUrl() { return "/api/calculators/" + calculator.getId() + "/images"; }
	long upload(String token, boolean propose) throws Exception {
		String response = mvc.perform(multipart(imageUrl()).file(new MockMultipartFile("file", "image.png", "image/png", png))
			.header("Authorization", token).param("propose", Boolean.toString(propose)))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return json.readTree(response).path("data").path("id").asLong();
	}
	@Test void defaultAdministratorPasswordDoesNotWork() throws Exception {
		mvc.perform(get("/api/user/profile").with(httpBasic("admin", "admin"))).andExpect(status().isUnauthorized());
	}
	@Test void unconfiguredEmailReturnsServiceUnavailable() throws Exception {
		mvc.perform(post("/api/collection/send-summary-email").header("Authorization",ownerToken))
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.error").value("Email service is not configured"));
	}
	@Test void nonAdminCannotAutoApproveMultipartOrBinary() throws Exception {
		long id = upload(ownerToken, false);
		assertThat(images.findById(id).orElseThrow().getIsApproved()).isFalse();
		mvc.perform(post(imageUrl()+"/binary").header("Authorization",ownerToken)
			.contentType("image/png").content(png).param("propose","false"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.isApproved").value(false));
		mvc.perform(get(imageUrl()).header("Authorization",otherToken)).andExpect(jsonPath("$.data").isEmpty());
	}
	@Test void mvcUploadCannotAutoApprove() throws Exception {
		mvc.perform(multipart("/calculators/"+calculator.getId()+"/images")
			.file(new MockMultipartFile("file","image.png","image/png",png))
			.with(user("owner")).with(csrf()).param("proposeForRepository","false"))
			.andExpect(status().is3xxRedirection());
		assertThat(images.findApprovedOrUserImages(calculator.getId(),"owner"))
			.hasSize(1).allMatch(image -> !image.getIsApproved());
	}
	@Test void ownerCanReadPrivateImageButOtherUsersAndAnonymousCannot() throws Exception {
		long id=upload(ownerToken,false);
		String path="/uploads/"+images.findById(id).orElseThrow().getImagePath();
		mvc.perform(get(path)).andExpect(status().isNotFound());
		mvc.perform(get(path).header("Authorization",otherToken)).andExpect(status().isNotFound());
		mvc.perform(get(path).header("Authorization",ownerToken)).andExpect(status().isOk())
			.andExpect(content().contentType("image/png")).andExpect(header().string("Cache-Control","private, no-store"));
		mvc.perform(get(path).header("Authorization",adminToken)).andExpect(status().isOk());
	}
	@Test void administratorCanPublishAnImage() throws Exception {
		long id=upload(adminToken,false);
		var image=images.findById(id).orElseThrow();
		assertThat(image.getIsApproved()).isTrue();
		mvc.perform(get("/uploads/"+image.getImagePath())).andExpect(status().isOk());
	}
	@Test void anotherUserCannotDeletePendingImageButOwnerCan() throws Exception {
		long id=upload(ownerToken,true);
		mvc.perform(delete(imageUrl()+"/"+id).header("Authorization",otherToken)).andExpect(status().isNotFound());
		assertThat(images.existsById(id)).isTrue();
		mvc.perform(delete(imageUrl()+"/"+id).header("Authorization",ownerToken)).andExpect(status().isOk());
		assertThat(images.existsById(id)).isFalse();
	}
	@Test void nonOwnerCannotEditLinkButOwnerAndAdministratorCan() throws Exception {
		CalculatorLink link=new CalculatorLink();link.setCalculator(calculator);link.setUrl("https://example.invalid/original");
		link.setTitle("Original");link.setAddedBy("owner");link=links.saveAndFlush(link);
		String path="/api/calculators/"+calculator.getId()+"/links/"+link.getId();
		mvc.perform(put(path).header("Authorization",otherToken).param("url","https://example.invalid/changed").param("title","Changed"))
			.andExpect(status().isForbidden());
		assertThat(links.findById(link.getId()).orElseThrow().getTitle()).isEqualTo("Original");
		mvc.perform(put(path).header("Authorization",ownerToken).param("url","https://example.invalid/owner").param("title","Owner"))
			.andExpect(status().isOk());
		mvc.perform(put(path).header("Authorization",adminToken).param("url","https://example.invalid/admin").param("title","Admin"))
			.andExpect(status().isOk());
		mvc.perform(put("/api/calculators/999999/links/"+link.getId()).header("Authorization",ownerToken)
			.param("url","https://example.invalid/mismatch").param("title","Wrong calculator"))
			.andExpect(status().isNotFound());
	}
	@Test void rejectsHtmlEvenWhenNamedAsAnImage() throws Exception {
		for(String name:new String[]{"proof.html","proof.png"}) {
			mvc.perform(multipart(imageUrl()).file(new MockMultipartFile("file",name,"image/png","<script>alert(1)</script>".getBytes()))
				.header("Authorization",ownerToken)).andExpect(status().isBadRequest());
		}
		assertThat(images.findApprovedOrUserImages(calculator.getId(),"owner")).isEmpty();
	}
	@Test void rejectsOversizeBinaryAndMultipart() throws Exception {
		mvc.perform(post(imageUrl()+"/binary").contentType("image/png").content(new byte[4097]).header("Authorization",ownerToken))
			.andExpect(status().isBadRequest());
		mvc.perform(multipart(imageUrl()).file(new MockMultipartFile("file","large.png","image/png",new byte[4097]))
			.header("Authorization",ownerToken)).andExpect(status().isBadRequest());
	}
	@Test void detectsFormatInsteadOfTrustingFilename() throws Exception {
		mvc.perform(multipart(imageUrl()).file(new MockMultipartFile("file","misleading.html","text/html",png))
			.header("Authorization",ownerToken)).andExpect(status().isOk())
			.andExpect(jsonPath("$.data.imagePath",org.hamcrest.Matchers.endsWith(".png")));
	}
	@Test void oldHtmlUploadsAreNeverServed() throws Exception {
		CalculatorImage image=new CalculatorImage();image.setCalculator(calculator);image.setUploadedBy("owner");
		image.setImagePath("old.html");image.setIsApproved(true);images.saveAndFlush(image);
		java.nio.file.Files.writeString(uploads.resolve("old.html"),"<script>alert(1)</script>");
		mvc.perform(get("/uploads/old.html")).andExpect(status().isNotFound());
	}
	MockHttpSession browserSession() {
		var principal=details.loadUserByUsername("owner");
		var authentication=new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities());
		MockHttpSession session=new MockHttpSession();
		session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,new SecurityContextImpl(authentication));
		return session;
	}
	@Test void browserSessionCannotMutateApiWithoutCsrf() throws Exception {
		mvc.perform(post("/api/collection/"+calculator.getId()).session(browserSession())).andExpect(status().isForbidden());
		assertThat(collections.existsByUsernameAndCalculatorId("owner",calculator.getId())).isFalse();
	}
	@Test void invalidBearerCannotReuseBrowserSession() throws Exception {
		mvc.perform(post("/api/collection/"+calculator.getId()).session(browserSession()).header("Authorization","Bearer invalid"))
			.andExpect(status().isUnauthorized());
		assertThat(collections.existsByUsernameAndCalculatorId("owner",calculator.getId())).isFalse();
	}
	@Test void basicMutationsRequireCsrfButJwtMutationsWork() throws Exception {
		mvc.perform(post("/api/collection/"+calculator.getId()).with(httpBasic("owner","OwnerPassword123!")))
			.andExpect(status().isForbidden());
		mvc.perform(post("/api/collection/"+calculator.getId()).header("Authorization",ownerToken)).andExpect(status().isOk());
		assertThat(collections.existsByUsernameAndCalculatorId("owner",calculator.getId())).isTrue();
	}
	@Test void selfMergeLeavesManufacturerAndCalculatorIntact() throws Exception {
		mvc.perform(post("/api/manufacturers/merge").header("Authorization",ownerToken)
			.param("targetManufacturerId",manufacturer.getId().toString()).param("sourceManufacturerId",manufacturer.getId().toString()))
			.andExpect(status().isBadRequest());
		assertThat(manufacturers.existsById(manufacturer.getId())).isTrue();
		assertThat(calculators.existsById(calculator.getId())).isTrue();
	}
	@Test void distinctMergePreservesCalculatorsEvenWithLoadedRelationships() {
		Manufacturer target=new Manufacturer();target.setName("Target");target=manufacturers.saveAndFlush(target);
		// Initialize a stale collection before the bulk update.
		manufacturer.getCalculators().add(calculator);
		assertThat(calculatorService.mergeManufacturers(target.getId(),manufacturer.getId(),"Renamed target")).isTrue();
		assertThat(manufacturers.existsById(manufacturer.getId())).isFalse();
		assertThat(calculators.findById(calculator.getId()).orElseThrow().getManufacturer().getId()).isEqualTo(target.getId());
		assertThat(manufacturers.findById(target.getId()).orElseThrow().getName()).isEqualTo("Renamed target");
	}
}
