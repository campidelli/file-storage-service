package campidelli.file.storage.service;

import campidelli.file.storage.config.S3Properties;
import campidelli.file.storage.dto.PreSignedURL;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public
class S3AsyncMultipartFileRepositoryService {

    private final S3AsyncClient s3Client;
    private final S3Properties s3Properties;
    private final S3Presigner s3Presigner;

    public S3AsyncMultipartFileRepositoryService(S3AsyncClient s3Client,
                                                 S3Properties s3Properties,
                                                 S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Properties = s3Properties;
        this.s3Presigner = s3Presigner;
    }

    public URL getPreSignedGetObjectURL(GetObjectRequest getObjectRequest, Duration duration) {
        GetObjectPresignRequest preSignedGetObjectRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration) // Set the expiration time for the URL
                .getObjectRequest(getObjectRequest)
                .build();
        // Get the pre-signed URL
        return s3Presigner.presignGetObject(preSignedGetObjectRequest).url();
    }

    public URL getPreSignedOPutObjectURL(PutObjectRequest putObjectRequest, Duration duration) {
        PutObjectPresignRequest preSignPutObjectRequest = PutObjectPresignRequest.builder()
                .signatureDuration(duration)
                .putObjectRequest(putObjectRequest)
                .build();
        return s3Presigner.presignPutObject(preSignPutObjectRequest).url();
    }

    public Mono<List<PreSignedURL>> getPreSignedGetObjectURLs(String bucket, String key) {
        GetObjectAttributesRequest request = GetObjectAttributesRequest.builder()
                .bucket(bucket)
                .key(key)
                .objectAttributes(ObjectAttributes.OBJECT_SIZE, ObjectAttributes.OBJECT_PARTS)
                .build();

        return Mono.fromFuture(s3Client.getObjectAttributes(request))
                .map(response -> getPreSignedGetObjectURLs(bucket, key, response));
    }

    private List<PreSignedURL> getPreSignedGetObjectURLs(String bucket, String key, GetObjectAttributesResponse response) {
        if (response.objectParts() != null && response.objectParts().totalPartsCount() > 0) {
            return getPreSignedGetObjectURLsUsingObjectParts(bucket, key, response.objectParts().totalPartsCount());
        }
        return getPreSignedGetObjectURLsUsingByteRange(bucket, key, response.objectSize());
    }

    private List<PreSignedURL> getPreSignedGetObjectURLsUsingObjectParts(String bucket, String key, int numberOfParts) {
        List<PreSignedURL> result = new ArrayList<>(numberOfParts);
        for (int partNumber = 1; partNumber <= numberOfParts; partNumber++) {
            result.add(getPreSignedGetObjectURLObjectParts(bucket, key, partNumber));
        }
        return result;
    }

    private List<PreSignedURL> getPreSignedGetObjectURLsUsingByteRange(String bucket, String key, long objectSize) {
        int numberOfParts = (int) Math.ceil((double) objectSize / s3Properties.multipart().minimumPartSizeInMb());
        List<PreSignedURL> result = new ArrayList<>(numberOfParts);
        for (int partNumber = 1; partNumber <= numberOfParts; partNumber++) {
            String range = getRange(partNumber, objectSize, s3Properties.multipart().minimumPartSizeInMb());
            result.add(getPreSignedGetObjectURLUsingByteRange(bucket, key, partNumber, range));
        }
        return result;
    }

    private String getRange(int partNumber, long objectSize, long chunkSize) {
        long from = calculateRangeFrom(partNumber, chunkSize);
        long to = calculateRangeTo(partNumber, objectSize < chunkSize ? objectSize : chunkSize);
        return String.format("bytes=%d-%d",from, to);
    }

    private long calculateRangeFrom(int partNumber, long chunkSize) {
        return (partNumber - 1) * chunkSize;
    }

    private long calculateRangeTo(int partNumber, long chunkSize) {
        return (partNumber * chunkSize) - 1;
    }

    private PreSignedURL getPreSignedGetObjectURLObjectParts(String bucket, String key, int partNumber) {
        return getPreSignedGetObjectURL(bucket, key, partNumber, Optional.empty());
    }

    private PreSignedURL getPreSignedGetObjectURLUsingByteRange(String bucket, String key, int partNumber, String range) {
        return getPreSignedGetObjectURL(bucket, key, partNumber, Optional.of(range));
    }

    private PreSignedURL getPreSignedGetObjectURL(String bucket,
                                                  String key,
                                                  int partNumber,
                                                  Optional<String> range) {

        GetObjectRequest getObjectRequest = makeGetObjectRequest(bucket, key, partNumber, range);
        GetObjectPresignRequest preSignRequest = makeGetObjectPresignRequest(getObjectRequest);
        PresignedGetObjectRequest preSignedGetObjectRequest = s3Presigner.presignGetObject(preSignRequest);

        return PreSignedURL.builder()
                .partNumber(partNumber)
                .url(preSignedGetObjectRequest.url())
                .headers(preSignedGetObjectRequest.signedHeaders())
                .build();
    }

    private GetObjectRequest makeGetObjectRequest(String bucket,
                                                  String key,
                                                  int partNumber,
                                                  Optional<String> range) {
        GetObjectRequest.Builder builder = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key);
        if (range.isPresent()) {
            builder.range(range.get());
        } else {
            // We must only use the GET param 'partNumber' for downloading a file
            // that was uploaded as multipart. In this case 'range' has to be empty.
            builder.partNumber(partNumber);
        }
        return builder.build();
    }

    private GetObjectPresignRequest makeGetObjectPresignRequest(GetObjectRequest getObjectRequest) {
        return GetObjectPresignRequest.builder()
                .signatureDuration(s3Properties.preSignedURLDuration())
                .getObjectRequest(getObjectRequest).build();
    }
}
