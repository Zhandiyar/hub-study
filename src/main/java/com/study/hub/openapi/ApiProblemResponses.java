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
                responseCode = "400",
                description = "Некорректный запрос",
                content = @Content(
                        mediaType = OpenApiConstants.PROBLEM_JSON,
                        schema = @Schema(implementation = ApiProblemDetail.class)
                )
        ),
        @ApiResponse(
                responseCode = "401",
                description = "JWT отсутствует или отклонён внешним Gateway",
                content = @Content(
                        mediaType = OpenApiConstants.PROBLEM_JSON,
                        schema = @Schema(implementation = ApiProblemDetail.class)
                )
        ),
        @ApiResponse(
                responseCode = "404",
                description = "Запрошенный ресурс не найден",
                content = @Content(
                        mediaType = OpenApiConstants.PROBLEM_JSON,
                        schema = @Schema(implementation = ApiProblemDetail.class)
                )
        ),
        @ApiResponse(
                responseCode = "500",
                description = "Непредвиденная ошибка сервера",
                content = @Content(
                        mediaType = OpenApiConstants.PROBLEM_JSON,
                        schema = @Schema(implementation = ApiProblemDetail.class)
                )
        ),
        @ApiResponse(
                responseCode = "503",
                description = "App Registry временно недоступен",
                content = @Content(
                        mediaType = OpenApiConstants.PROBLEM_JSON,
                        schema = @Schema(implementation = ApiProblemDetail.class)
                )
        )
})
public @interface ApiProblemResponses {
}
