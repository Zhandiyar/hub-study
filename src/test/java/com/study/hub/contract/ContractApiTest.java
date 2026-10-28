package com.study.hub.contract;

import com.study.hub.exception.ContractNotFoundException;
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
class ContractApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContractService contractService;

    @Test
    void returnsContractWithoutCaching() throws Exception {
        var response = new ContractResponse(
                "contract-123",
                "ACTIVE",
                new BigDecimal("15000.00"),
                "KZT",
                List.of(
                        new PaymentDetailResponse(901L, "contract-123"),
                        new PaymentDetailResponse(902L, "flight")
                )
        );
        when(contractService.getContract(456L, "contract-123"))
                .thenReturn(response);

        mockMvc.perform(get(
                        "/hub/api/v1/services/456/contracts/contract-123"
                ))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.contractNumber").value("contract-123"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.amount").value(15000.00))
                .andExpect(jsonPath("$.currency").value("KZT"))
                .andExpect(jsonPath("$.paymentDetails[0].serviceFieldId").value(901))
                .andExpect(jsonPath("$.paymentDetails[0].value").value("contract-123"))
                .andExpect(jsonPath("$.paymentDetails[1].serviceFieldId").value(902))
                .andExpect(jsonPath("$.paymentDetails[1].value").value("flight"));

        verify(contractService).getContract(456L, "contract-123");
    }

    @Test
    void returnsMissingContractAsNotFound() throws Exception {
        when(contractService.getContract(456L, "missing"))
                .thenThrow(new ContractNotFoundException());

        mockMvc.perform(get(
                        "/hub/api/v1/services/456/contracts/missing"
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONTRACT_NOT_FOUND"));
    }

    @Test
    void returnsApplicationNotFoundForUnknownServiceId() throws Exception {
        when(contractService.getContract(999L, "contract-123"))
                .thenThrow(new ApplicationNotFoundException());

        mockMvc.perform(get(
                        "/hub/api/v1/services/999/contracts/contract-123"
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("APPLICATION_NOT_FOUND"));
    }

    @Test
    void returnsConflictWhenApplicationIsUnavailable() throws Exception {
        when(contractService.getContract(456L, "contract-123"))
                .thenThrow(new ApplicationUnavailableException());

        mockMvc.perform(get(
                        "/hub/api/v1/services/456/contracts/contract-123"
                ))
                .andExpect(status().isConflict())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value("APPLICATION_UNAVAILABLE"));
    }

    @Test
    void hidesPartnerFailureBehindBadGateway() throws Exception {
        when(contractService.getContract(456L, "contract-123"))
                .thenThrow(new PartnerApiUnavailableException());

        mockMvc.perform(get(
                        "/hub/api/v1/services/456/contracts/contract-123"
                ))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("PARTNER_API_UNAVAILABLE"))
                .andExpect(jsonPath("$.detail").value(
                        "Partner service returned an invalid response or is temporarily unavailable"
                ));
    }

    @Test
    void returnsPartnerBusinessErrorAsUnprocessableEntity() throws Exception {
        when(contractService.getContract(456L, "contract-123"))
                .thenThrow(new PartnerBusinessException(
                        "Contract amount is invalid",
                        "CONTRACT_DIFFERENT_AMOUNT",
                        Map.of("contract", List.of("Contract amount is invalid"))
                ));

        mockMvc.perform(get(
                        "/hub/api/v1/services/456/contracts/contract-123"
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
