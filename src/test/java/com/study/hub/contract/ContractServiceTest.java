package com.study.hub.contract;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.study.hub.application.ApplicationAvailabilityService;
import com.study.hub.client.partner.PartnerApiClient;
import com.study.hub.client.appregistry.dto.ApplicationRoutingResponse;
import com.study.hub.client.appregistry.model.ApplicationStatus;
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
        when(applicationAvailabilityService.getActiveRoutingByServiceId("service-456"))
                .thenReturn(new ApplicationRoutingResponse(
                        "insurance",
                        "insurance-app",
                        ApplicationStatus.ACTIVE
                ));
        when(partnerApiClient.getContract("insurance-app", "contract-123"))
                .thenReturn(expected);
        var service = new ContractService(applicationAvailabilityService, partnerApiClient);

        var result = service.getContract("service-456", "contract-123");

        assertThat(result).isSameAs(expected);
        verify(applicationAvailabilityService)
                .getActiveRoutingByServiceId("service-456");
    }

    @Test
    void doesNotCallPartnerForUnknownApplication() {
        when(applicationAvailabilityService.getActiveRoutingByServiceId("unknown-service"))
                .thenThrow(new ApplicationNotFoundException());
        var service = new ContractService(applicationAvailabilityService, partnerApiClient);

        assertThatThrownBy(() -> service.getContract("unknown-service", "contract-123"))
                .isInstanceOf(ApplicationNotFoundException.class);

        verifyNoInteractions(partnerApiClient);
    }

    @Test
    void doesNotCallPartnerWhenApplicationIsUnavailable() {
        when(applicationAvailabilityService.getActiveRoutingByServiceId("service-456"))
                .thenThrow(new ApplicationUnavailableException());
        var service = new ContractService(applicationAvailabilityService, partnerApiClient);

        assertThatThrownBy(() -> service.getContract("service-456", "contract-123"))
                .isInstanceOf(ApplicationUnavailableException.class);

        verifyNoInteractions(partnerApiClient);
    }
}
