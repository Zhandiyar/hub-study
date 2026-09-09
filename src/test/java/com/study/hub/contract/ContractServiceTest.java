package com.study.hub.contract;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.study.hub.application.ApplicationAvailabilityService;
import com.study.hub.client.partner.PartnerApiClient;
import com.study.hub.exception.ApplicationNotFoundException;
import com.study.hub.exception.ApplicationUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    private ApplicationAvailabilityService applicationAvailabilityService;

    @Mock
    private PartnerApiClient partnerApiClient;

    @Test
    void returnsContractForRegisteredApplication() {
        var expected = JsonNodeFactory.instance.objectNode()
                .put("policyNumber", "contract-123")
                .put("state", "ACTIVE");
        when(partnerApiClient.getContract("insurance-app", "contract-123"))
                .thenReturn(expected);
        var service = new ContractService(applicationAvailabilityService, partnerApiClient);

        var result = service.getContract("insurance-app", "contract-123");

        assertThat(result).isSameAs(expected);
        verify(applicationAvailabilityService).getActiveConfiguration("insurance-app");
    }

    @Test
    void doesNotCallPartnerForUnknownApplication() {
        when(applicationAvailabilityService.getActiveConfiguration("unknown-app"))
                .thenThrow(new ApplicationNotFoundException());
        var service = new ContractService(applicationAvailabilityService, partnerApiClient);

        assertThatThrownBy(() -> service.getContract("unknown-app", "contract-123"))
                .isInstanceOf(ApplicationNotFoundException.class);

        verifyNoInteractions(partnerApiClient);
    }

    @Test
    void doesNotCallPartnerWhenApplicationIsUnavailable() {
        when(applicationAvailabilityService.getActiveConfiguration("insurance-app"))
                .thenThrow(new ApplicationUnavailableException());
        var service = new ContractService(applicationAvailabilityService, partnerApiClient);

        assertThatThrownBy(() -> service.getContract("insurance-app", "contract-123"))
                .isInstanceOf(ApplicationUnavailableException.class);

        verifyNoInteractions(partnerApiClient);
    }
}
