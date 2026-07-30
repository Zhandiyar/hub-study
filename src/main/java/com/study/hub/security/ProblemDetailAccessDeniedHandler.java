package com.study.hub.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.hub.web.ProblemDetailFactory;
import com.study.hub.web.ProblemType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

    private final BearerTokenAccessDeniedHandler bearerAccessDeniedHandler =
            new BearerTokenAccessDeniedHandler();
    private final ObjectMapper objectMapper;
    private final ProblemDetailFactory problemDetailFactory;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception
    ) throws IOException {
        bearerAccessDeniedHandler.handle(request, response, exception);

        var problem = problemDetailFactory.create(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                "Insufficient permissions",
                ProblemType.FORBIDDEN,
                request.getRequestURI()
        );

        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
