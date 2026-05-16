package com.datavalley.careerguidance.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.datavalley.careerguidance.service.WorkspaceService;

@RestController
@RequestMapping("/api/counselor")
public class CounselorController {

    private final WorkspaceService workspaceService;

    public CounselorController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping("/overview")
    public Map<String, Object> getOverview() {
        return workspaceService.getCounselorOverview();
    }
}
