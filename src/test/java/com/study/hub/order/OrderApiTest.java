package com.study.hub.order;

import com.study.hub.exception.OrderNotFoundException;
import com.study.hub.exception.ApplicationNotFoundException;
import com.study.hub.exception.ApplicationUnavailableException;
import com.study.hub.exception.PartnerApiUnavailableException;
import com.study.hub.exception.PartnerBusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void returnsOrderWithoutCaching() throws Exception {
        var response = new OrderResponse(
                "order-123",
                "CREATED",
                new BigDecimal("15000.00"),
                Map.of("tickets", List.of(Map.of(
                        "route", "Алматы — Астана",
                        "airport", "ALA — NQZ",
                        "date", "2026-11-05"
                ))),
                new OrderPaymentResponse(
                        456L,
                        List.of(
                                new PaymentDetailResponse(901L, "order-123"),
                                new PaymentDetailResponse(902L, "flight")
                        )
                )
        );
        when(orderService.getOrder("travel-app", "order-123", "kk-KZ"))
                .thenReturn(response);

        mockMvc.perform(get(
                        "/hub/api/v1/applications/travel-app/orders/order-123"
                ).header("Accept-Language", "kk-KZ"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.orderId").value("order-123"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.amount").value(15000.00))
                .andExpect(jsonPath("$.data.tickets[0].route").value("Алматы — Астана"))
                .andExpect(jsonPath("$.data.tickets[0].airport").value("ALA — NQZ"))
                .andExpect(jsonPath("$.payment.serviceId").value(456))
                .andExpect(jsonPath("$.payment.details[0].serviceFieldId").value(901))
                .andExpect(jsonPath("$.payment.details[0].value").value("order-123"))
                .andExpect(jsonPath("$.payment.details[1].serviceFieldId").value(902))
                .andExpect(jsonPath("$.payment.details[1].value").value("flight"));

        verify(orderService).getOrder("travel-app", "order-123", "kk-KZ");
    }

    @Test
    void returnsMissingOrderAsNotFound() throws Exception {
        when(orderService.getOrder("travel-app", "missing", "ru"))
                .thenThrow(new OrderNotFoundException());

        mockMvc.perform(get(
                        "/hub/api/v1/applications/travel-app/orders/missing"
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void returnsApplicationNotFoundForUnknownAppId() throws Exception {
        when(orderService.getOrder("missing", "order-123", "ru"))
                .thenThrow(new ApplicationNotFoundException());

        mockMvc.perform(get(
                        "/hub/api/v1/applications/missing/orders/order-123"
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("APPLICATION_NOT_FOUND"));
    }

    @Test
    void returnsConflictWhenApplicationIsUnavailable() throws Exception {
        when(orderService.getOrder("travel-app", "order-123", "ru"))
                .thenThrow(new ApplicationUnavailableException());

        mockMvc.perform(get(
                        "/hub/api/v1/applications/travel-app/orders/order-123"
                ))
                .andExpect(status().isConflict())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value("APPLICATION_UNAVAILABLE"));
    }

    @Test
    void hidesPartnerFailureBehindBadGateway() throws Exception {
        when(orderService.getOrder("travel-app", "order-123", "ru"))
                .thenThrow(new PartnerApiUnavailableException());

        mockMvc.perform(get(
                        "/hub/api/v1/applications/travel-app/orders/order-123"
                ))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("PARTNER_API_UNAVAILABLE"))
                .andExpect(jsonPath("$.detail").value(
                        "Partner service returned an invalid response or is temporarily unavailable"
                ));
    }

    @Test
    void returnsPartnerBusinessErrorAsUnprocessableEntity() throws Exception {
        when(orderService.getOrder("travel-app", "order-123", "ru"))
                .thenThrow(new PartnerBusinessException(
                        "Contract amount is invalid",
                        "CONTRACT_DIFFERENT_AMOUNT",
                        Map.of("contract", List.of("Contract amount is invalid"))
                ));

        mockMvc.perform(get(
                        "/hub/api/v1/applications/travel-app/orders/order-123"
                ))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value("PARTNER_BUSINESS_ERROR"))
                .andExpect(jsonPath("$.errorCode").value("CONTRACT_DIFFERENT_AMOUNT"))
                .andExpect(jsonPath("$.detail").value("Contract amount is invalid"))
                .andExpect(jsonPath("$.errors.contract[0]")
                        .value("Contract amount is invalid"));
    }

}
