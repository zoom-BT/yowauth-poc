package yowyob.comops.api.kernel.adapter.out.search;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "iwm-organization-search-v1", createIndex = true)
public record OrganizationSearchDocument(
        @Id UUID id,
        @Field(type = FieldType.Keyword) UUID tenantId,
        @Field(type = FieldType.Keyword) UUID businessActorId,
        @Field(type = FieldType.Keyword) String code,
        @Field(type = FieldType.Keyword) String service,
        @Field(type = FieldType.Text) String shortName,
        @Field(type = FieldType.Text) String longName,
        @Field(type = FieldType.Keyword) String legalForm,
        @Field(type = FieldType.Boolean) boolean isActive,
        @Field(type = FieldType.Keyword) String status) {
}
