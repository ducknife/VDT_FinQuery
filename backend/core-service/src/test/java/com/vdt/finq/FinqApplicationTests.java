package com.vdt.finq;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = { "cleaner.trial.enabled=false", "answer.trial.enabled=false" })
class FinqApplicationTests {

	@Test
	void contextLoads() {
	}

}
