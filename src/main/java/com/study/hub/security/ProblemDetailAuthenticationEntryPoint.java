package com.study.hub.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.hub.web.ProblemDetailFactory;
import com.study.hub.web.ProblemType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final BearerTokenAuthenticationEntryPoint bearerEntryPoint =
            new BearerTokenAuthenticationEntryPoint();
    private final ObjectMapper objectMapper;
    private final ProblemDetailFactory problemDetailFactory;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        bearerEntryPoint.commence(request, response, exception);

        var problem = problemDetailFactory.create(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "Authentication is required",
                ProblemType.UNAUTHORIZED,
                request.getRequestURI()
        );

        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
