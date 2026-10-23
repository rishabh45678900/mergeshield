package com.mergeshield.api;
import com.mergeshield.service.CompatibilityService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api")
public class AnalysisController {
  private final CompatibilityService service;
  public AnalysisController(CompatibilityService service) { this.service = service; }
  @GetMapping("/health")
  public Object health() { return java.util.Map.of("status", "ok", "message", "MergeShield API is running"); }
  @PostMapping("/analyze")
  public AnalysisResponse analyze(@Valid @RequestBody AnalysisRequest request) {
    return service.analyze(request.oldSpec(), request.newSpec());
  }
}
