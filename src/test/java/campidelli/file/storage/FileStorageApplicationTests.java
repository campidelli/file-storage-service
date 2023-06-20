package campidelli.file.storage;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.springframework.test.util.AssertionErrors.assertNotNull;

@ActiveProfiles("test")
@AutoConfigureWebTestClient(timeout = "36000")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
class FileStorageApplicationTests {

	private final int serverPort;
	private final TestRestTemplate restTemplate;

	@Autowired
	public FileStorageApplicationTests(@Value("${local.server.port}") int serverPort,
									   TestRestTemplate restTemplate) {
		this.serverPort = serverPort;
		this.restTemplate = restTemplate;
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
