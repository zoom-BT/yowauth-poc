package yowyob.comops.api.kernel.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories;

@Configuration
@ConditionalOnProperty(prefix = "iwm.search.elasticsearch", name = "enabled", havingValue = "true")
@EnableReactiveElasticsearchRepositories(basePackages = "yowyob.comops.api.kernel.adapter.out.search")
public class KernelSearchConfiguration {
}
