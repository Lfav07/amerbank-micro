package com.amerbank.audit_logging.controller;


import com.amerbank.audit_logging.dto.AuditEventResponse;
import com.amerbank.audit_logging.dto.AuditEventSummaryResponse;
import com.amerbank.audit_logging.dto.AuditFilterRequest;
import com.amerbank.audit_logging.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/audit")
@Tag(name = "Audit", description = "Audit logs Management")
public class AuditController {

    private static final String JWT_SCHEME = "Bearer JWT";

    private final AuditService service;

    @Operation(
            summary = "Search audit events",
            description = """
                    Retrieves a paginated list of audit events matching the given filter criteria.
                   
                    **Authentication:** Required
                    **Authorization:** Requires ADMIN or USER role

                    **Use case:** When an admin or user needs to search and browse audit logs with optional filters.

                    **Authorization header:** `Authorization: Bearer {token}`
                    """,
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Audit events retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuditEventSummaryResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing JWT token"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions"
            )
    })
    @GetMapping
    public ResponseEntity<Page<AuditEventSummaryResponse>> list(
            @ParameterObject @ModelAttribute AuditFilterRequest filterRequest,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.searchSummary(filterRequest, pageable));
    }

    @Operation(
            summary = "Get audit event by ID",
            description = """
                    Retrieves a single audit event with full payload details by its unique identifier.

                    **Authentication:** Required
                    **Authorization:** Requires ADMIN or USER role
   
                    **Use case:** When a user needs to inspect the full details of a specific audit event.
      
                    **Authorization header:** `Authorization: Bearer {token}`
                    """,
            security = @SecurityRequirement(name = JWT_SCHEME)
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Audit event found and retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuditEventResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing JWT token"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Audit event not found"
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<AuditEventResponse> getById(
            @Parameter(description = "Audit event unique identifier", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

}
