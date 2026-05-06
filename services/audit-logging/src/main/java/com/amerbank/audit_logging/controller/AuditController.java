package com.amerbank.audit_logging.controller;


import com.amerbank.audit_logging.dto.AuditEventResponse;
import com.amerbank.audit_logging.dto.AuditEventSummaryResponse;
import com.amerbank.audit_logging.dto.AuditFilterRequest;
import com.amerbank.audit_logging.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/audit")
public class AuditController {
    private final AuditService service;
    @GetMapping
    public ResponseEntity<Page<AuditEventSummaryResponse>> list(@ModelAttribute AuditFilterRequest filterRequest,
                                                                   @PageableDefault(size = 20) Pageable pageable){
        return ResponseEntity.ok(service.searchSummary(filterRequest, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditEventResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

}
