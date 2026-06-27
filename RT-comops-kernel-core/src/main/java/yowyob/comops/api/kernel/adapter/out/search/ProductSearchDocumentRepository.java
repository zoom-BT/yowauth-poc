package yowyob.comops.api.kernel.adapter.out.search;

import java.util.UUID;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;

public interface ProductSearchDocumentRepository extends ReactiveElasticsearchRepository<ProductSearchDocument, UUID> {
}
