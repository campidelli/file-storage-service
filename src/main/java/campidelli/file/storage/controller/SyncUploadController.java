package campidelli.file.storage.controller;

import campidelli.file.storage.service.S3SyncFileRepositoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/sync/file")
@Slf4j
public class SyncUploadController {

    private final S3SyncFileRepositoryService fileRepositoryService;

    @Autowired
    public SyncUploadController(S3SyncFileRepositoryService fileRepositoryService) {
        this.fileRepositoryService = fileRepositoryService;
    }

    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void uploadFile(@RequestParam("file") MultipartFile file) {
        long start = System.currentTimeMillis();
        fileRepositoryService.saveFile(file);
        log.info("POST '/v1/sync/file' elapsed time: {} ms.", System.currentTimeMillis() - start);
    }

    @DeleteMapping( "/{id}")
    public void deleteFile(@PathVariable String id) {
        long start = System.currentTimeMillis();
        fileRepositoryService.deleteFile(id);
        log.info("DELETE '/v1/sync/file/{}' - elapsed time: {} ms.", id, System.currentTimeMillis() - start);
    }
}
