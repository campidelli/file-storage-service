package campidelli.file.storage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;

@Configuration
public class S3Configuration {

    @Bean
    public SdkHttpClient httpClient() {
        return ApacheHttpClient.builder()
                .build();
    }

    @Bean
    public AwsCredentialsProvider credentialsProvider() {
        return DefaultCredentialsProvider.create();
    }

    @Bean
    public Region region(@Value("${aws.region}") String region) {
        return Region.of(region);
    }

    @Bean
    public S3Client s3Client(AwsCredentialsProvider credentialsProvider,
                             Region region,
                             SdkHttpClient httpClient) {
        return S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(region)
                .httpClient(httpClient)
                .build();
    }

    @Bean
    public S3AsyncClient s3asyncClient(AwsCredentialsProvider credentialsProvider,
                                       Region region,
                                       @Value("${aws.s3.download.throughputInGbps}") Double throughputInGbps,
                                       @Value("${aws.s3.download.minimumPartSizeInMb}") Long minimumPartSizeInMb) {
        return S3AsyncClient.crtBuilder()
                    .credentialsProvider(credentialsProvider)
                    .region(region)
                    .targetThroughputInGbps(throughputInGbps)
                    .minimumPartSizeInBytes(minimumPartSizeInMb * MB)
                    .build();
    }

    @Bean
    public S3TransferManager transferManager(S3AsyncClient s3AsyncClient) {
        return  S3TransferManager.builder()
                    .s3Client(s3AsyncClient)
                    .build();
    }
}
