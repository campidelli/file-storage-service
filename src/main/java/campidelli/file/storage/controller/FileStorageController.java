package campidelli.file.storage.controller;

import campidelli.file.storage.service.FileRepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;

@RestController
@RequestMapping("/v1/file")
public class FileStorageController {

    private final FileRepositoryService fileRepositoryService;

    @Autowired
    public FileStorageController(FileRepositoryService fileRepositoryService) {
        this.fileRepositoryService = fileRepositoryService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok("FileName: " + file.getName());
    }

    @GetMapping(value = "/download/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> downloadFile(@PathVariable String id) {
        InputStream fileStream = fileRepositoryService.getFile(id);

        final StreamingResponseBody body = outputStream -> {
            int numberOfBytesToWrite;
            byte[] data = new byte[1024];
            while ((numberOfBytesToWrite = fileStream.read(data, 0, data.length)) != -1) {
                outputStream.write(data, 0, numberOfBytesToWrite);
            }
            fileStream.close();
        };
        return ResponseEntity.ok(body);
    }
}
