package com.amerbank.customer.customer.util;

import com.amerbank.customer.customer.config.CustomerProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TraceIdUtil {
    private  final CustomerProperties customerProperties;
    

    public String extractOrGenerateTraceId(HttpServletRequest request) {
        String traceId = extractHeader(request, customerProperties.getTRACE_ID_HEADER());
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        
        traceId = extractHeader(request, customerProperties.getREQUEST_ID_HEADER());
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
