package campidelli.file.storage;

import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@AutoConfigureWebTestClient(timeout = "36000")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestMethodOrder(OrderAnnotation.class)
@Testcontainers
@Slf4j
public class FileStorageApplicationTests {

	private static final String FILE_NAME = "the-return-of-sherlock-holmes.pdf";

	@LocalServerPort
	private int port;

	private WebTestClient client;

	@Container
	private static final S3MockContainer s3Mock = new S3MockContainer("2.12.2")
			.withValidKmsKeys("arn:aws:kms:us-east-1:1234567890:key/valid-test-key-ref")
			.withRetainFilesOnExit(true)
			.withEnv("debug", "true");

	@DynamicPropertySource
	static void registerDynamicProperties(DynamicPropertyRegistry registry) {
		registry.add("aws.s3.endpoint", s3Mock::getHttpEndpoint);
	}

	@BeforeEach
	public void setup() {
		client = WebTestClient.bindToServer()
				.baseUrl("http://localhost:" + this.port)
				.build();
	}

	@Test
	@Order(1)
	public void testUpload() {
		MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("file", new ClassPathResource(FILE_NAME));

		try {
			client.post()
					.uri("/v1/async/file")
					.contentType(MediaType.MULTIPART_FORM_DATA)
					.body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
					.exchange()
					.expectStatus().is2xxSuccessful();

			client.get()
					.uri("/v1/async/file")
					.exchange()
					.expectStatus()
					.is2xxSuccessful()
					.expectHeader()
					.contentType(MediaType.APPLICATION_JSON)
					.expectBody()
					.jsonPath("$.length()").isEqualTo(1)
					.jsonPath("$[0]").isEqualTo(FILE_NAME);
		} finally {
			System.out.println(s3Mock.getLogs());
		}
	}
//
//	@Test
//	@Order(2)
//	public void testListFiles() {
//		client.get()
//				.uri("/v1/async/file/")
//				.exchange()
//				.expectStatus()
//				.is2xxSuccessful()
//				.expectHeader()
//				.contentType(MediaType.APPLICATION_JSON)
//				.expectBody()
//				.jsonPath("$.length()").isEqualTo(1)
//				.jsonPath("$[0]").isEqualTo(FILE_NAME);
//	}
}
