package com.study.hub.order;

import com.study.hub.application.ApplicationAvailabilityService;
import com.study.hub.client.appregistry.dto.ApplicationRoutingResponse;
import com.study.hub.client.appregistry.dto.PaymentFieldMappingResponse;
import com.study.hub.client.appregistry.model.ApplicationStatus;
import com.study.hub.client.partner.PartnerApiClient;
import com.study.hub.client.partner.PartnerOrderResponse;
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
class OrderServiceTest {

    @Mock
    private ApplicationAvailabilityService applicationAvailabilityService;

    @Mock
    private PartnerApiClient partnerApiClient;

    @Test
    void returnsOrderWithReadyPaymentDetails() {
        when(applicationAvailabilityService.getActiveRoutingByAppId("travel-app"))
                .thenReturn(routing(List.of(
                        new PaymentFieldMappingResponse("type", 902L)
                )));
        when(partnerApiClient.getOrder("travel-app", "order-123", "kk-KZ"))
                .thenReturn(new PartnerOrderResponse(
                        "order-123",
                        "CREATED",
                        new BigDecimal("15000.00"),
                        data(),
                        Map.of("type", "flight", "unused", "ignored")
                ));
        var service = new OrderService(applicationAvailabilityService, partnerApiClient);

        OrderResponse result = service.getOrder(
                "travel-app", "order-123", "kk-KZ"
        );

        assertThat(result.orderId()).isEqualTo("order-123");
        assertThat(result.amount()).isEqualByComparingTo("15000.00");
        assertThat(result.status()).isEqualTo("CREATED");
        assertThat(result.data()).isEqualTo(data());
        assertThat(result.payment().serviceId()).isEqualTo(456L);
        assertThat(result.payment().details()).containsExactly(
                new PaymentDetailResponse(901L, "order-123"),
                new PaymentDetailResponse(902L, "flight")
        );
        verify(applicationAvailabilityService).getActiveRoutingByAppId("travel-app");
    }

    @Test
    void returnsOnlyOrderIdDetailWhenRouteHasNoAdditionalMappings() {
        when(applicationAvailabilityService.getActiveRoutingByAppId("travel-app"))
                .thenReturn(routing(List.of()));
        when(partnerApiClient.getOrder("travel-app", "order-123", "ru"))
                .thenReturn(new PartnerOrderResponse(
                        "order-123",
                        "CREATED",
                        new BigDecimal("100.00"),
                        data(),
                        Map.of()
                ));
        var service = new OrderService(applicationAvailabilityService, partnerApiClient);

        OrderResponse result = service.getOrder("travel-app", "order-123", "ru");

        assertThat(result.payment().details()).containsExactly(
                new PaymentDetailResponse(901L, "order-123")
        );
    }

    @Test
    void rejectsOrderWithoutConfiguredPaymentField() {
        when(applicationAvailabilityService.getActiveRoutingByAppId("travel-app"))
                .thenReturn(routing(List.of(
                        new PaymentFieldMappingResponse("type", 902L)
                )));
        when(partnerApiClient.getOrder("travel-app", "order-123", "ru"))
                .thenReturn(new PartnerOrderResponse(
                        "order-123",
                        "CREATED",
                        new BigDecimal("15000.00"),
                        data(),
                        Map.of()
                ));
        var service = new OrderService(applicationAvailabilityService, partnerApiClient);

        assertThatThrownBy(() -> service.getOrder(
                "travel-app", "order-123", "ru"
        ))
                .isInstanceOf(PartnerApiUnavailableException.class);
    }

    @Test
    void doesNotCallPartnerForUnknownApplication() {
        when(applicationAvailabilityService.getActiveRoutingByAppId("missing"))
                .thenThrow(new ApplicationNotFoundException());
        var service = new OrderService(applicationAvailabilityService, partnerApiClient);

        assertThatThrownBy(() -> service.getOrder("missing", "order-123", "ru"))
                .isInstanceOf(ApplicationNotFoundException.class);

        verifyNoInteractions(partnerApiClient);
    }

    @Test
    void doesNotCallPartnerWhenApplicationIsUnavailable() {
        when(applicationAvailabilityService.getActiveRoutingByAppId("travel-app"))
                .thenThrow(new ApplicationUnavailableException());
        var service = new OrderService(applicationAvailabilityService, partnerApiClient);

        assertThatThrownBy(() -> service.getOrder(
                "travel-app", "order-123", "ru"
        ))
                .isInstanceOf(ApplicationUnavailableException.class);

        verifyNoInteractions(partnerApiClient);
    }

    private ApplicationRoutingResponse routing(List<PaymentFieldMappingResponse> mappings) {
        return new ApplicationRoutingResponse(
                "travel-app",
                ApplicationStatus.ACTIVE,
                456L,
                901L,
                mappings
        );
    }

    private Map<String, Object> data() {
        return Map.of("tickets", List.of(Map.of(
                "route", "Алматы — Астана",
                "airport", "ALA — NQZ",
                "date", "2026-11-05"
        )));
    }
}
