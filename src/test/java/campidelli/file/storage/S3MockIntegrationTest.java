package campidelli.file.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.utils.AttributeMap;

import java.net.URI;

import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

@Slf4j
public abstract class S3MockIntegrationTest {

    protected static final String S3_MOCK_VERSION = "3.0.0";
    protected static final String INITIAL_BUCKET_NAME = "campidelli-file-storage-service-bucket";
    protected static final String TEST_ENC_KEYREF =
            "arn:aws:kms:us-east-1:1234567890:key/valid-test-key-ref";
    protected static final String FILE_NAME = "the-return-of-sherlock-holmes.pdf";
    protected static final String FILE_TYPE = MediaType.APPLICATION_PDF_VALUE;

    protected S3Client createS3Client(String endpoint) {
        return S3Client.builder()
                .region(Region.of("ap-southeast-2"))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create("foo", "bar")
                        )
                )
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true).build()
                )
                .endpointOverride(URI.create(endpoint))
                .httpClient(
                        ApacheHttpClient.builder()
                                .buildWithDefaults(
                                    AttributeMap.builder()
                                            .put(TRUST_ALL_CERTIFICATES, Boolean.TRUE)
                                            .build()
                                )
                )
                .build();
    }

    protected Resource getTestFile() {
        return new ClassPathResource(FILE_NAME);
    }
}
