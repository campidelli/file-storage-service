package campidelli.file.storage.service;

import campidelli.file.storage.config.S3Properties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.awscore.presigner.PresignedRequest;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.CreateMultipartUploadPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedCreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.utils.StringInputStream;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class S3AsyncMultipartFileRepositoryService {

    private final S3Client s3Client;
    private final S3Presigner preSigner;
    private final SdkHttpClient httpClient;
    private final S3Properties s3Properties;

    @Autowired
    public S3AsyncMultipartFileRepositoryService(S3Client s3Client,
                                                 S3Presigner preSigner,
                                                 SdkHttpClient httpClient,
                                                 S3Properties s3Properties) {
        this.s3Client = s3Client;
        this.preSigner = preSigner;
        this.httpClient = httpClient;
        this.s3Properties = s3Properties;
    }

    public List<String> generatePreSignedURLs(String key) {

        CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                .bucket(s3Properties.bucket())
                .key(key)
                .build();

        CreateMultipartUploadPresignRequest createMultipartUploadPresignRequest = CreateMultipartUploadPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .createMultipartUploadRequest(createMultipartUploadRequest)
                .build();

        PresignedCreateMultipartUploadRequest presignedCreateMultipartUploadRequest =
                preSigner.presignCreateMultipartUpload(createMultipartUploadPresignRequest);

        HttpExecuteResponse response = execute(presignedCreateMultipartUploadRequest, null);
        if (!response.httpResponse().isSuccessful()) {
            throw new RuntimeException("Can't create pre-signed URL for multipart upload.");
        }

        ListMultipartUploadsRequest listMultipartUploadsRequest = ListMultipartUploadsRequest.builder()
                .bucket(s3Properties.bucket())
                .prefix(key)
                .build();

        SdkIterable<MultipartUpload> uploads = s3Client.listMultipartUploadsPaginator(listMultipartUploadsRequest)
                .uploads();

        uploads.forEach(u -> {
            System.out.println(u.uploadId());
        });

        return new ArrayList<>();
    }

    private HttpExecuteResponse execute(PresignedRequest presigned, String payload) {
        ContentStreamProvider requestPayload = payload == null ? null : () -> new StringInputStream(payload);

        HttpExecuteRequest request = HttpExecuteRequest.builder()
                .request(presigned.httpRequest())
                .contentStreamProvider(requestPayload)
                .build();

        try {
            return httpClient.prepareRequest(request).call();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
