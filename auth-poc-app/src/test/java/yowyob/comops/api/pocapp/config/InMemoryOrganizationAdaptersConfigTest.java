package yowyob.comops.api.pocapp.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class InMemoryOrganizationAdaptersConfigTest {

    private final InMemoryOrganizationAdaptersConfig config = new InMemoryOrganizationAdaptersConfig();

    @Test
    void entitlementDirectoryAlwaysGrantsService() {
        StepVerifier.create(config.inMemoryEntitlementDirectory()
                        .resolveRuntimeEntitlement(UUID.randomUUID(), UUID.randomUUID(), "SALES"))
                .assertNext(e -> {
                    assertThat(e.serviceCode()).isEqualTo("SALES");
                    assertThat(e.effective()).isTrue();
                    assertThat(e.requestQuotaLimit()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void userOrganizationAccessReturnsSeededOrg() {
        StepVerifier.create(config.inMemoryUserOrganizationAccessDirectory()
                        .listUserOrganizations(InMemoryOrganizationAdaptersConfig.DEMO_TENANT_ID, UUID.randomUUID()))
                .assertNext(a -> {
                    assertThat(a.organizationId()).isEqualTo(InMemoryOrganizationAdaptersConfig.DEMO_ORG_ID);
                    assertThat(a.organizationCode()).isEqualTo(InMemoryOrganizationAdaptersConfig.DEMO_ORG_CODE);
                    assertThat(a.services()).isNotEmpty();
                })
                .verifyComplete();
    }

    @Test
    void signUpContextMatchesDemoCodeCaseInsensitive() {
        StepVerifier.create(config.inMemorySignUpContextDirectory()
                        .findByOrganizationCode(InMemoryOrganizationAdaptersConfig.DEMO_ORG_CODE.toLowerCase()))
                .assertNext(c -> assertThat(c.organizationCode())
                        .isEqualTo(InMemoryOrganizationAdaptersConfig.DEMO_ORG_CODE))
                .verifyComplete();
    }

    @Test
    void signUpContextEmptyForUnknownCode() {
        StepVerifier.create(config.inMemorySignUpContextDirectory().findByOrganizationCode("UNKNOWN"))
                .verifyComplete();
    }
}
