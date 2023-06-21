package campidelli.file.storage.controller;

import campidelli.file.storage.service.S3AsyncFileRepositoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.List;

import static java.net.URLConnection.guessContentTypeFromName;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/v1/async/file")
@Slf4j
public class AsyncFileStorageController {

    private final S3AsyncFileRepositoryService fileRepositoryService;

    @Autowired
    public AsyncFileStorageController(S3AsyncFileRepositoryService fileRepositoryService) {
        this.fileRepositoryService = fileRepositoryService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Mono<List<String>> listFiles() {
        return fileRepositoryService.listFiles().collectList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Mono<Void> uploadFile(@RequestHeader HttpHeaders headers, @RequestPart("file") FilePart file) {
        long length = headers.getContentLength() < 0 ? file.headers().getContentLength() : headers.getContentLength();
        if (length < 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Header Content-Length must informed.");
        }
        return fileRepositoryService.saveFile(file.content(), guessContentTypeFromName(file.filename()), length, file.filename());
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

    @DeleteMapping( "/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Void> deleteFile(@PathVariable String id) {
        return fileRepositoryService.deleteFile(id);
    }
}
