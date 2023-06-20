package campidelli.file.storage.service;

import campidelli.file.storage.config.S3Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class S3AsyncFileRepositoryService {

    private final S3AsyncClient s3AsyncClient;
    private final S3TransferManager transferManager;
    private final S3Properties s3Properties;

    @Autowired
    public S3AsyncFileRepositoryService(S3AsyncClient s3AsyncClient,
                                        S3TransferManager transferManager,
                                        S3Properties s3Properties) {
        this.s3AsyncClient = s3AsyncClient;
        this.transferManager = transferManager;
        this.s3Properties = s3Properties;
    }

    public CompletableFuture<List<String>> listFiles() {
        return s3AsyncClient.listObjects(request -> request.bucket(s3Properties.getBucket()))
                .thenCompose(listObjectsResponse -> {
                    if (listObjectsResponse == null) {
                        log.warn("Couldn't list the S3 files.");
                        return CompletableFuture.completedFuture(new ArrayList<>());
                    }
                    return CompletableFuture.completedFuture(
                            listObjectsResponse.contents().stream()
                                .map(S3Object::key)
                                .toList());
                });
    }

    public CompletableFuture<ResponseInputStream<GetObjectResponse>> getFile(String id) {
        return s3AsyncClient.getObject(request -> request.bucket(s3Properties.getBucket()).key(id),
                AsyncResponseTransformer.toBlockingInputStream());
    }

    public CompletableFuture<PutObjectResponse> saveFile(MultipartFile file) {
        try {
            AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromInputStream(
                    file.getInputStream(), file.getSize(), Executors.newSingleThreadExecutor());

            return s3AsyncClient.putObject(
                    request -> request.bucket(s3Properties.getBucket()).key(file.getOriginalFilename()),
                    asyncRequestBody);
        } catch (IOException e) {
            log.error("Error reading the file input stream.", e);
            throw new RuntimeException(e);
        }
    }

    public CompletableFuture<DeleteObjectResponse> deleteFile(String id) {
        return s3AsyncClient.deleteObject(request -> request.bucket(s3Properties.getBucket()).key(id));
    }
}
