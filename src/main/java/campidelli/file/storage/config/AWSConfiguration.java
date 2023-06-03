package campidelli.file.storage.config;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

@Configuration
public class AWSConfiguration {

    @Bean
    public AWSCredentialsProvider awsCredentialsProvider() {
        return new DefaultAWSCredentialsProviderChain();
    }

    @Bean
    public AmazonS3 amazonS3(AWSCredentialsProvider awsCredentialsProvider,
                             @Value("${aws.region}") String region) {
        return AmazonS3ClientBuilder
                .standard()
                .withCredentials(awsCredentialsProvider)
                .withRegion(region)
                .build();
    }

    @Bean
    public TransferManager transferManager(AmazonS3 amazonS3,
                                           @Value("${aws.s3.upload.maxThreads}") int maxUploadThreads,
                                           @Value("${aws.s3.upload.multipart.threshold}") long multipartUploadThreshold) {
        return TransferManagerBuilder
                .standard()
                .withS3Client(amazonS3)
                .withMultipartUploadThreshold((multipartUploadThreshold))
                .withExecutorFactory(() -> Executors.newFixedThreadPool(maxUploadThreads))
                .build();
    }
}
