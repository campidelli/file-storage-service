package campidelli.file.storage.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface FileRepositoryService {

    List<String> listFiles();
    InputStream getFile(String id);
    void saveFile(String id, MultipartFile file);
    void deleteFile(String id);
}
