package com.lingring;

import com.lingring.global.config.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class LingRingApplicationTests {

	@Test
	void contextLoads() {
	}

}