package com.study.hub.application;

import com.study.hub.client.appregistry.AppRegistryClient;
import com.study.hub.openapi.ApiProblemResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hub/api/v1")
@RequiredArgsConstructor
@Tag(
        name = "Партнёрские приложения",
        description = "Получение доступной клиенту конфигурации партнёрских приложений"
)
@ApiProblemResponses
public class ApplicationConfigurationController {

    private final AppRegistryClient appRegistryClient;

    @GetMapping(
            value = "/applications/{appId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Получить конфигурацию партнёрского приложения")
    @ApiResponse(
            responseCode = "200",
            description = "Приложение найдено; состояние передано в поле status",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(
                            implementation = ApplicationConfigurationResponse.class
                    )
            )
    )
    public ResponseEntity<ApplicationConfigurationResponse> getApplication(
            @Parameter(description = "Уникальный идентификатор приложения", example = "insurance-app")
            @PathVariable
            String appId
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApplicationConfigurationResponse.from(
                        appRegistryClient.getApplicationConfiguration(appId)
                ));
    }

}
