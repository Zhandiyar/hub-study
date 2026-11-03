package com.study.hub.order;

import com.study.hub.application.ApplicationAvailabilityService;
import com.study.hub.client.appregistry.dto.PaymentFieldMappingResponse;
import com.study.hub.client.partner.PartnerApiClient;
import com.study.hub.client.partner.PartnerOrderResponse;
import com.study.hub.exception.PartnerApiUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final ApplicationAvailabilityService applicationAvailabilityService;
    private final PartnerApiClient partnerApiClient;

    public OrderResponse getOrder(
            String appId,
            String orderId,
            String language
    ) {
        var routing = applicationAvailabilityService
                .getActiveRoutingByAppId(appId);
        PartnerOrderResponse order = partnerApiClient.getOrder(
                routing.appId(),
                orderId,
                language
        );

        List<PaymentDetailResponse> paymentDetails = new ArrayList<>();
        paymentDetails.add(new PaymentDetailResponse(
                routing.contractNumberServiceFieldId(),
                order.orderId()
        ));
        for (PaymentFieldMappingResponse mapping : routing.paymentFieldMappings()) {
            String value = order.paymentFields().get(mapping.code());
            if (value == null || value.isBlank()) {
                log.warn(
                        "Partner order response misses configured payment field: "
                                + "appId={} fieldCode={}",
                        routing.appId(),
                        mapping.code()
                );
                throw new PartnerApiUnavailableException();
            }
            paymentDetails.add(new PaymentDetailResponse(mapping.serviceFieldId(), value));
        }

        return toResponse(routing.serviceId(), order, paymentDetails);
    }

    private OrderResponse toResponse(
            Long serviceId,
            PartnerOrderResponse order,
            List<PaymentDetailResponse> paymentDetails
    ) {
        return new OrderResponse(
                order.orderId(),
                order.status(),
                order.amount(),
                order.data(),
                new OrderPaymentResponse(serviceId, paymentDetails)
        );
    }
}
