package com.study.hub.launch;

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
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hub/api/v1/applications/{appId}/launch")
@RequiredArgsConstructor
@Tag(
        name = "Запуск приложения",
        description = "Подготовка URL и партнёрской сессии для запуска приложения"
)
@ApiProblemResponses
@ApiPartnerProblemResponse
public class LaunchController {

    private final LaunchService launchService;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Подготовить приложение к запуску")
    @ApiResponse(
            responseCode = "200",
            description = "Данные для запуска подготовлены",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = LaunchResponse.class)
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
    public ResponseEntity<LaunchResponse> launch(
            @Parameter(description = "Идентификатор приложения", example = "insurance-app")
            @PathVariable String appId
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(launchService.launch(appId));
    }
}
