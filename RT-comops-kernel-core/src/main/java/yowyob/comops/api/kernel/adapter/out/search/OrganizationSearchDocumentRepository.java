package yowyob.comops.api.kernel.adapter.out.search;

import java.util.UUID;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;

public interface OrganizationSearchDocumentRepository
        extends ReactiveElasticsearchRepository<OrganizationSearchDocument, UUID> {
}
