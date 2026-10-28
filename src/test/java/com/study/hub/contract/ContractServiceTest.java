package com.study.hub.contract;

import com.study.hub.application.ApplicationAvailabilityService;
import com.study.hub.client.appregistry.dto.ApplicationRoutingResponse;
import com.study.hub.client.appregistry.dto.PaymentFieldMappingResponse;
import com.study.hub.client.appregistry.model.ApplicationStatus;
import com.study.hub.client.partner.PartnerApiClient;
import com.study.hub.client.partner.PartnerContractResponse;
import com.study.hub.exception.ApplicationNotFoundException;
import com.study.hub.exception.ApplicationUnavailableException;
import com.study.hub.exception.PartnerApiUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
    void returnsContractWithReadyPaymentDetails() {
        when(applicationAvailabilityService.getActiveRoutingByServiceId(456L))
                .thenReturn(routing(List.of(
                        new PaymentFieldMappingResponse("type", 902L)
                )));
        when(partnerApiClient.getContract("travel-app", "contract-123"))
                .thenReturn(new PartnerContractResponse(
                        "contract-123",
                        "ACTIVE",
                        new BigDecimal("15000.00"),
                        "KZT",
                        Map.of("type", "flight", "unused", "ignored")
                ));
        var service = new ContractService(applicationAvailabilityService, partnerApiClient);

        ContractResponse result = service.getContract(456L, "contract-123");

        assertThat(result.contractNumber()).isEqualTo("contract-123");
        assertThat(result.amount()).isEqualByComparingTo("15000.00");
        assertThat(result.currency()).isEqualTo("KZT");
        assertThat(result.paymentDetails()).containsExactly(
                new PaymentDetailResponse(901L, "contract-123"),
                new PaymentDetailResponse(902L, "flight")
        );
        verify(applicationAvailabilityService).getActiveRoutingByServiceId(456L);
    }

    @Test
    void returnsOnlyContractNumberDetailWhenRouteHasNoAdditionalMappings() {
        when(applicationAvailabilityService.getActiveRoutingByServiceId(456L))
                .thenReturn(routing(List.of()));
        when(partnerApiClient.getContract("travel-app", "contract-123"))
                .thenReturn(new PartnerContractResponse(
                        "contract-123",
                        "ACTIVE",
                        new BigDecimal("100.00"),
                        "KZT",
                        Map.of()
                ));
        var service = new ContractService(applicationAvailabilityService, partnerApiClient);

        ContractResponse result = service.getContract(456L, "contract-123");

        assertThat(result.paymentDetails()).containsExactly(
                new PaymentDetailResponse(901L, "contract-123")
        );
    }

    @Test
    void rejectsContractWithoutConfiguredPaymentField() {
        when(applicationAvailabilityService.getActiveRoutingByServiceId(456L))
                .thenReturn(routing(List.of(
                        new PaymentFieldMappingResponse("type", 902L)
                )));
        when(partnerApiClient.getContract("travel-app", "contract-123"))
                .thenReturn(new PartnerContractResponse(
                        "contract-123",
                        "ACTIVE",
                        new BigDecimal("15000.00"),
                        "KZT",
                        Map.of()
                ));
        var service = new ContractService(applicationAvailabilityService, partnerApiClient);

        assertThatThrownBy(() -> service.getContract(456L, "contract-123"))
                .isInstanceOf(PartnerApiUnavailableException.class);
    }

    @Test
    void doesNotCallPartnerForUnknownApplication() {
        when(applicationAvailabilityService.getActiveRoutingByServiceId(999L))
                .thenThrow(new ApplicationNotFoundException());
        var service = new ContractService(applicationAvailabilityService, partnerApiClient);

        assertThatThrownBy(() -> service.getContract(999L, "contract-123"))
                .isInstanceOf(ApplicationNotFoundException.class);

        verifyNoInteractions(partnerApiClient);
    }

    @Test
    void doesNotCallPartnerWhenApplicationIsUnavailable() {
        when(applicationAvailabilityService.getActiveRoutingByServiceId(456L))
                .thenThrow(new ApplicationUnavailableException());
        var service = new ContractService(applicationAvailabilityService, partnerApiClient);

        assertThatThrownBy(() -> service.getContract(456L, "contract-123"))
                .isInstanceOf(ApplicationUnavailableException.class);

        verifyNoInteractions(partnerApiClient);
    }

    private ApplicationRoutingResponse routing(List<PaymentFieldMappingResponse> mappings) {
        return new ApplicationRoutingResponse(
                456L,
                "travel",
                "travel-app",
                ApplicationStatus.ACTIVE,
                901L,
                mappings
        );
    }
}
