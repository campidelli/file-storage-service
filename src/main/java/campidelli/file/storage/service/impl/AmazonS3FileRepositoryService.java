package campidelli.file.storage.service.impl;

import campidelli.file.storage.service.FileRepositoryService;
import com.amazonaws.AmazonClientException;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class AmazonS3FileRepositoryService implements FileRepositoryService {

    private final AmazonS3 amazonS3;
    private final TransferManager transferManager;
    private final String bucketName;

    @Autowired
    public AmazonS3FileRepositoryService(AmazonS3 amazonS3, TransferManager transferManager,
                                         @Value("${aws.s3.bucket.name}") String bucketName) {
        this.amazonS3 = amazonS3;
        this.transferManager = transferManager;
        this.bucketName = bucketName;
        createBucketIfNotExists();
    }

    private void createBucketIfNotExists() {
        if (!amazonS3.doesBucketExistV2(bucketName)) {
            amazonS3.createBucket(bucketName);
        }
    }

    @Override
    public List<String> listFiles() {
        ObjectListing objectListing = amazonS3.listObjects(bucketName);
        return objectListing.getObjectSummaries()
                .stream()
                .map(S3ObjectSummary::getKey)
                .toList();
    }

    @Override
    public InputStream getFile(String id) {
        S3Object s3Object = amazonS3.getObject(bucketName, id);
        return s3Object.getObjectContent();
    }

    @Override
    public void saveFile(String id, MultipartFile file) {
        ProgressListener progressListener =
                progressEvent -> System.out.println("Transferred bytes: " + progressEvent.getBytesTransferred());

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(file.getContentType());
        objectMetadata.setContentLength(file.getSize());

        try {
            PutObjectRequest request = new PutObjectRequest(bucketName, id, file.getInputStream(), objectMetadata);
            request.setGeneralProgressListener(progressListener);

            Upload upload = transferManager.upload(request);
            upload.waitForCompletion();
            System.out.println("Upload complete.");
        } catch (IOException | AmazonClientException | InterruptedException e) {
            System.out.println("Error occurred while uploading file");
            e.printStackTrace();
        }
    }

    @Override
    public void deleteFile(String id) {
        amazonS3.deleteObject(bucketName, id);
    }
}
