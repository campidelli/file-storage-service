package campidelli.file.storage;

import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.util.AssertionErrors.assertNotNull;

@ActiveProfiles("test")
@AutoConfigureWebTestClient(timeout = "36000")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Slf4j
class FileStorageApplicationTests {

	@LocalServerPort
	private int serverPort;

	@Autowired
	private TestRestTemplate restTemplate;

	@Container
	private static final S3MockContainer s3Mock = new S3MockContainer("2.12.2")
			.withValidKmsKeys("arn:aws:kms:us-east-1:1234567890:key/valid-test-key-ref")
			.withRetainFilesOnExit(true)
			.withEnv("debug", "true");

	@DynamicPropertySource
	static void registerDynamicProperties(DynamicPropertyRegistry registry) {
		registry.add("aws.s3.endpoint", s3Mock::getHttpEndpoint);
	}

	@Test
	void whenUploadSingleFile_thenSuccess1() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("file", new ClassPathResource("the-return-of-sherlock-holmes.pdf"));

		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

		String url = "http://localhost:" + serverPort + "/v1/async/file/";

		Object result = restTemplate.postForObject(url, request, Object.class);
		assertNotNull("Result must not be null.", result);
	}
}
