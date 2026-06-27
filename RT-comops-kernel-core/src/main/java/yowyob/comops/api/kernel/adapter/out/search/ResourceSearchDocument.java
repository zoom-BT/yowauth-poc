package yowyob.comops.api.kernel.adapter.out.search;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "iwm-resource-search-v1", createIndex = true)
public record ResourceSearchDocument(
        @Id UUID id,
        @Field(type = FieldType.Keyword) UUID tenantId,
        @Field(type = FieldType.Keyword) UUID organizationId,
        @Field(type = FieldType.Keyword) UUID agencyId,
        @Field(type = FieldType.Keyword) String resourceCode,
        @Field(type = FieldType.Text) String name,
        @Field(type = FieldType.Keyword) String category,
        @Field(type = FieldType.Keyword) String serialNumber,
        @Field(type = FieldType.Keyword) String status,
        @Field(type = FieldType.Keyword) String ipAddress,
        @Field(type = FieldType.Keyword) String macAddress,
        @Field(type = FieldType.Double) Double latitude,
        @Field(type = FieldType.Double) Double longitude) {

    public ResourceSearchDocument withStatus(String updatedStatus) {
        return new ResourceSearchDocument(id, tenantId, organizationId, agencyId, resourceCode, name, category,
                serialNumber, updatedStatus, ipAddress, macAddress, latitude, longitude);
    }
}
