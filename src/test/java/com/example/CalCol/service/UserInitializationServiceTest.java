package com.example.CalCol.service;

import com.example.CalCol.entity.AppUser;
import com.example.CalCol.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserInitializationServiceTest {
	UserRepository users;
	UserInitializationService initializer;
	BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
	@BeforeEach void setup() {
		users=mock(UserRepository.class);
		initializer=new UserInitializationService(users,encoder);
		ReflectionTestUtils.setField(initializer,"adminUsername","admin");
		ReflectionTestUtils.setField(initializer,"adminEmail","");
		ReflectionTestUtils.setField(initializer,"adminPassword","");
	}
	@Test void emptyDatabaseWithoutConfigurationCreatesNoAccounts() {
		initializer.run();
		verify(users,never()).save(any());
	}
	@Test void configuredAdministratorHasEncodedPasswordAndNoDemoUser() {
		ReflectionTestUtils.setField(initializer,"adminEmail","admin@example.invalid");
		ReflectionTestUtils.setField(initializer,"adminPassword","ExplicitBootstrap123!");
		initializer.run();
		ArgumentCaptor<AppUser> account=ArgumentCaptor.forClass(AppUser.class);
		verify(users).save(account.capture());
		assertThat(account.getValue().getRole()).isEqualTo("ADMIN");
		assertThat(encoder.matches("ExplicitBootstrap123!",account.getValue().getPassword())).isTrue();
	}
	@Test void rejectsWeakBootstrapPassword() {
		ReflectionTestUtils.setField(initializer,"adminEmail","admin@example.invalid");
		ReflectionTestUtils.setField(initializer,"adminPassword","admin");
		assertThatThrownBy(initializer::run).isInstanceOf(IllegalStateException.class);
		verify(users,never()).save(any());
	}
	@Test void existingAccountsAreNotModified() {
		when(users.count()).thenReturn(1L);
		initializer.run();
		verify(users,never()).save(any());
	}
}
