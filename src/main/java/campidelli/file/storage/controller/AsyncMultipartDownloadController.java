package campidelli.file.storage.controller;

import campidelli.file.storage.config.S3Properties;
import campidelli.file.storage.dto.PreSignedURL;
import campidelli.file.storage.service.S3AsyncMultipartFileRepositoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

@RestController
@RequestMapping("/v1/async/file/multipart")
@Slf4j
public class AsyncMultipartDownloadController {
    private final S3AsyncMultipartFileRepositoryService s3Service;
    private final S3Properties s3Properties;

    public AsyncMultipartDownloadController(S3AsyncMultipartFileRepositoryService s3Service, S3Properties s3Properties) {
        this.s3Service = s3Service;
        this.s3Properties = s3Properties;
    }

    @GetMapping(path = "/{objectKey}")
    public Mono<Void> multipartDownload(@PathVariable("objectKey") String key, ServerHttpResponse response) {
        Flux<DataBuffer> body = preSignedMultipartDownloadURLs(key)
                .flatMapMany(Flux::fromIterable)
                .flatMapSequential(this::downloadPart);
        return response.writeWith(Flux.from(body));
    }

    private Mono<DataBuffer> downloadPart(PreSignedURL preSignedURL) {
        log.info("Processing part {}", preSignedURL.getPartNumber());
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(preSignedURL.getUrl().toExternalForm());
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        return WebClient
                .builder()
                .uriBuilderFactory(factory)
                .codecs(codecs -> codecs
                        .defaultCodecs()
                        .maxInMemorySize(s3Properties.maximumObjectSizeInMb() * 1024 * 1024))
                .build()
                .get()
                .headers(getHeadersFromPreSignedURL(preSignedURL))
                .retrieve()
                .bodyToMono(DataBuffer.class);
    }

    private Consumer<HttpHeaders> getHeadersFromPreSignedURL(PreSignedURL url) {
        return httpHeaders -> {
            httpHeaders.addAll(CollectionUtils.toMultiValueMap(url.getHeaders()));
        };
    }

    @GetMapping(path = "/url/{objectKey}")
    public Mono<List<PreSignedURL>> preSignedMultipartDownloadURLs(@PathVariable("objectKey") String key) {
        return s3Service.getPreSignedGetObjectURLs(s3Properties.bucket(), key);
    }
}