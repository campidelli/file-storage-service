package campidelli.file.storage.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class S3FileRepositoryService {
    private final S3Client s3Client;
    private final S3TransferManager transferManager;
    private final String bucket;

    @Autowired
    public S3FileRepositoryService(S3Client s3Client,
                                   S3TransferManager transferManager,
                                   @Value("${aws.s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.transferManager = transferManager;
        this.bucket = bucket;
        createBucketIfNotExists();
    }

    private void createBucketIfNotExists() {
        try {
            s3Client.headBucket(request -> request.bucket(bucket));
            log.info("S3 bucket {} found.", bucket);
        } catch (NoSuchBucketException e) {
            log.info("S3 bucket {} not found. Creating.", bucket);
            s3Client.createBucket(request -> request.bucket(bucket));
        }
    }

    public List<String> listFiles() {
        ListObjectsResponse listObjectsResponse = s3Client.listObjects(request -> request.bucket(bucket));
        if (listObjectsResponse == null) {
            log.warn("Couldn't list the S3 files.");
            return new ArrayList<String>();
        }
        return listObjectsResponse.contents().stream()
                        .map(S3Object::key)
                        .toList();
    }

    public ResponseInputStream<GetObjectResponse> getFile(String id) {
        return s3Client.getObject(request -> request.bucket(bucket).key(id));
    }

    public PutObjectResponse saveFile(String id, MultipartFile file) {
        try {
            RequestBody requestBody = RequestBody.fromInputStream(file.getInputStream(), file.getSize());
            return s3Client.putObject(request -> request.bucket(bucket).key(id), requestBody);
        } catch (IOException e) {
            log.error("Error reading the file input stream.", e);
            throw new RuntimeException(e);
        }
    }

    public DeleteObjectResponse deleteFile(String id) {
        return s3Client.deleteObject(request -> request.bucket(bucket).key(id));
    }
}
