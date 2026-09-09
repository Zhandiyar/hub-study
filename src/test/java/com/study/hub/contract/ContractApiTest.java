package com.study.hub.contract;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.study.hub.exception.ContractNotFoundException;
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
        var response = JsonNodeFactory.instance.objectNode()
                .put("policyNumber", "contract-123")
                .put("state", "ACTIVE")
                .put("insuredObject", "vehicle");
        when(contractService.getContract("insurance-app", "contract-123"))
                .thenReturn(response);

        mockMvc.perform(get(
                        "/hub/api/v1/applications/insurance-app/contracts/contract-123"
                ))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.policyNumber").value("contract-123"))
                .andExpect(jsonPath("$.state").value("ACTIVE"))
                .andExpect(jsonPath("$.insuredObject").value("vehicle"));

        verify(contractService).getContract("insurance-app", "contract-123");
    }

    @Test
    void returnsMissingContractAsNotFound() throws Exception {
        when(contractService.getContract("insurance-app", "missing"))
                .thenThrow(new ContractNotFoundException());

        mockMvc.perform(get(
                        "/hub/api/v1/applications/insurance-app/contracts/missing"
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONTRACT_NOT_FOUND"));
    }

    @Test
    void returnsConflictWhenApplicationIsUnavailable() throws Exception {
        when(contractService.getContract("insurance-app", "contract-123"))
                .thenThrow(new ApplicationUnavailableException());

        mockMvc.perform(get(
                        "/hub/api/v1/applications/insurance-app/contracts/contract-123"
                ))
                .andExpect(status().isConflict())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value("APPLICATION_UNAVAILABLE"));
    }

    @Test
    void hidesPartnerFailureBehindBadGateway() throws Exception {
        when(contractService.getContract("insurance-app", "contract-123"))
                .thenThrow(new PartnerApiUnavailableException());

        mockMvc.perform(get(
                        "/hub/api/v1/applications/insurance-app/contracts/contract-123"
                ))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("PARTNER_API_UNAVAILABLE"))
                .andExpect(jsonPath("$.detail").value(
                        "Partner service returned an invalid response or is temporarily unavailable"
                ));
    }

    @Test
    void returnsPartnerBusinessErrorAsUnprocessableEntity() throws Exception {
        when(contractService.getContract("insurance-app", "contract-123"))
                .thenThrow(new PartnerBusinessException(
                        "Contract amount is invalid",
                        "CONTRACT_DIFFERENT_AMOUNT",
                        Map.of("contract", List.of("Contract amount is invalid"))
                ));

        mockMvc.perform(get(
                        "/hub/api/v1/applications/insurance-app/contracts/contract-123"
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
