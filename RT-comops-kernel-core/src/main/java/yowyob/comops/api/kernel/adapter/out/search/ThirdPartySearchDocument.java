package yowyob.comops.api.kernel.adapter.out.search;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

@Document(indexName = "iwm-third-party-search-v1", createIndex = true)
public record ThirdPartySearchDocument(
                @Id UUID id,
                @Field(type = FieldType.Keyword) UUID tenantId,
                @Field(type = FieldType.Keyword) UUID organizationId,
                @Field(type = FieldType.Keyword) String code,
                @Field(type = FieldType.Keyword) String referenceCode,
                @MultiField(mainField = @Field(type = FieldType.Text), otherFields = {
                                @InnerField(suffix = "keyword", type = FieldType.Keyword) }) String name,
                @MultiField(mainField = @Field(type = FieldType.Text), otherFields = {
                                @InnerField(suffix = "keyword", type = FieldType.Keyword) }) String displayName,
                @Field(type = FieldType.Keyword) String type,
                @Field(type = FieldType.Text) String longName,
                @Field(type = FieldType.Keyword) Set<String> roles,
                @Field(type = FieldType.Boolean) boolean prospect,
                @Field(type = FieldType.Keyword) String accountingAccount,
                @Field(type = FieldType.Keyword) String segment,
                @Field(type = FieldType.Integer) Integer qualificationScore,
                @Field(type = FieldType.Boolean) boolean enabled,
                @Field(type = FieldType.Boolean) boolean active,
                @Field(type = FieldType.Date) Instant lastContactedAt,
                @Field(type = FieldType.Date) Instant nextFollowUpAt,
                @Field(type = FieldType.Keyword) String followUpStatus,
                @Field(type = FieldType.Date) Instant convertedAt,
                @Field(type = FieldType.Keyword) String partyType,
                @Field(type = FieldType.Keyword) UUID partyId) {
}
