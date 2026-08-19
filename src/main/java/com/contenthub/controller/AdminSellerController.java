package com.contenthub.controller;

import com.contenthub.dto.SellerDtos.AdminSellerResponse;
import com.contenthub.dto.SellerDtos.RejectRequest;
import com.contenthub.service.SellerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/sellers")
public class AdminSellerController {

    private final SellerService sellerService;

    public AdminSellerController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    @GetMapping
    public ResponseEntity<List<AdminSellerResponse>> listAll() {
        return ResponseEntity.ok(sellerService.adminListAll());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<AdminSellerResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(sellerService.adminApprove(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<AdminSellerResponse> reject(@PathVariable Long id, @RequestBody(required = false) RejectRequest body) {
        String reason = body != null ? body.reason() : null;
        return ResponseEntity.ok(sellerService.adminReject(id, reason));
    }
}
