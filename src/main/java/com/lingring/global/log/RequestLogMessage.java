package com.lingring.global.log;

import java.nio.charset.StandardCharsets;
import org.springframework.web.util.ContentCachingRequestWrapper;

public record RequestLogMessage(
        String httpMethod,
        String requestUri,
        String clientIp,
        String requestBody
) {

    public static RequestLogMessage createInstance(
            final ContentCachingRequestWrapper requestWrapper
    ) {
        return new RequestLogMessage(
                requestWrapper.getMethod(),
                buildRequestUri(requestWrapper),
                requestWrapper.getHeader("X-Real-IP"),
                getRequestBody(requestWrapper)
        );
    }

    private static String buildRequestUri(final ContentCachingRequestWrapper request) {
        final String queryString = request.getQueryString();
        if (queryString == null) {
            return request.getRequestURI();
        }
        return request.getRequestURI() + "?" + queryString;
    }

    private static String getRequestBody(final ContentCachingRequestWrapper request) {
        return new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);
    }
}
