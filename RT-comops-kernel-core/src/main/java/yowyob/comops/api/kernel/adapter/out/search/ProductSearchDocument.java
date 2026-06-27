package yowyob.comops.api.kernel.adapter.out.search;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "iwm-product-search-v1", createIndex = true)
public record ProductSearchDocument(
        @Id UUID id,
        @Field(type = FieldType.Keyword) UUID tenantId,
        @Field(type = FieldType.Keyword) UUID organizationId,
        @Field(type = FieldType.Keyword) String sku,
        @Field(type = FieldType.Text) String name,
        @Field(type = FieldType.Keyword) String familyCode,
        @Field(type = FieldType.Text) String variantLabel,
        @Field(type = FieldType.Keyword) String barcode,
        @Field(type = FieldType.Text) String description,
        @Field(type = FieldType.Double) BigDecimal unitPrice,
        @Field(type = FieldType.Keyword) String currency,
        @Field(type = FieldType.Keyword) String status) {
}
