package campidelli.file.storage.service;

import campidelli.file.storage.config.S3Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class S3SyncFileRepositoryService {
    private final S3Client s3Client;
    private final S3Properties s3Properties;

    @Autowired
    public S3SyncFileRepositoryService(S3Client s3Client,
                                       S3Properties s3Properties) {
        this.s3Client = s3Client;
        this.s3Properties = s3Properties;
    }

    public void createBucketIfNotExists() {
        try {
            s3Client.headBucket(request -> request.bucket(s3Properties.bucket()));
            log.info("S3 bucket '{}' found.", s3Properties.bucket());
        } catch (NoSuchBucketException e) {
            log.info("S3 bucket '{}' not found. Creating.", s3Properties.bucket());
            s3Client.createBucket(request -> request.bucket(s3Properties.bucket()));
        }
    }

    public List<String> listFiles() {
        ListObjectsResponse listObjectsResponse = s3Client.listObjects(request -> request.bucket(s3Properties.bucket()));
        if (listObjectsResponse == null) {
            log.warn("Couldn't list the S3 files.");
            return new ArrayList<String>();
        }
        return listObjectsResponse.contents().stream()
                        .map(S3Object::key)
                        .toList();
    }

    public ResponseInputStream<GetObjectResponse> getFile(String id) {
        return s3Client.getObject(request -> request.bucket(s3Properties.bucket()).key(id));
    }

    public void saveFile(MultipartFile file) {
        try {
            RequestBody requestBody = RequestBody.fromInputStream(file.getInputStream(), file.getSize());
            s3Client.putObject(request -> request.bucket(s3Properties.bucket()).key(file.getOriginalFilename()), requestBody);
        } catch (IOException e) {
            log.error("Error reading the file input stream.", e);
            throw new RuntimeException(e);
        }
    }

    public void deleteFile(String id) {
        s3Client.deleteObject(request -> request.bucket(s3Properties.bucket()).key(id));
    }
}
