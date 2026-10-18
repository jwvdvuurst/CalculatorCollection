package com.example.CalCol;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mail.javamail.JavaMailSender;
import com.example.CalCol.service.EmailService;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CalculatorCollectorApplicationTests {
	@Autowired ApplicationContext context;
	@Autowired EmailService emailService;

	@Test
	void contextLoads() {
		assertThat(context.getBeansOfType(JavaMailSender.class)).isEmpty();
		assertThat(emailService.isAvailable()).isFalse();
	}

}
