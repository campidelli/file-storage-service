package campidelli.file.storage.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Configuration {

    private final S3Properties s3Properties;

    @Autowired
    public S3Configuration(S3Properties s3Properties) {
        this.s3Properties = s3Properties;
    }

    @Bean
    public SdkHttpClient httpClient() {
        return ApacheHttpClient.builder()
                .build();
    }

    @Bean
    public AwsCredentialsProvider credentialsProvider() {
        if (s3Properties.credentials() != null) {
            AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(s3Properties.credentials().key(), s3Properties.credentials().secret());
            return StaticCredentialsProvider.create(awsBasicCredentials);
        }
        return DefaultCredentialsProvider.create();
    }

    @Bean
    public Region region() {
        return Region.of(s3Properties.region());
    }

    @Bean
    public S3Client s3Client(AwsCredentialsProvider credentialsProvider,
                             Region region,
                             SdkHttpClient httpClient) {
        S3ClientBuilder builder = S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(region)
                .forcePathStyle(true)
                .httpClient(httpClient);
        if (s3Properties.endpoint() != null) {
            builder.endpointOverride(s3Properties.endpoint());
        }
        return builder.build();
    }

    @Bean
    public S3AsyncClient s3asyncClient(AwsCredentialsProvider credentialsProvider,
                                       Region region) {
        S3CrtAsyncClientBuilder builder = S3AsyncClient.crtBuilder()
                .credentialsProvider(credentialsProvider)
                .region(region)
                .forcePathStyle(true)
                .targetThroughputInGbps(s3Properties.multipart().throughputInGbps())
                .minimumPartSizeInBytes(s3Properties.multipart().minimumPartSizeInMb() * MB)
                .checksumValidationEnabled(!s3Properties.checksumValidationDisabled());
        if (s3Properties.endpoint() != null) {
            builder.endpointOverride(s3Properties.endpoint());
        }
        return builder.build();
    }

    @Bean
    public S3TransferManager transferManager(S3AsyncClient s3AsyncClient) {
        return S3TransferManager.builder()
                    .s3Client(s3AsyncClient)
                    .build();
    }

    @Bean
    public S3Presigner preSigner(AwsCredentialsProvider credentialsProvider,
                                 Region region) {
        return S3Presigner.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();
    }
}
