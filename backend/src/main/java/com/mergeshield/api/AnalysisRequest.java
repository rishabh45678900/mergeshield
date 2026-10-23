package com.mergeshield.api;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record AnalysisRequest(
  @NotBlank @Size(max=1_000_000) String oldSpec,
  @NotBlank @Size(max=1_000_000) String newSpec
) {}
