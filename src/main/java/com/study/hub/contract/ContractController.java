package com.study.hub.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.study.hub.openapi.ApiPartnerProblemResponse;
import com.study.hub.openapi.ApiProblemDetail;
import com.study.hub.openapi.ApiProblemResponses;
import com.study.hub.openapi.OpenApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hub/api/v1/services/{serviceId}/contracts")
@RequiredArgsConstructor
@Tag(
        name = "Договоры",
        description = "Получение данных договора партнёрского приложения"
)
@ApiProblemResponses
@ApiPartnerProblemResponse
public class ContractController {

    private final ContractService contractService;

    @GetMapping("/{contractNumber}")
    @Operation(summary = "Получить данные договора")
    @ApiResponse(
            responseCode = "200",
            description = "Договор найден",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(type = "object")
            )
    )
    @ApiResponse(
            responseCode = "409",
            description = "Приложение недоступно в текущем статусе",
            content = @Content(
                    mediaType = OpenApiConstants.PROBLEM_JSON,
                    schema = @Schema(implementation = ApiProblemDetail.class)
            )
    )
    public ResponseEntity<JsonNode> getContract(
            @Parameter(description = "Идентификатор платёжного сервиса")
            @PathVariable
            @NotBlank
            @Size(max = 128)
            String serviceId,
            @Parameter(description = "Номер договора")
            @PathVariable
            @Size(max = 128)
            String contractNumber
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(contractService.getContract(serviceId, contractNumber));
    }
}
