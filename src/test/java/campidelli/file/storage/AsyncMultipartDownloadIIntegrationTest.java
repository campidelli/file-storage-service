package campidelli.file.storage;

import campidelli.file.storage.dto.PreSignedURL;
import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import java.io.File;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.io.Files;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Testcontainers
@Slf4j
public class AsyncMultipartDownloadIIntegrationTest extends S3MockIntegrationTest {

	@LocalServerPort
	private int port;
	@Autowired
	private WebTestClient webTestClient;
	private S3Client s3Client;
	private Resource file;
	private static final File tempDir = Files.createTempDir();
	@Container
	private static final S3MockContainer s3Mock = new S3MockContainer(S3_MOCK_VERSION)
			.withValidKmsKeys(TEST_ENC_KEYREF)
			.withInitialBuckets(INITIAL_BUCKET_NAME)
			.withVolumeAsRoot(tempDir.getAbsolutePath())
			.withEnv("debug", "true");

	@DynamicPropertySource
	static void registerDynamicProperties(DynamicPropertyRegistry registry) {
		registry.add("aws.s3.endpoint", s3Mock::getHttpEndpoint);
	}

	@BeforeEach
	public void setup() {
		webTestClient = webTestClient
				.mutate()
				//default timeout of 5 seconds is too small if async controller makes multiple outgoing calls
				.responseTimeout(Duration.ofMillis(30000))
				.baseUrl("http://localhost:" + this.port)
				.build();
		s3Client = createS3Client(s3Mock.getHttpEndpoint());
		file = getTestFile();
	}

	@Test
	@Order(1)
	public void testMultipartDownloadGetURLs() {
		uploadFileToUploadBucket();

		webTestClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/v1/async/file/multipart/url/{objectKey}")
						.build(FILE_NAME))
				.exchange()
				.expectStatus()
				.is2xxSuccessful()
				.expectHeader()
				.contentType(MediaType.APPLICATION_JSON)
				.expectBodyList(PreSignedURL.class)
				.hasSize(1)
				.value(urls -> {
					for (int i = 0; i < urls.size(); i++) {
						assertEquals(i + 1, urls.get(i).getPartNumber());
						assertNotNull(urls.get(i).getUrl());
						assertEquals(2, urls.get(i).getHeaders().size());
						assertTrue(urls.get(i).getHeaders().containsKey("host"));
						assertTrue(urls.get(i).getHeaders().containsKey("range"));
					}
				});
	}

	@Test
	@Order(2)
	public void logS3MockContainer() {
		log.info(s3Mock.getLogs());
	}

	private void uploadFileToUploadBucket() {
		try {
			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
					.bucket(INITIAL_BUCKET_NAME)
					.key(FILE_NAME)
					.contentLength(file.contentLength())
					.contentType(FILE_TYPE)
					.build();
			PutObjectResponse putObjectResponse = s3Client.putObject(
					putObjectRequest, RequestBody.fromBytes(file.getContentAsByteArray()));
			assertTrue(putObjectResponse.sdkHttpResponse().isSuccessful());

			ResponseBytes<GetObjectResponse> getObjectResponse = downloadFile(INITIAL_BUCKET_NAME, FILE_NAME);
			assertTrue(getObjectResponse.response().sdkHttpResponse().isSuccessful());

			assertEquals(file.contentLength(), getObjectResponse.response().contentLength());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private ResponseBytes<GetObjectResponse> downloadFile(String bucket, String key) {
		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.build();
		return s3Client.getObject(getObjectRequest, ResponseTransformer.toBytes());
	}
}
