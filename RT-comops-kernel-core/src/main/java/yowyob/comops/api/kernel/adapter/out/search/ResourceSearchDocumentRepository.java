package yowyob.comops.api.kernel.adapter.out.search;

import java.util.UUID;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;

public interface ResourceSearchDocumentRepository extends ReactiveElasticsearchRepository<ResourceSearchDocument, UUID> {
}
