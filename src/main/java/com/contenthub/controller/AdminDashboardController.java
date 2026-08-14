package com.contenthub.controller;

import com.contenthub.dto.AdminDtos.AdminPurchaseResponse;
import com.contenthub.dto.AdminDtos.AdminUserResponse;
import com.contenthub.dto.AdminDtos.DashboardStatsResponse;
import com.contenthub.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStatsResponse> stats() {
        return ResponseEntity.ok(dashboardService.stats());
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> users() {
        return ResponseEntity.ok(dashboardService.listUsers());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserResponse> userDetail(@PathVariable Long id) {
        return ResponseEntity.ok(dashboardService.userDetail(id));
    }

    @GetMapping("/purchases")
    public ResponseEntity<List<AdminPurchaseResponse>> purchases() {
        return ResponseEntity.ok(dashboardService.allPurchases());
    }
}
