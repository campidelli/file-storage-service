package campidelli.file.storage.controller;

import campidelli.file.storage.service.S3AsyncFileRepositoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.List;

@RestController
@RequestMapping("/v1/async/file")
@Slf4j
public class AsyncDownloadController {

    private final S3AsyncFileRepositoryService fileRepositoryService;

    @Autowired
    public AsyncDownloadController(S3AsyncFileRepositoryService fileRepositoryService) {
        this.fileRepositoryService = fileRepositoryService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Mono<List<String>> listFiles() {
        return fileRepositoryService.listFiles().collectList();
    }

    @GetMapping(value = "/{id}")
    public Mono<ResponseEntity<Flux<ByteBuffer>>> downloadFile(@PathVariable String id) {
        return fileRepositoryService.getFile(id)
            .map(fileDownload -> ResponseEntity.ok()
                .contentLength(fileDownload.getLength())
                .contentType(MediaType.valueOf(fileDownload.getType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileDownload.getName() + "\"")
                .body(fileDownload.getContent()));
    }
}
