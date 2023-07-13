package campidelli.file.storage.controller;

import campidelli.file.storage.service.S3SyncFileRepositoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/v1/sync/file")
@Slf4j
public class SyncDownloadController {

    private final S3SyncFileRepositoryService fileRepositoryService;

    @Autowired
    public SyncDownloadController(S3SyncFileRepositoryService fileRepositoryService) {
        this.fileRepositoryService = fileRepositoryService;
    }

    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> listFiles() {
        long start = System.currentTimeMillis();
        ResponseEntity<List<String>> response = ResponseEntity.ok(fileRepositoryService.listFiles());
        log.info("GET '/v1/sync/file' elapsed time: {} ms.", System.currentTimeMillis() - start);
        return response;
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> downloadFile(@PathVariable String id) {
        long start = System.currentTimeMillis();
        InputStream fileStream = fileRepositoryService.getFile(id);

        final StreamingResponseBody body = outputStream -> {
            int numberOfBytesToWrite;
            byte[] data = new byte[1024];
            while ((numberOfBytesToWrite = fileStream.read(data, 0, data.length)) != -1) {
                outputStream.write(data, 0, numberOfBytesToWrite);
            }
            fileStream.close();
        };
        log.info("GET '/v1/sync/file/{}' elapsed time: {} ms.", id, System.currentTimeMillis() - start);
        return ResponseEntity.ok(body);
    }
}
