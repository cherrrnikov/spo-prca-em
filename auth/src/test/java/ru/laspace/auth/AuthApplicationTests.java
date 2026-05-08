package ru.laspace.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = AuthApplication.class)
@Import(TestcontainersConfiguration.class)
class AuthApplicationTests {

	@Test
	void contextLoads() {
	}

}
