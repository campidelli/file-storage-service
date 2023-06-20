package campidelli.file.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(prefix = "aws.s3")
@Data
public class S3Properties {

    private String region;
    private URI endpoint;
    private String bucket;
    private boolean createBucketIfNotExist;
    private Multipart multipart;

    @Data
    public static class Multipart {
        private double throughputInGbps;
        private long minimumPartSizeInMb;
    }
}
