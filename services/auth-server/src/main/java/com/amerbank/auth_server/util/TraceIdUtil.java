package com.amerbank.auth_server.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TraceIdUtil {
    
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    
    public String extractOrGenerateTraceId(HttpServletRequest request) {
        String traceId = extractHeader(request, TRACE_ID_HEADER);
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        
        traceId = extractHeader(request, REQUEST_ID_HEADER);
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        
        return UUID.randomUUID().toString();
    }
    
    private String extractHeader(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        return (value != null && !value.isBlank()) ? value : null;
    }
}
