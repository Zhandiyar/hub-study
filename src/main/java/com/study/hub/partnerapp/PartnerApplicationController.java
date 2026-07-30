package com.study.hub.partnerapp;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@Validated
@RestController
@RequestMapping("/hub/api/v1")
@RequiredArgsConstructor
@Tag(
        name = "Партнёрские приложения",
        description = "Получение доступной клиенту конфигурации партнёрских приложений"
)
public class PartnerApplicationController {

    private final PartnerApplicationService applicationService;

    @GetMapping("/{appId}")
    @Operation(summary = "Получить конфигурацию партнёрского приложения")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Приложение активно",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PartnerApplicationResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Недопустимый appId",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "JWT отсутствует или недействителен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Приложение не найдено или отключено",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Технические работы или недоступна зависимость",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    public ResponseEntity<PartnerApplicationResponse> getApplication(
            @Parameter(description = "Уникальный идентификатор приложения", example = "insurance-app")
            @PathVariable
            @NotBlank
            @Size(max = 64)
            @Pattern(
                    regexp = "[a-z0-9][a-z0-9._-]*",
                    message = "must contain only lowercase letters, digits, dots, underscores or hyphens"
            )
            String appId,
            Locale locale
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(applicationService.getApplication(appId, locale));
    }
}
