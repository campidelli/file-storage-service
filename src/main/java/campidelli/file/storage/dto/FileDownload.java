package campidelli.file.storage.dto;

import lombok.Builder;
import lombok.Data;
import reactor.core.publisher.Flux;

import java.nio.ByteBuffer;

@Data
@Builder
public class FileDownload {
    private Flux<ByteBuffer> content;
    private String type;
    private long length;
    private String name;
}
