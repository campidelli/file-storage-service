package campidelli.file.storage.service;

import campidelli.file.storage.config.S3Properties;
import campidelli.file.storage.dto.FileDownload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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

    public Flux<String> listFiles() {
        return Flux.create((emitter) -> {
            CompletableFuture<ListObjectsResponse> future = s3AsyncClient.listObjects(
                    request -> request.bucket(s3Properties.getBucket()));
            future.whenComplete((response, throwable) -> {
                if (throwable == null) {
                    response.contents().stream()
                            .map(S3Object::key)
                            .forEach(emitter::next);
                }
                emitter.complete();
            });
        });
    }

    public Mono<FileDownload> getFile(String id) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(id)
                .build();

        CompletableFuture<ResponsePublisher<GetObjectResponse>> future = s3AsyncClient.getObject(getObjectRequest,
                AsyncResponseTransformer.toPublisher());

        return Mono.fromFuture(future)
                .map(response -> FileDownload.builder()
                        .name(getMetadataItem(response.response(), "filename", id))
                        .type(response.response().contentType())
                        .length(response.response().contentLength())
                        .content(Flux.from(response))
                        .build());
    }

    private String getMetadataItem(GetObjectResponse sdkResponse, String key, String defaultValue) {
        for (Map.Entry<String, String> entry : sdkResponse.metadata().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return defaultValue;
    }

    public Mono<Void> saveFile(Flux<DataBuffer> dataBufferFlux, String type, Long length, String id) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(id)
                .contentLength(length)
                .contentType(type)
                .build();

        Flux<ByteBuffer> byteBufferFlux = dataBufferFlux
                .flatMap(dataBuffer -> Flux.fromIterable(dataBuffer::readableByteBuffers));

        UploadRequest uploadRequest = UploadRequest.builder()
                .putObjectRequest(putObjectRequest)
                .requestBody(AsyncRequestBody.fromPublisher(byteBufferFlux))
                .addTransferListener(LoggingTransferListener.create())
                .build();

        Upload upload = transferManager.upload(uploadRequest);

        return Mono.fromFuture(upload.completionFuture())
                .map(completedUpload -> completedUpload.response())
                .handle((response, sink) -> {
                    if (response.sdkHttpResponse() == null || !response.sdkHttpResponse().isSuccessful()) {
                        sink.error(new RuntimeException(response.sdkHttpResponse().toString()));
                    }
                }).then();
    }

    public Mono<Void> deleteFile(String id) {
        CompletableFuture<DeleteObjectResponse> future = s3AsyncClient.deleteObject(
                request -> request.bucket(s3Properties.getBucket()).key(id));
        return Mono.fromFuture(future).then();
    }
}
