package campidelli.file.storage.controller;

import campidelli.file.storage.service.S3AsyncFileRepositoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/v1/async/file")
@Slf4j
public class AsyncFileStorageController {

    private final S3AsyncFileRepositoryService fileRepositoryService;

    @Autowired
    public AsyncFileStorageController(S3AsyncFileRepositoryService fileRepositoryService) {
        this.fileRepositoryService = fileRepositoryService;
    }

    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CompletableFuture<List<String>>> listFiles() {
        long start = System.currentTimeMillis();
        ResponseEntity<CompletableFuture<List<String>>> response = ResponseEntity.ok(fileRepositoryService.listFiles());
        log.info("GET '/v1/sync/file' elapsed time: {} ms.", System.currentTimeMillis() - start);
        return response;
    }

    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CompletableFuture<PutObjectResponse>> uploadFile(@RequestParam("file") MultipartFile file) {
        long start = System.currentTimeMillis();
        ResponseEntity<CompletableFuture<PutObjectResponse>> response = ResponseEntity.ok(fileRepositoryService.saveFile(file));
        log.info("POST '/v1/sync/file' elapsed time: {} ms.", System.currentTimeMillis() - start);
        return response;
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> downloadFile(@PathVariable String id) {
        long start = System.currentTimeMillis();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> fileStream = fileRepositoryService.getFile(id);
//
//        final StreamingResponseBody body = outputStream -> {
//            int numberOfBytesToWrite;
//            byte[] data = new byte[1024];
//            while ((numberOfBytesToWrite = fileStream.read(data, 0, data.length)) != -1) {
//                outputStream.write(data, 0, numberOfBytesToWrite);
//            }
//            fileStream.close();
//        };
        log.info("GET '/v1/sync/file/{}' elapsed time: {} ms.", id, System.currentTimeMillis() - start);
        return ResponseEntity.ok(null);
    }

    @DeleteMapping( "/{id}")
    public ResponseEntity<CompletableFuture<DeleteObjectResponse>> deleteFile(@PathVariable String id) {
        long start = System.currentTimeMillis();
        ResponseEntity<CompletableFuture<DeleteObjectResponse>> response = ResponseEntity.ok(fileRepositoryService.deleteFile(id));
        log.info("DELETE '/v1/sync/file/{}' - elapsed time: {} ms.", id, System.currentTimeMillis() - start);
        return response;
    }
}
