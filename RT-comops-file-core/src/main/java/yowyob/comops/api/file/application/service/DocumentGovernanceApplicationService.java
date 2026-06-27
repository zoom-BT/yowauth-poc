package yowyob.comops.api.file.application.service;

import yowyob.comops.api.file.application.port.out.DocumentGovernancePolicyRepository;
import yowyob.comops.api.file.application.port.out.DocumentLinkRepository;
import yowyob.comops.api.file.application.port.out.DocumentReviewRepository;
import yowyob.comops.api.file.domain.model.DocumentGovernancePolicy;
import yowyob.comops.api.file.domain.model.DocumentLink;
import yowyob.comops.api.file.domain.model.DocumentReview;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class DocumentGovernanceApplicationService {

    private final DocumentGovernancePolicyRepository documentGovernancePolicyRepository;
    private final DocumentReviewRepository documentReviewRepository;
    private final DocumentLinkRepository documentLinkRepository;

    public DocumentGovernanceApplicationService(DocumentGovernancePolicyRepository documentGovernancePolicyRepository,
            DocumentReviewRepository documentReviewRepository, DocumentLinkRepository documentLinkRepository) {
        this.documentGovernancePolicyRepository = documentGovernancePolicyRepository;
        this.documentReviewRepository = documentReviewRepository;
        this.documentLinkRepository = documentLinkRepository;
    }

    public Mono<DocumentGovernancePolicy> upsertPolicy(UUID tenantId, UUID organizationId, UUID agencyId,
            String targetType, String documentCategory, UpsertDocumentGovernancePolicyCommand command) {
        return documentGovernancePolicyRepository.findByScope(tenantId, organizationId, agencyId, targetType,
                        documentCategory)
                .defaultIfEmpty(DocumentGovernancePolicy.create(tenantId, organizationId, agencyId, targetType,
                        documentCategory, command.mandatory(), command.approvalRequired(), command.expiryDays(),
                        command.reviewerResponsibilityType()))
                .map(existing -> existing.update(command.mandatory(), command.approvalRequired(),
                        command.expiryDays(), command.reviewerResponsibilityType()))
                .flatMap(documentGovernancePolicyRepository::save);
    }

    public Mono<DocumentGovernancePolicy> getPolicy(UUID tenantId, UUID organizationId, UUID agencyId,
            String targetType, String documentCategory) {
        return documentGovernancePolicyRepository.findByScope(tenantId, organizationId, agencyId, targetType,
                        documentCategory)
                .switchIfEmpty(Mono.just(DocumentGovernancePolicy.create(tenantId, organizationId, agencyId,
                        targetType, documentCategory, false, false, null, null)));
    }

    public Mono<DocumentReview> review(UUID tenantId, UUID documentLinkId, UUID reviewerUserId, String reviewStatus,
            Instant expiresAt, String notes) {
        return documentLinkRepository.findById(tenantId, documentLinkId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("document link not found")))
                .flatMap(link -> getPolicy(tenantId, link.organizationId(), null, link.targetType(),
                                link.documentCategory())
                        .flatMap(policy -> documentReviewRepository.save(DocumentReview.review(tenantId,
                                link.organizationId(), link.id(), reviewerUserId, reviewStatus,
                                expiresAt == null && policy.expiryDays() != null
                                        ? Instant.now().plus(policy.expiryDays(), ChronoUnit.DAYS)
                                        : expiresAt,
                                notes))));
    }

    public Flux<DocumentStatusView> targetStatus(UUID tenantId, String targetType, UUID targetId) {
        return documentLinkRepository.findByTarget(tenantId, targetType, targetId)
                .flatMap(this::toStatusView);
    }

    public Mono<DocumentGovernanceOverview> organizationOverview(UUID tenantId, UUID organizationId) {
        return Mono.zip(documentGovernancePolicyRepository.findByOrganization(tenantId, organizationId).collectList(),
                        documentLinkRepository.findByOrganization(tenantId, organizationId).collectList(),
                        documentReviewRepository.findByOrganization(tenantId, organizationId).collectList())
                .map(tuple -> {
                    List<DocumentGovernancePolicy> policies = tuple.getT1();
                    List<DocumentLink> links = tuple.getT2();
                    List<DocumentReview> reviews = tuple.getT3();
                    long approvalRequired = policies.stream().filter(DocumentGovernancePolicy::approvalRequired).count();
                    long approved = reviews.stream().filter(review -> "APPROVED".equals(review.reviewStatus())).count();
                    long rejected = reviews.stream().filter(review -> "REJECTED".equals(review.reviewStatus())).count();
                    long expired = reviews.stream().filter(review -> review.expiresAt() != null && review.expiresAt().isBefore(Instant.now())).count();
                    return new DocumentGovernanceOverview(organizationId, policies.size(), links.size(),
                            approvalRequired, approved, rejected, expired,
                            Math.max(0, links.size() - approved - rejected));
                });
    }

    private Mono<DocumentStatusView> toStatusView(DocumentLink link) {
        return Mono.zip(getPolicy(link.tenantId(), link.organizationId(), null, link.targetType(),
                                link.documentCategory()),
                        documentReviewRepository.findByDocumentLinkId(link.tenantId(), link.id()).next()
                                .map(Optional::of)
                                .defaultIfEmpty(Optional.empty()))
                .map(tuple -> {
                    Optional<DocumentReview> review = tuple.getT2();
                    return new DocumentStatusView(link.id(), link.targetType(), link.targetId(),
                            link.documentCategory(), tuple.getT1().mandatory(), tuple.getT1().approvalRequired(),
                            review.map(DocumentReview::reviewStatus).orElse("PENDING"),
                            review.map(DocumentReview::expiresAt).orElse(null));
                });
    }

    public record UpsertDocumentGovernancePolicyCommand(boolean mandatory, boolean approvalRequired,
            Integer expiryDays, String reviewerResponsibilityType) {
    }

    public record DocumentStatusView(UUID documentLinkId, String targetType, UUID targetId, String documentCategory,
            boolean mandatory, boolean approvalRequired, String reviewStatus, Instant expiresAt) {
    }

    public record DocumentGovernanceOverview(UUID organizationId, int policyCount, int documentCount,
            long approvalRequiredPolicyCount, long approvedDocuments, long rejectedDocuments,
            long expiredDocuments, long pendingDocuments) {
    }
}
