package com.synccarreira.synccarreira_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.mail.host=localhost",
		"spring.mail.port=25",
		"email.from.address=contato@synccarreira.com.br",
		"email.from.name=SyncCarreira"
})
class SynccarreiraApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
