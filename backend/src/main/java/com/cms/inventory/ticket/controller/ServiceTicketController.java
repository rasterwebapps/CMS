package com.cms.inventory.ticket.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.inventory.ticket.dto.ServiceTicketAssignRequest;
import com.cms.inventory.ticket.dto.ServiceTicketCancelRequest;
import com.cms.inventory.ticket.dto.ServiceTicketCloseRequest;
import com.cms.inventory.ticket.dto.ServiceTicketCreateRequest;
import com.cms.inventory.ticket.dto.ServiceTicketResolveRequest;
import com.cms.inventory.ticket.dto.ServiceTicketResponse;
import com.cms.inventory.ticket.service.ServiceTicketService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/ticket/tickets")
public class ServiceTicketController {

    private static final String VIEW_ANY =
        "@perm.hasAny('INVENTORY_SERVICE_TICKET_VIEW', 'INVENTORY_SERVICE_TICKET_MANAGE', 'INVENTORY_SERVICE_TICKET_ASSIGN', 'INVENTORY_SERVICE_TICKET_RESOLVE', 'INVENTORY_SERVICE_TICKET_CLOSE')";

    private final ServiceTicketService ticketService;

    public ServiceTicketController(ServiceTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('INVENTORY_SERVICE_TICKET_MANAGE')")
    public ResponseEntity<ServiceTicketResponse> create(@Valid @RequestBody ServiceTicketCreateRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.create(request, username(jwt)));
    }

    @GetMapping("/page")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<Page<ServiceTicketResponse>> findPage(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ticketService.findPage(locationId, categoryId, status, priority, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW_ANY)
    public ResponseEntity<ServiceTicketResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.findById(id));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("@perm.has('INVENTORY_SERVICE_TICKET_ASSIGN')")
    public ResponseEntity<ServiceTicketResponse> assign(@PathVariable Long id, @Valid @RequestBody ServiceTicketAssignRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ticketService.assign(id, request, username(jwt)));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("@perm.has('INVENTORY_SERVICE_TICKET_RESOLVE')")
    public ResponseEntity<ServiceTicketResponse> resolve(@PathVariable Long id, @Valid @RequestBody ServiceTicketResolveRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ticketService.resolve(id, request, username(jwt)));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("@perm.has('INVENTORY_SERVICE_TICKET_CLOSE')")
    public ResponseEntity<ServiceTicketResponse> close(@PathVariable Long id, @Valid @RequestBody(required = false) ServiceTicketCloseRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ticketService.close(id, request, username(jwt)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@perm.has('INVENTORY_SERVICE_TICKET_MANAGE')")
    public ResponseEntity<ServiceTicketResponse> cancel(@PathVariable Long id, @Valid @RequestBody ServiceTicketCancelRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ticketService.cancel(id, request, username(jwt)));
    }

    private String username(Jwt jwt) {
        return jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
    }
}
