package com.study.hub.openapi;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiResponses({
        @ApiResponse(
                responseCode = "422",
                description = "Партнёрский сервис отклонил запрос по бизнес-правилам",
                content = @Content(
                        mediaType = OpenApiConstants.PROBLEM_JSON,
                        schema = @Schema(implementation = ApiProblemDetail.class)
                )
        ),
        @ApiResponse(
                responseCode = "502",
                description = "Партнёрский сервис вернул техническую ошибку или недоступен",
                content = @Content(
                        mediaType = OpenApiConstants.PROBLEM_JSON,
                        schema = @Schema(implementation = ApiProblemDetail.class)
                )
        )
})
public @interface ApiPartnerProblemResponse {
}
