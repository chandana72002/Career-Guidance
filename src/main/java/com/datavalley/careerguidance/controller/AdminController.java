package com.datavalley.careerguidance.controller;

import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.datavalley.careerguidance.dto.CareerDtos;
import com.datavalley.careerguidance.service.CareerService;
import com.datavalley.careerguidance.service.WorkspaceService;
import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final CareerService careerService;
    private final WorkspaceService workspaceService;

    public AdminController(CareerService careerService, WorkspaceService workspaceService) {
        this.careerService = careerService;
        this.workspaceService = workspaceService;
    }

    @GetMapping("/overview")
    public Map<String, Object> getOverview() {
        return workspaceService.getAdminOverview();
    }

    @PostMapping("/careers")
    public CareerDtos.CareerResponse createCareer(@Valid @RequestBody CareerDtos.CareerUpsertRequest request) {
        return careerService.createCareer(request);
    }

    @PutMapping("/careers/{careerId}")
    public CareerDtos.CareerResponse updateCareer(@PathVariable Long careerId,
                                                  @Valid @RequestBody CareerDtos.CareerUpsertRequest request) {
        return careerService.updateCareer(careerId, request);
    }

    @DeleteMapping("/careers/{careerId}")
    public void deleteCareer(@PathVariable Long careerId) {
        careerService.deleteCareer(careerId);
    }
}
