package kr.co.pinup;

import kr.co.pinup.config.KakaoWebClientConfigTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertTrue;

@SpringBootTest
@DirtiesContext
@ActiveProfiles("test")
@Import(KakaoWebClientConfigTest.class)
public class PinupApplicationTests {

	@Value("${spring.datasource.url}")
	private String datasourceUrl;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoads() {
		assertNotNull(applicationContext);
	}

	@Test
	void checkDatasourceUrl() {
		assertNotNull(datasourceUrl);
		assertTrue(datasourceUrl.startsWith("jdbc:h2:mem:testdb"));
	}
}