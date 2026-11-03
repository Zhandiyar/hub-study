package com.study.hub.order;

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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hub/api/v1/applications/{appId}/orders")
@RequiredArgsConstructor
@Tag(
        name = "Заказы",
        description = "Получение данных заказа партнёрского приложения"
)
@ApiProblemResponses
@ApiPartnerProblemResponse
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/{orderId}")
    @Operation(summary = "Получить данные заказа")
    @ApiResponse(
            responseCode = "200",
            description = "Заказ найден",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = OrderResponse.class)
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
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(description = "Уникальный идентификатор приложения")
            @PathVariable
            @NotBlank
            @Size(max = 128)
            String appId,
            @Parameter(description = "Идентификатор заказа у партнёра")
            @PathVariable
            @NotBlank
            @Size(max = 128)
            String orderId,
            @Parameter(description = "Предпочитаемый язык ответа", example = "kk-KZ")
            @RequestHeader(
                    name = HttpHeaders.ACCEPT_LANGUAGE,
                    defaultValue = "ru"
            )
            String language
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(orderService.getOrder(appId, orderId, language));
    }
}
