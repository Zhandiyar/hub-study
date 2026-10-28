package com.study.hub.contract;

import com.study.hub.application.ApplicationAvailabilityService;
import com.study.hub.client.appregistry.dto.PaymentFieldMappingResponse;
import com.study.hub.client.partner.PartnerApiClient;
import com.study.hub.client.partner.PartnerContractResponse;
import com.study.hub.exception.PartnerApiUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractService {

    private final ApplicationAvailabilityService applicationAvailabilityService;
    private final PartnerApiClient partnerApiClient;

    public ContractResponse getContract(Long serviceId, String contractNumber) {
        var routing = applicationAvailabilityService
                .getActiveRoutingByServiceId(serviceId);
        PartnerContractResponse contract = partnerApiClient.getContract(
                routing.appId(),
                contractNumber
        );

        List<PaymentDetailResponse> paymentDetails = new ArrayList<>();
        paymentDetails.add(new PaymentDetailResponse(
                routing.contractNumberServiceFieldId(),
                contract.contractNumber()
        ));
        for (PaymentFieldMappingResponse mapping : routing.paymentFieldMappings()) {
            String value = contract.paymentFields().get(mapping.code());
            if (value == null || value.isBlank()) {
                log.warn(
                        "Partner contract response misses configured payment field: "
                                + "appId={} fieldCode={}",
                        routing.appId(),
                        mapping.code()
                );
                throw new PartnerApiUnavailableException();
            }
            paymentDetails.add(new PaymentDetailResponse(mapping.serviceFieldId(), value));
        }

        return toResponse(contract, paymentDetails);
    }

    private ContractResponse toResponse(
            PartnerContractResponse contract,
            List<PaymentDetailResponse> paymentDetails
    ) {
        return new ContractResponse(
                contract.contractNumber(),
                contract.status(),
                contract.amount(),
                contract.currency(),
                paymentDetails
        );
    }
}
