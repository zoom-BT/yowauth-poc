package yowyob.comops.api.actor.application.service;

import yowyob.comops.api.actor.application.port.in.GetCurrentBusinessActorUseCase;
import yowyob.comops.api.actor.application.port.in.OnboardBusinessActorCommand;
import yowyob.comops.api.actor.application.port.in.OnboardBusinessActorUseCase;
import yowyob.comops.api.actor.application.port.in.ReactivateMyBusinessActorCommand;
import yowyob.comops.api.actor.application.port.in.ReactivateMyBusinessActorUseCase;
import yowyob.comops.api.actor.application.port.in.UpdateBusinessActorCommand;
import yowyob.comops.api.actor.application.port.in.UpdateBusinessActorUseCase;
import yowyob.comops.api.actor.application.port.out.BusinessActorApprovalPolicy;
import yowyob.comops.api.actor.application.port.out.BusinessActorProfileRepository;
import yowyob.comops.api.actor.application.port.out.BusinessActorSelfReactivationPolicy;
import yowyob.comops.api.actor.application.port.out.UserAccountDirectory;
import yowyob.comops.api.actor.domain.BusinessActorAlreadyExistsException;
import yowyob.comops.api.actor.domain.BusinessActorNotFoundException;
import yowyob.comops.api.actor.domain.BusinessActorSelfReactivationDisabledException;
import yowyob.comops.api.actor.domain.BusinessActorSelfReactivationNotAllowedException;
import yowyob.comops.api.actor.domain.UserAccountLinkNotFoundException;
import yowyob.comops.api.actor.domain.model.BusinessActorGovernanceStatus;
import yowyob.comops.api.actor.domain.model.BusinessActorProfile;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class BusinessActorApplicationService
        implements OnboardBusinessActorUseCase, GetCurrentBusinessActorUseCase, UpdateBusinessActorUseCase,
        ReactivateMyBusinessActorUseCase {

    private final BusinessActorProfileRepository businessActorProfileRepository;
    private final UserAccountDirectory userAccountDirectory;
    private final Optional<BusinessActorApprovalPolicy> businessActorApprovalPolicy;
    private final Optional<BusinessActorSelfReactivationPolicy> businessActorSelfReactivationPolicy;

    public BusinessActorApplicationService(BusinessActorProfileRepository businessActorProfileRepository,
            UserAccountDirectory userAccountDirectory,
            Optional<BusinessActorApprovalPolicy> businessActorApprovalPolicy,
            Optional<BusinessActorSelfReactivationPolicy> businessActorSelfReactivationPolicy) {
        this.businessActorProfileRepository = businessActorProfileRepository;
        this.userAccountDirectory = userAccountDirectory;
        this.businessActorApprovalPolicy = businessActorApprovalPolicy;
        this.businessActorSelfReactivationPolicy = businessActorSelfReactivationPolicy;
    }

    @Override
    public Mono<BusinessActorProfile> onboard(OnboardBusinessActorCommand command) {
        Objects.requireNonNull(command, "command is required");
        return resolveActorId(command.tenantId(), command.userId())
                .flatMap(actorId -> businessActorProfileRepository.findByActorId(command.tenantId(), actorId)
                        .flatMap(existing -> Mono.<BusinessActorProfile>error(new BusinessActorAlreadyExistsException(actorId)))
                        .switchIfEmpty(Mono.defer(() -> applyApprovalPolicy(command.tenantId(),
                                BusinessActorProfile.create(command.tenantId(), actorId, command.code(),
                                        command.isIndividual(), command.isAvailable(), command.isVerified(),
                                        command.isActive(), command.type(), command.role(), command.qualifications(),
                                        command.paymentMethods(), command.addresses(), command.biography(),
                                        command.name(), command.businessId(), command.niu(),
                                        command.tradeRegistryNumber(), command.website(), command.contactPhone(),
                                        command.privateAddress(), command.businessAddress(),
                                        command.businessProfile()))
                                .flatMap(businessActorProfileRepository::save))));
    }

    @Override
    public Mono<BusinessActorProfile> getByUser(UUID tenantId, UUID userId) {
        return resolveActorId(tenantId, userId)
                .flatMap(actorId -> businessActorProfileRepository.findByActorId(tenantId, actorId)
                        .switchIfEmpty(Mono.error(new BusinessActorNotFoundException(actorId))));
    }

    @Override
    public Mono<BusinessActorProfile> update(UpdateBusinessActorCommand command) {
        Objects.requireNonNull(command, "command is required");
        return resolveActorId(command.tenantId(), command.userId())
                .flatMap(actorId -> businessActorProfileRepository.findByActorId(command.tenantId(), actorId)
                        .switchIfEmpty(Mono.error(new BusinessActorNotFoundException(actorId)))
                        .map(existing -> existing.update(command.code(), command.isIndividual(), command.isAvailable(),
                                command.isVerified(), command.isActive(), command.type(), command.role(),
                                command.qualifications(), command.paymentMethods(), command.addresses(),
                                command.biography(), command.name(), command.businessId(), command.niu(),
                                command.tradeRegistryNumber(), command.website(), command.contactPhone(),
                                command.privateAddress(), command.businessAddress(), command.businessProfile()))
                        .flatMap(businessActorProfileRepository::save));
    }

    @Override
    public Mono<BusinessActorProfile> reactivate(ReactivateMyBusinessActorCommand command) {
        Objects.requireNonNull(command, "command is required");
        return resolveActorId(command.tenantId(), command.userId())
                .flatMap(actorId -> businessActorProfileRepository.findByActorId(command.tenantId(), actorId)
                        .switchIfEmpty(Mono.error(new BusinessActorNotFoundException(actorId)))
                        .flatMap(existing -> ensureSelfReactivationAllowed(command.tenantId(), existing))
                        .map(existing -> existing.reactivate(null, "self-reactivated by platform option"))
                        .flatMap(businessActorProfileRepository::save));
    }

    private Mono<UUID> resolveActorId(UUID tenantId, UUID userId) {
        return userAccountDirectory.findByUserId(tenantId, userId)
                .filter(link -> link.actorId() != null)
                .map(UserAccountDirectory.UserAccountLink::actorId)
                .switchIfEmpty(Mono.error(new UserAccountLinkNotFoundException(userId)));
    }

    private Mono<BusinessActorProfile> applyApprovalPolicy(UUID tenantId, BusinessActorProfile profile) {
        return businessActorApprovalPolicy
                .map(policy -> policy.requiresApproval(tenantId)
                        .map(requiresApproval -> requiresApproval ? profile : profile.approve(null, "auto-approved by platform options")))
                .orElseGet(() -> Mono.just(profile));
    }

    private Mono<BusinessActorProfile> ensureSelfReactivationAllowed(UUID tenantId, BusinessActorProfile profile) {
        return businessActorSelfReactivationPolicy
                .map(policy -> policy.isSelfReactivationAllowed(tenantId)
                        .flatMap(isAllowed -> {
                            if (!isAllowed) {
                                return Mono.error(new BusinessActorSelfReactivationDisabledException());
                            }
                            if (profile.governanceStatus() == BusinessActorGovernanceStatus.BLOCKED
                                    || profile.governanceStatus() == BusinessActorGovernanceStatus.APPROVED
                                    || profile.governanceStatus() == BusinessActorGovernanceStatus.PENDING_REVIEW) {
                                return Mono.error(new BusinessActorSelfReactivationNotAllowedException(profile.id(),
                                        profile.governanceStatus().name()));
                            }
                            return Mono.just(profile);
                        }))
                .orElseGet(() -> Mono.error(new BusinessActorSelfReactivationDisabledException()));
    }
}
