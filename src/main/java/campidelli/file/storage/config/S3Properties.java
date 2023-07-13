package campidelli.file.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "aws.s3")
public record S3Properties(String region,
                           URI endpoint,
                           String bucket,
                           boolean createBucketIfNotExist,
                           boolean checksumValidationDisabled,
                           int maximumObjectSizeInMb,
                           Duration preSignedURLDuration,
                           Credentials credentials,
                           Multipart multipart) {

  public record Multipart(
      double throughputInGbps,
      int minimumPartSizeInMb) { }

  public record Credentials(
          String key,
          String secret) { }
}
