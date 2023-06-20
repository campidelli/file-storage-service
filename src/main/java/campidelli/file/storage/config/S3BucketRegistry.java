package campidelli.file.storage.config;

import campidelli.file.storage.service.S3SyncFileRepositoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class S3BucketRegistry implements InitializingBean {

    private final S3Properties s3Properties;
    private final S3SyncFileRepositoryService s3Service;

    @Autowired
    public S3BucketRegistry(S3Properties s3Properties, S3SyncFileRepositoryService s3Service) {
        this.s3Properties = s3Properties;
        this.s3Service = s3Service;
    }

    @Override
    public void afterPropertiesSet() {
        if (s3Properties.isCreateBucketIfNotExist()) {
            s3Service.createBucketIfNotExists();
        }
    }
}
