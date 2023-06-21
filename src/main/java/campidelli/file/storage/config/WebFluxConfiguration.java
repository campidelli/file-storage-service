package campidelli.file.storage.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.PathMatchConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class WebFluxConfiguration implements WebFluxConfigurer {

    @Override
    public void configurePathMatching(PathMatchConfigurer configurer) {
        configurer.setUseTrailingSlashMatch(true);
    }
}