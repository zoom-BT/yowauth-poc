package yowyob.comops.api.auth.adapter.out.persistence;

import yowyob.comops.api.auth.application.port.out.UserAccountRepository;
import yowyob.comops.api.auth.domain.model.UserAccount;
import reactor.core.publisher.Flux;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class UserAccountR2dbcRepositoryAdapter implements UserAccountRepository {

    private final UserAccountSpringDataRepository repository;

    public UserAccountR2dbcRepositoryAdapter(UserAccountSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Boolean> existsByUsername(java.util.UUID tenantId, String username) {
        return repository.existsByTenantIdAndUsernameIgnoreCase(tenantId, username);
    }

    @Override
    public Mono<UserAccount> findById(java.util.UUID tenantId, java.util.UUID userId) {
        return repository.findByIdAndTenantId(userId, tenantId).map(this::toDomain);
    }

    @Override
    public Mono<UserAccount> findByPrincipal(java.util.UUID tenantId, String principal) {
        return repository.findByTenantIdAndUsernameIgnoreCase(tenantId, principal)
                .switchIfEmpty(repository.findByTenantIdAndEmailIgnoreCase(tenantId, principal))
                .switchIfEmpty(repository.findByTenantIdAndPhoneNumber(tenantId, principal))
                .map(this::toDomain);
    }

    @Override
    public Flux<UserAccount> findAllByPrincipal(String principal) {
        return repository.findAllByUsernameIgnoreCase(principal)
                .concatWith(repository.findAllByEmailIgnoreCase(principal))
                .concatWith(repository.findAllByPhoneNumber(principal))
                .distinct(UserAccountEntity::id)
                .map(this::toDomain);
    }

    @Override
    public Mono<UserAccount> save(UserAccount userAccount) {
        return repository.save(toEntity(userAccount)).map(this::toDomain);
    }

    private UserAccountEntity toEntity(UserAccount userAccount) {
        return new UserAccountEntity(userAccount.id(), userAccount.tenantId(), userAccount.createdAt(),
                userAccount.updatedAt(), userAccount.actorId(), userAccount.username(), userAccount.email(),
                userAccount.phoneNumber(), userAccount.passwordHash(), userAccount.authProvider(),
                userAccount.externalSubject(), userAccount.status(), userAccount.plan(), userAccount.onboardingStatus(),
                userAccount.onboardingStep(), userAccount.accountType(), userAccount.businessType(),
                userAccount.onboardingPayload(), userAccount.emailVerifiedAt(), userAccount.phoneVerifiedAt(),
                userAccount.mfaEnabled(), userAccount.mfaChannel());
    }

    private UserAccount toDomain(UserAccountEntity entity) {
        return UserAccount.rehydrate(entity.id(), entity.tenantId(), entity.createdAt(), entity.updatedAt(),
                entity.actorId(), entity.username(), entity.email(), entity.phoneNumber(), entity.passwordHash(),
                entity.authProvider(), entity.externalSubject(), entity.status(), entity.plan(),
                entity.onboardingStatus(), entity.onboardingStep(), entity.accountType(), entity.businessType(),
                entity.onboardingPayload(), entity.emailVerifiedAt(), entity.phoneVerifiedAt(), entity.mfaEnabled(),
                entity.mfaChannel());
    }
}
