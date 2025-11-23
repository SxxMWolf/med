package com.SxxM.med;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class MedApplicationTests {

	@Test
	void contextLoads() {
		// 컨텍스트가 정상적으로 로드되는지 확인
	}

}
