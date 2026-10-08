package com.nexuscomply.common.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter implements Filter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_ATTRIBUTE = "requestId";

    private static final ThreadLocal<String> CURRENT_REQUEST_ID = new ThreadLocal<>();

    public static String getRequestId() {
        String id = CURRENT_REQUEST_ID.get();
        return id != null ? id : "REQ-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestId = httpRequest.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = "REQ-" + UUID.randomUUID().toString().substring(0, 8);
        }

        CURRENT_REQUEST_ID.set(requestId);
        httpRequest.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        httpResponse.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            chain.doFilter(request, response);
        } finally {
            CURRENT_REQUEST_ID.remove();
        }
    }
}
