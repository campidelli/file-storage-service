package campidelli.file.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(prefix = "aws.s3")
public record S3Properties(String region,
                           URI endpoint,
                           String bucket,
                           boolean createBucketIfNotExist,
                           boolean checksumValidationDisabled,
                           Multipart multipart) {

  public record Multipart(
      double throughputInGbps,
      long minimumPartSizeInMb) {

  }
}
